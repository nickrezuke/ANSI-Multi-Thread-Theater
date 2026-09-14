import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogoExtrusionLoader extends Loader {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final StatusStage[] EXTRUSION_STAGES = new StatusStage[] {
            new StatusStage(25, "Reading source image geometry..."),
            new StatusStage(60, "Generating 3D voxel extrusion layers..."),
            new StatusStage(90, "Calculating rotation matrices..."),
            new StatusStage(100, "3D Motion Graphics Pipeline Ready!")
    };

    //private static final String ASCII_RAMP = "@%#*+=-:. ";
    //private static final String ASCII_RAMP = "@#W$9876543210?!abc;:+=-,._ ";
    private static final String ASCII_RAMP = "$@B%8&WM#*oahkbdpqwmZO0QJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ";

    private String imageFilePath;
    private boolean loadSuccess = false;
    private double rotationAngle = 0.0;

    // Structure to hold individual 3D points of the extruded logo
    private static class VoxelPoint {
        double x, y, z;
        int r, g, b;

        VoxelPoint(double x, double y, double z, int r, int g, int b) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private final List<VoxelPoint> voxelPoints = new ArrayList<>();

    // Default configuration paths and dimensions
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 40;
    private static final int DEPTH_LAYERS = 6; // Thickness of the 3D extrusion

    public LogoExtrusionLoader(String imageFilePath, int w, int h) {
        super(EXTRUSION_STAGES, w, h);
        this.imageFilePath = imageFilePath;
    }

    public LogoExtrusionLoader(String imageFilePath) {
        this(imageFilePath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public LogoExtrusionLoader(int w, int h) {
        this(null, w, h);
    }

    public LogoExtrusionLoader() {
        this(null, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    protected void initialize() {
        if(imageFilePath == null) {
            decideRandomImage();
        }
        setTargetFps(30);
        try {
            loadAndExtrudeImage();
            loadSuccess = !voxelPoints.isEmpty();
        } catch (IOException e) {
            System.err.println("[Loader Error] Failed to load image asset: " + imageFilePath);
            e.printStackTrace();
            loadSuccess = false;
        }
    }

    private void decideRandomImage() {
        switch((int)(Math.random() * 7)) {
            case 0:
                imageFilePath = "ImageFolderLogoIcon/CCBlueDevil.png"; break;
            case 1:
                imageFilePath = "ImageFolderLogoIcon/LehighHawk.png"; break;
            case 2:
                imageFilePath = "ImageFolderLogoIcon/UbuntuLogo.png"; break;
            case 3:
                imageFilePath = "ImageFolderLogoIcon/LinuxMintLogo.png"; break;
            case 4:
                imageFilePath = "ImageFolderLogoIcon/ArchLinuxLogo.png"; break;
            case 5:
                imageFilePath = "ImageFolderLogoIcon/DebianLogo.png"; break;
            case 6:
            default:
                imageFilePath = "ImageFolderLogoIcon/KaliLogo.png"; break;
        }
    }

    /**
     * Reads the 2D image, filters out transparent alpha boundaries, 
     * and projects pixels along a Z-axis depth vector to build a 3D point cloud.
     */
    private void loadAndExtrudeImage() throws IOException {
        File imgFile = new File(imageFilePath);
        if (!imgFile.exists()) {
            throw new IOException("Image file not found: " + imageFilePath);
        }

        BufferedImage img = ImageIO.read(imgFile);
        if (img == null) {
            throw new IOException("Could not decode image format for: " + imageFilePath);
        }

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        // 1. Increase maxTargetSize for a higher resolution grid
        int maxTargetSize = 55;
        double scale = Math.min((double) maxTargetSize / imgWidth, (double) maxTargetSize / imgHeight);
        int scaledWidth = Math.max(1, (int) (imgWidth * scale));
        int scaledHeight = Math.max(1, (int) (imgHeight * scale));

        double centerX = scaledWidth / 2.0;
        double centerY = scaledHeight / 2.0;

        // 2. Step by 0.25 to sample sub-pixels and eliminate sparse gaps
        for (double y = 0; y < scaledHeight; y += 0.25) {
            for (double x = 0; x < scaledWidth; x += 0.25) {
                int origX = (int) (x / scale);
                int origY = (int) (y / scale);
                if (origX >= imgWidth) origX = imgWidth - 1;
                if (origY >= imgHeight) origY = imgHeight - 1;

                int argb = img.getRGB(origX, origY);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha < 10) {
                    continue;
                }

                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                double relX = x - centerX;
                double relY = y - centerY;

                // 3. Make Z-layer extrusion denser (step by 0.5 instead of integers)
                for (double zLayer = -DEPTH_LAYERS; zLayer <= DEPTH_LAYERS; zLayer += 0.5) {
                    double relZ = zLayer * 0.6;
                    voxelPoints.add(new VoxelPoint(relX, relY, relZ, r, g, b));
                }
            }
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(outputBuffer, " ");
        if (zBuffer != null) {
            Arrays.fill(zBuffer, Double.POSITIVE_INFINITY);
        }

        if (!loadSuccess) {
            String errorMsg = " ERROR: FILE '" + imageFilePath + "' NOT FOUND OR INVALID ";
            int centerOffset = (window_height / 2) * window_width + (window_width / 2) - (errorMsg.length() / 2);
            if (centerOffset >= 0 && centerOffset < outputBuffer.length) {
                outputBuffer[centerOffset] = errorMsg;
            }
            return;
        }

        // Increment rotation angle to animate continuous spinning motion
        rotationAngle += 0.04;
        double cosA = Math.cos(rotationAngle);
        double sinA = Math.sin(rotationAngle);

        int screenCenterX = window_width / 2;
        int screenCenterY = window_height / 2;

        for (VoxelPoint p : voxelPoints) {
            // Apply 3D Rotation around the Y-axis (Yaw)
            double xRot = p.x * cosA + p.z * sinA;
            double yRot = p.y;
            double zRot = -p.x * sinA + p.z * cosA;

            // Add camera depth offset
            double cameraZ = zRot + 45.0;
            if (cameraZ <= 0) continue; // Clip points behind camera view plane

            // Perspective projection calculation
            double projectionScale = 35.0 / cameraZ;
            int screenX = (int) Math.round(screenCenterX + (xRot * projectionScale * 2.0)); // 2.0 accounts for terminal aspect ratio
            int screenY = (int) Math.round(screenCenterY + (yRot * projectionScale));

            if (screenX < 0 || screenX >= window_width || screenY < 0 || screenY >= window_height) {
                continue;
            }

            int bufferOffset = screenX + (window_width * screenY);

            // Z-buffer occlusion test for proper 3D depth rendering
            if (zBuffer != null) {
                if (cameraZ > zBuffer[bufferOffset]) {
                    continue; // Obscured by a closer pixel layer
                }
                zBuffer[bufferOffset] = cameraZ;
            }

            // Calculate shading brightness based on surface normal / lighting simulation
            double brightness = (0.2126 * p.r) + (0.7152 * p.g) + (0.0722 * bForShading(p.b));
            // Apply simple ambient/directional shading modifier using Z depth
            brightness = Math.min(255, Math.max(20, brightness * (0.7 + (zRot / (DEPTH_LAYERS * 2.0)) * 0.3)));
            
            int rampIndex = (int) ((brightness / 255.0) * (ASCII_RAMP.length() - 1));
            char asciiChar = ASCII_RAMP.charAt(rampIndex);

            // Truecolor ANSI escape token formatting matching base engine standard
            String truecolorAnsiToken = "\u001B[38;2;" + p.r + ";" + p.g + ";" + p.b + "m" + asciiChar + "\u001B[0m";

            if (bufferOffset >= 0 && bufferOffset < outputBuffer.length) {
                outputBuffer[bufferOffset] = truecolorAnsiToken;
            }
        }
    }

    private int bForShading(int b) {
        return b;
    }
}