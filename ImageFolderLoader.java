import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

    private List<BufferedImage> cachedFrames = new ArrayList<>();
    private int frameCount = 0;
    private int currentFrameIndex = 0;
    private boolean folderLoadedSuccessfully = false;

    private int renderWidth;
    private int renderHeight;
    private int offsetX;
    private int offsetY;

    // Default Values
    private static final String[] DEFAULT_PATHS = {"ImageFolderPNG", "ImageFolderJPG", "ImageFolderBMP"}; 
    // TODO: add filetypes? like .jpeg, .tif .tiff, .dib, .wbmp, .gif, And finalize which videos to play!
    // TODO: 6000+ frames takes too long, can we parallelize loading and playing???
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 40;

    // Master constructor handles all assignments
    public ImageFolderLoader(String folderPath, int w, int h) {
        super(FOLDER_STAGES, w, h);
        this.folderPath = folderPath;
    }

    public ImageFolderLoader() {
        String randPath = DEFAULT_PATHS[(int)(Math.random() * DEFAULT_PATHS.length)];
        this(randPath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ImageFolderLoader(String folderPath) {
        this(folderPath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public ImageFolderLoader(int w, int h) {
        String randPath = DEFAULT_PATHS[(int)(Math.random() * DEFAULT_PATHS.length)];
        this(randPath, w, h);
    }

    @Override
    protected void initialize() {
        //setTargetFps(30); // If needed
        try {
            loadAndCacheFolderFrames();
            folderLoadedSuccessfully = !cachedFrames.isEmpty();
            frameCount = cachedFrames.size();
        } catch (IOException e) {
            System.err.println("[Loader Error] Failed to process image directory: " + folderPath);
            e.printStackTrace();
            folderLoadedSuccessfully = false;
        }
    }

    /**
     * Reads all PNG, JPG, and JPEG files from the target directory, sorts them by
     * filename, and downscales each image into memory.
     */
    // TODO: maybe make jpg background white / black become transparent??
    private void loadAndCacheFolderFrames() throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Directory not found or invalid: " + folderPath);
        }

        // Filter directory for compatible static image formats
        File[] frameFiles = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".bmp") || lower.endsWith(".jpeg") || lower.endsWith(".wbmp") || lower.endsWith(".dib") || lower.endsWith(".gif") || lower.endsWith(".tif") || lower.endsWith(".tiff");
        });

        if (frameFiles == null || frameFiles.length == 0) {
            throw new IOException("No valid .png, .jpg, or .jpeg files found in: " + folderPath);
        }

        // Sort files to ensure natural frame ordering 
        // (e.g., frame_01.png, frame_02.png)
        Arrays.sort(frameFiles, Comparator.comparing(File::getName));

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

        // Load and cache all frames directly into scaled memory buffers
        for (File frameFile : frameFiles) {
            BufferedImage original = ImageIO.read(frameFile);
            if (original == null) {
                continue;
            }

            BufferedImage scaledFrame = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gScaled = scaledFrame.createGraphics();
            gScaled.setComposite(AlphaComposite.Clear);
            gScaled.fillRect(0, 0, renderWidth, renderHeight);
            gScaled.setComposite(AlphaComposite.SrcOver);
            gScaled.drawImage(original, 0, 0, renderWidth, renderHeight, null);
            gScaled.dispose();

            cachedFrames.add(scaledFrame);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");

        if (zBuffer != null) {
            Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
        }

        if (!folderLoadedSuccessfully) {
            String errorMsg = " ERROR: NO VALID FRAMES IN '" + folderPath + "' ";
            int centerOffset = (window_height / 2) * window_width + (window_width / 2) - (errorMsg.length() / 2);
            if (centerOffset >= 0 && centerOffset < outputBuffer.length) {
                outputBuffer[centerOffset] = errorMsg;
            }
            return;
        }

        BufferedImage activeFrame = cachedFrames.get(currentFrameIndex);

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

        currentFrameIndex = (currentFrameIndex + 1) % frameCount;
    }
}
