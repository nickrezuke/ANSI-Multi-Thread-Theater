import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageFolderLoader extends Loader {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final StatusStage[] FOLDER_STAGES = new StatusStage[] {
            new StatusStage(20, "Scanning frame directory..."),
            new StatusStage(50, "Loading JPG and PNG image files..."),
            new StatusStage(80, "Downscaling frames for terminal view..."),
            new StatusStage(100, "Frame Sequence Ready!")
    };

    private static final String ASCII_RAMP = "@#W$9876543210?!abc;:+=-,._ ";

    // The actual folder name
    private String folderPath;

    // Frame cache. Preallocated up front once we know the file count, then
    // filled in from multiple worker threads. Index order is preserved
    // (frameCache[i] corresponds to frameFiles[i] after sorting), so
    // playback can loop over "however much of the front of this array is
    // ready" instead of waiting for the whole thing.
    private volatile BufferedImage[] frameCache;
    private boolean[] frameReadyFlags;
    private final Object readyLock = new Object();
    private int nextContiguousIndex = 0;
    private final AtomicInteger framesReady = new AtomicInteger(0);

    private volatile int totalFrameCount = 0;
    private volatile boolean folderLoadedSuccessfully = false;
    private volatile boolean loadingFailed = false;

    private int currentFrameIndex = 0;

    private volatile int renderWidth;
    private volatile int renderHeight;
    private volatile int offsetX;
    private volatile int offsetY;

    private ExecutorService loaderExecutor;

    // Default Values
    private static final String[] DEFAULT_PATHS = {"ImageFolderPNG", "ImageFolderJPG", "ImageFolderBMP"}; // TODO add a jpeg for example??
    private static final String BAD_APPLE_PATH = "ImageFolderBadApple";

    private static final int DEFAULT_WIDTH = 130;
    private static final int DEFAULT_HEIGHT = 40;

    // Master constructor handles all assignments
    public ImageFolderLoader(String folderPath, int w, int h) {
        super(FOLDER_STAGES, w, h);
        // We might have passed a special variant... double check
        if(folderPath == "Bad Apple") {
            this.folderPath = BAD_APPLE_PATH;
        } else {
            this.folderPath = folderPath;
        }
    }

    public ImageFolderLoader() {
        this(DEFAULT_PATHS[(int)(Math.random() * DEFAULT_PATHS.length)], DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ImageFolderLoader(String folderPath) {
        this(folderPath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ImageFolderLoader(int w, int h) {
        this(DEFAULT_PATHS[(int)(Math.random() * DEFAULT_PATHS.length)], w, h);
    }

    @Override
    public void stopLoading() {
        super.stopLoading();
        // loadingThread.join() in the standard usage pattern only waits on
        // the render thread, not this pool. Without this, a caller that
        // stops early on a large folder leaves thousands of queued decodes
        // running for a loader nobody's watching anymore.
        if (loaderExecutor != null) {
            loaderExecutor.shutdownNow();
        }
    }

    @Override
    protected void initialize() {
        setTargetFps(30); // Speed of Image slideshow
        try {
            prepareFolderAndKickoffBackgroundLoad();
        } catch (IOException e) {
            System.err.println("[Loader Error] Failed to process image directory: " + folderPath);
            e.printStackTrace();
            loadingFailed = true;
        }
    }

    /**
     * Lists and sorts the frame files, synchronously decodes just the first
     * frame (needed to establish the shared render bounds), then hands every
     * remaining frame off to a pool of worker threads. This lets rendering
     * start almost immediately instead of blocking until all 6000+ files are
     * decoded and scaled.
     */
    private void prepareFolderAndKickoffBackgroundLoad() throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Directory not found or invalid: " + folderPath);
        }

        File[] frameFiles = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".bmp");
        });

        if (frameFiles == null || frameFiles.length == 0) {
            throw new IOException("No valid .png, .jpg, or .jpeg files found in: " + folderPath);
        }

        // Sort files to ensure natural frame ordering
        // (e.g., frame_01.png, frame_02.png)
        Arrays.sort(frameFiles, Comparator.comparing(File::getName));

        totalFrameCount = frameFiles.length;
        frameCache = new BufferedImage[totalFrameCount];
        frameReadyFlags = new boolean[totalFrameCount];

        // Read first frame to establish target aspect ratio & layout bounds
        BufferedImage firstFrame = ImageIO.read(frameFiles[0]);
        if (firstFrame == null) {
            throw new IOException("Failed to decode primary reference image: " + frameFiles[0].getName());
        }

        int masterWidth = firstFrame.getWidth();
        int masterHeight = firstFrame.getHeight();

        // Calculate aspect ratio scale to fit within terminal dimensions
        double scaleX = (double) window_width / (double) masterWidth;
        double scaleY = (double) window_height / ((double) masterHeight * 0.5);
        double scaleFactor = Math.min(scaleX, scaleY);

        this.renderWidth = Math.min((int) (masterWidth * scaleFactor), window_width);
        this.renderHeight = Math.min((int) (masterHeight * scaleFactor * 0.5), window_height);
        this.offsetX = (window_width - this.renderWidth) / 2;
        this.offsetY = (window_height - this.renderHeight) / 2;

        frameCache[0] = scaleFrame(firstFrame);
        markFrameReady(0);
        folderLoadedSuccessfully = true;

        // Decode + scale every other frame in parallel. Each task owns a
        // single, distinct array slot, so there's no shared-state contention
        // beyond the ready-tracking below.
        // Daemon threads: forceTerminalCleanup() is final (no subclass hook)
        // and the Ctrl+C shutdown hook in ExampleTask calls it directly,
        // bypassing stopLoading() entirely. Non-daemon pool threads would
        // silently keep the JVM alive after cleanup in that path, so these
        // must never be able to block process exit on their own.
        // -1: the render thread (Loader.run()) also needs CPU every tick to
        // build and print the frame. On low core-count machines, letting
        // the pool claim every core can starve that thread and make the
        // playback we just sped up to start immediately stutter instead.
        int workerCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        loaderExecutor = Executors.newFixedThreadPool(workerCount, runnable -> {
            Thread t = new Thread(runnable, "ImageFolderLoader-decode");
            t.setDaemon(true);
            return t;
        });

        for (int i = 1; i < frameFiles.length; i++) {
            final int index = i;
            final File file = frameFiles[i];
            loaderExecutor.submit(() -> {
                // isRunning is the same flag Loader flips to false on
                // stopLoading()/forceTerminalCleanup(). If this instance has
                // already been torn down (e.g. the user skipped to a
                // different animation while a 6000-frame folder was still
                // loading), there's no render loop left to consume these
                // frames, so stop doing decode work nobody will see.
                if (!isRunning) {
                    return;
                }
                try {
                    BufferedImage original = ImageIO.read(file);
                    if (original != null) {
                        frameCache[index] = scaleFrame(original);
                    }
                } catch (IOException e) {
                    System.err.println("[Loader Warning] Skipped unreadable frame: " + file.getName());
                } finally {
                    // Always mark ready, even on failure, so a single bad
                    // file can't stall the contiguous-ready pointer forever.
                    // frameCache[index] just stays null and gets skipped
                    // during playback.
                    markFrameReady(index);
                }
            });
        }

        loaderExecutor.shutdown();
    }

    private BufferedImage scaleFrame(BufferedImage original) {
        BufferedImage scaledFrame = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gScaled = scaledFrame.createGraphics();
        gScaled.setComposite(AlphaComposite.Clear);
        gScaled.fillRect(0, 0, renderWidth, renderHeight);
        gScaled.setComposite(AlphaComposite.SrcOver);
        gScaled.drawImage(original, 0, 0, renderWidth, renderHeight, null);
        gScaled.dispose();
        return scaledFrame;
    }

    /**
     * Frames can finish decoding out of order across worker threads, but
     * playback can only safely loop over an unbroken run starting at index
     * 0. This advances that run's length every time a new frame lands,
     * and publishes it through an AtomicInteger so the render thread can
     * read it without locking.
     */
    private void markFrameReady(int index) {
        synchronized (readyLock) {
            frameReadyFlags[index] = true;
            while (nextContiguousIndex < frameReadyFlags.length && frameReadyFlags[nextContiguousIndex]) {
                nextContiguousIndex++;
            }
            framesReady.set(nextContiguousIndex);
        }
        // Deliberately not calling setProgress() here. this.progress is
        // owned by whatever external process is using this loader (see
        // ExampleTask) - FOLDER_STAGES messages are flavor text, not a
        // report of internal decode state, so this class shouldn't be
        // writing to it.
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");

        if (zBuffer != null) {
            Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
        }

        if (loadingFailed || !folderLoadedSuccessfully) {
            writeCenteredMessage(outputBuffer, " ERROR: NO VALID FRAMES IN '" + folderPath + "' ");
            return;
        }

        int available = framesReady.get();
        if (available == 0) {
            writeCenteredMessage(outputBuffer, " Loading frames... ");
            return;
        }

        BufferedImage activeFrame = frameCache[currentFrameIndex % available];
        currentFrameIndex = (currentFrameIndex + 1) % available;

        if (activeFrame == null) {
            // That particular file failed to decode; just hold this tick
            // blank rather than crashing or flashing garbage.
            return;
        }

        for (int y = 0; y < renderHeight; y++) {
            for (int x = 0; x < renderWidth; x++) {
                int targetScreenX = x + offsetX;
                int targetScreenY = y + offsetY;

                if (targetScreenX < 0 || targetScreenX >= window_width ||
                        targetScreenY < 0 || targetScreenY >= window_height) {
                    continue;
                }

                int rgb = activeFrame.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;

                // Skip transparent background pixels
                if (alpha == 0) {
                    continue;
                }

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double brightness = (0.2126 * r) + (0.7152 * g) + (0.0722 * b);
                int rampIndex = (int) ((brightness / 255.0) * (ASCII_RAMP.length() - 1));
                char asciiChar = ASCII_RAMP.charAt(rampIndex);

                String truecolorAnsiToken = "\u001B[38;2;" + r + ";" + g + ";" + b + "m" + asciiChar + "\u001B[0m";
                int bufferOffset = targetScreenX + (window_width * targetScreenY);

                if (bufferOffset >= 0 && bufferOffset < outputBuffer.length) {
                    outputBuffer[bufferOffset] = truecolorAnsiToken;
                }
            }
        }
    }

    private void writeCenteredMessage(String[] outputBuffer, String message) {
        int centerOffset = (window_height / 2) * window_width + (window_width / 2) - (message.length() / 2);
        if (centerOffset >= 0 && centerOffset < outputBuffer.length) {
            outputBuffer[centerOffset] = message;
        }
    }
}