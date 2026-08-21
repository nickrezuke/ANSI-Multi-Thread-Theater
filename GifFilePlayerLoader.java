import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.NodeList;

public class GifFilePlayerLoader extends Loader {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final StatusStage[] GIF_STAGES = new StatusStage[] {
            new StatusStage(20, "Activating headless graphics engine..."),
            new StatusStage(50, "Parsing image data boundaries..."),
            new StatusStage(80, "Resolving full history disposal maps..."),
            new StatusStage(100, "Render Sequence Initialized!")
    };

    private String GIF_FILE_NAME;

    private static final String ASCII_RAMP = "@#W$9876543210?!abc;:+=-,._ ";

    private List<BufferedImage> cachedFrames = new ArrayList<>();
    private int frameCount = 0;
    private int currentFrameIndex = 0;
    private boolean gifLoadedSuccessfully = false;

    private int renderWidth;
    private int renderHeight;
    private int offsetX;
    private int offsetY;

    public GifFilePlayerLoader() {
        super(GIF_STAGES, 80, 40);
    }

    public GifFilePlayerLoader(int w, int h) {
        super(GIF_STAGES, w, h);
    }

    @Override
    protected void initialize() {
        setTargetFps(30); // Speed of GIFs

        switch((int)(Math.random() * 3)) { // 3 example gifs
            case 0:
                GIF_FILE_NAME = "ImageFolderGIF/Skull.gif"; break;
            case 1:
                GIF_FILE_NAME = "ImageFolderGIF/HelloWave.gif"; break;
            case 2:
            default:
                GIF_FILE_NAME = "ImageFolderGIF/GemHeart.gif"; break;
        }

        try {
            loadAndCacheGifFrames();
            gifLoadedSuccessfully = !cachedFrames.isEmpty();
            frameCount = cachedFrames.size();
        } catch (IOException e) {
            System.err.println("[Loader Error] Failed to read image file: " + GIF_FILE_NAME);
            e.printStackTrace();
            gifLoadedSuccessfully = false;
        }
    }

    /**
     * Deep-clones a BufferedImage to preserve exact snapshot history state maps.
     */
    private BufferedImage cloneImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Extracts individual sub-images from the target animated file and pools
     * them into a scaled memory cache to prevent heavy frame-by-frame runtime IO
     * lag.
     */
    private void loadAndCacheGifFrames() throws IOException {
        File gifFile = new File(GIF_FILE_NAME);
        if (!gifFile.exists()) {
            throw new IOException("File not found! Make sure '" + GIF_FILE_NAME + "' is in your project directory.");
        }

        try (ImageInputStream stream = ImageIO.createImageInputStream(gifFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                throw new IOException("No compatible GIF ImageReaders found in your local JVM runtime context.");
            }

            ImageReader reader = readers.next();
            reader.setInput(stream);

            int totalImages = reader.getNumImages(true);

            BufferedImage firstFrame = reader.read(0);
            int masterWidth = firstFrame.getWidth();
            int masterHeight = firstFrame.getHeight();

            // This canvas tracks the active composition screen frame-by-frame
            BufferedImage masterCanvas = new BufferedImage(masterWidth, masterHeight, BufferedImage.TYPE_INT_ARGB);

            // This canvas holds the historical backup state for "Restore to Previous"
            // actions
            BufferedImage backupCanvas = new BufferedImage(masterWidth, masterHeight, BufferedImage.TYPE_INT_ARGB);

            // --- TERMINAL ASPECT RATIO CORRECTOR ---
            double scaleX = (double) window_width / (double) masterWidth;
            double scaleY = (double) window_height / ((double) masterHeight * 0.5);
            double scaleFactor = Math.min(scaleX, scaleY);

            this.renderWidth = (int) (masterWidth * scaleFactor);
            this.renderHeight = (int) (masterHeight * scaleFactor * 0.5);

            if (this.renderWidth > window_width)
                this.renderWidth = window_width;
            if (this.renderHeight > window_height)
                this.renderHeight = window_height;

            this.offsetX = (window_width - this.renderWidth) / 2;
            this.offsetY = (window_height - this.renderHeight) / 2;

            int prevLeft = 0, prevTop = 0, prevWidth = masterWidth, prevHeight = masterHeight;
            String prevDisposal = "none";

            for (int i = 0; i < totalImages; i++) {
                BufferedImage rawFrame = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);

                int frameLeft = 0;
                int frameTop = 0;
                int frameWidth = rawFrame.getWidth();
                int frameHeight = rawFrame.getHeight();
                String disposalMethod = "none";

                try {
                    String metaFormat = metadata.getNativeMetadataFormatName();
                    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormat);

                    NodeList descNodes = root.getElementsByTagName("ImageDescriptor");
                    if (descNodes.getLength() > 0) {
                        IIOMetadataNode descriptor = (IIOMetadataNode) descNodes.item(0);
                        frameLeft = Integer.parseInt(descriptor.getAttribute("imageLeftPosition"));
                        frameTop = Integer.parseInt(descriptor.getAttribute("imageTopPosition"));
                    }

                    NodeList gceNodes = root.getElementsByTagName("GraphicControlExtension");
                    if (gceNodes.getLength() > 0) {
                        IIOMetadataNode gce = (IIOMetadataNode) gceNodes.item(0);
                        disposalMethod = gce.getAttribute("disposalMethod");
                    }
                } catch (Exception e) {
                    // Fallback to default configurations
                }

                // --- PROCESS PREVIOUS DISPOSAL BEFORE DRAWING NEW FRAME ---
                if ("restoreToBackgroundColor".equals(prevDisposal)) {
                    // Method 2: Clear out exactly where the previous frame sat
                    Graphics2D gCanvas = masterCanvas.createGraphics();
                    gCanvas.setComposite(AlphaComposite.Clear);
                    gCanvas.fillRect(prevLeft, prevTop, prevWidth, prevHeight);
                    gCanvas.dispose();
                } else if ("restoreToPrevious".equals(prevDisposal)) {
                    // Method 3: Re-apply the raw state snapshot saved before the previous frame ran
                    Graphics2D gCanvas = masterCanvas.createGraphics();
                    gCanvas.setComposite(AlphaComposite.Src);
                    gCanvas.drawImage(backupCanvas, 0, 0, null);
                    gCanvas.dispose();
                } else if (i == 0) {
                    // Ensure full workspace optimization blank on launch
                    Graphics2D gCanvas = masterCanvas.createGraphics();
                    gCanvas.setComposite(AlphaComposite.Clear);
                    gCanvas.fillRect(0, 0, masterWidth, masterHeight);
                    gCanvas.dispose();
                }

                // If the CURRENT frame is telling us it wants a "Restore to Previous" action
                // later,
                // we must capture a snapshot of the screen right now *before* drawing this
                // frame.
                if ("restoreToPrevious".equals(disposalMethod)) {
                    backupCanvas = cloneImage(masterCanvas);
                }

                // Draw the fresh update frame onto the master canvas structure
                Graphics2D g = masterCanvas.createGraphics();
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(rawFrame, frameLeft, frameTop, null);
                g.dispose();

                // Downscale the final clean frame data
                BufferedImage scaledFrame = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gScaled = scaledFrame.createGraphics();
                gScaled.setComposite(AlphaComposite.Clear);
                gScaled.fillRect(0, 0, renderWidth, renderHeight);
                gScaled.setComposite(AlphaComposite.SrcOver);
                gScaled.drawImage(masterCanvas, 0, 0, renderWidth, renderHeight, null);
                gScaled.dispose();

                cachedFrames.add(scaledFrame);

                // Cache metadata tracking properties for the next pass
                prevLeft = frameLeft;
                prevTop = frameTop;
                prevWidth = frameWidth;
                prevHeight = frameHeight;
                prevDisposal = disposalMethod;
            }
            reader.dispose();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");

        if (zBuffer != null) {
            Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
        }

        if (!gifLoadedSuccessfully) {
            String errorMsg = " ERROR: PLACE '" + GIF_FILE_NAME + "' IN YOUR APP ROOT FOLDER ";
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
                if (targetScreenX < 0 || targetScreenX >= window_width || targetScreenY < 0
                        || targetScreenY >= window_height) {
                    continue;
                }
                int rgb = activeFrame.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                // If it's transparent, bypass processing to let the border block remain intact
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