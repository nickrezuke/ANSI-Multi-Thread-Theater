// TODO: Make this Sushi Loader Better / Add more Foods?

public class SushiLoader extends Loader {
    private static final StatusStage[] SUSHI_STAGES = {
            new StatusStage(20, "Assembling Sushi:"),
            new StatusStage(45, "Filling Ceramic Bowls:"),
            new StatusStage(70, "Slicing Salmon:"),
            new StatusStage(95, "Obtaining Chopsticks:"),
            new StatusStage(100, "Itadakimasu!")
    };

    private double timeClock = 0.0;
    private final int width = 120;
    private final int height = 36;
    private static final double CAMERA_DISTANCE = 3.5;

    // Direct 24-bit ANSI color palettes
    private static final int[] RGB_BELT = { 55, 58, 62 }; // Dark Slate Tread Plates
    private static final int[] RGB_BELT_LIT = { 85, 90, 95 }; // Highlighted Tread Edges
    private static final int[] RGB_BOWL_RIM = { 210, 35, 35 }; // Ceramic Crimson Red
    private static final int[] RGB_NOODLES = { 245, 215, 90 }; // Bright Ramen Yellow
    private static final int[] RGB_SUSHI_RICE = { 240, 240, 245 }; // Pristine Rice White
    private static final int[] RGB_SUSHI_FISH = { 250, 110, 90 }; // Salmon Pink
    private static final int[] RGB_GARNISH = { 45, 165, 65 }; // Scallion Green

    public SushiLoader() {
        super(SUSHI_STAGES, 120, 36);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.0025; // Controls path transit velocity and global spin vectors

        // Global camera viewing angle adjustments
        double rotX = 0.45; // Tilted downward slightly to see inside the bowls
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double rotY = 0.15 * Math.sin(timeClock * 0.4); // Subtle, slow camera drift swing
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);

        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // -------------------------------------------------------------
        // LAYER 1: THE TREADMILL BELT PLATES
        // -------------------------------------------------------------
        // Render a flat racetrack tracking ring loop on the counter surface
        for (double bz = -1.6; bz <= 1.6; bz += 0.04) {
            for (double bx = -2.8; bx <= 2.8; bx += 0.05) {

                // Keep the geometry inside an elongated track ring profile
                double distToCenterTrack = Math.abs(Math.abs(bx) - 1.2) + Math.abs(bz) * 0.4;
                boolean inBeltPlates = distToCenterTrack < 0.65 && distToCenterTrack > 0.25;

                if (inBeltPlates) {
                    double by = 0.25; // Belt platform elevation plane

                    // Direction vector logic for mechanical belt cleat markings
                    double beltScroll = (bx > 0) ? bz + timeClock * 0.8 : bz - timeClock * 0.8;
                    boolean isCleatLine = Math.abs(Math.sin(beltScroll * 8.0 + bx * 2.0)) > 0.82;

                    int[] bRGB = isCleatLine ? RGB_BELT_LIT : RGB_BELT;
                    char bChar = isCleatLine ? '>' : '█';

                    plot3DElement(bx, by, bz, 0.0, -1.0, 0.0, bRGB, bChar, cosX, sinX, cosY, sinY, lightX, lightY,
                            lightZ, outputBuffer, zBuffer);
                }
            }
        }

        // -------------------------------------------------------------
        // LAYER 2: THE MOVING FOOD AGENTS (Capsule Loop Distribution)
        // -------------------------------------------------------------
        int totalPlates = 5; // Spawn 5 independent dishes traveling along the loop
        for (int i = 0; i < totalPlates; i++) {
            // Distribute items uniformly across the track phase spaces
            double progressPhase = (timeClock * 0.4 + (double) i / totalPlates) * 2.0 * Math.PI;

            // Parametric Capsule Equation: maps tracking paths cleanly onto elongated
            // rounded loops
            double itemZ = 1.1 * Math.sin(progressPhase);
            double itemX = 1.8 * Math.cos(progressPhase);

            // Alternate item indices: Evens = Ramen Bowls, Odds = Sushi Platters
            if (i % 2 == 0) {
                renderRamenBowlMesh(itemX, itemZ, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer,
                        zBuffer);
            } else {
                renderSushiPlatterMesh(itemX, itemZ, cosX, sinX, cosY, sinY, lightX, lightY, lightZ, outputBuffer,
                        zBuffer);
            }
        }
    }

    private void renderRamenBowlMesh(double cx, double cz, double cosX, double sinX, double cosY, double sinY,
            double lx, double ly, double lz, String[] outBuf, double[] zBuf) {
        double bowlRadius = 0.26;

        // Extrude a hemisphere shell container
        for (double ry = 0.0; ry <= bowlRadius; ry += 0.02) {
            double currentLayerRadius = Math.sqrt(bowlRadius * bowlRadius - ry * ry);

            for (double angle = 0; angle < 2 * Math.PI; angle += 0.15) {
                double px = cx + currentLayerRadius * Math.cos(angle);
                double py = 0.25 - ry; // Extrude upwards from the belt surface level
                double pz = cz + currentLayerRadius * Math.sin(angle);

                // Normal vector tracking for hemispherical specular calculations
                double rNx = Math.cos(angle);
                double rNy = -ry / bowlRadius;
                double rNz = Math.sin(angle);

                int[] itemRGB = RGB_BOWL_RIM;
                char itemChar = '█';

                // Fill the open inner cavity layer with noodles and garnish segments
                if (ry > bowlRadius - 0.04) {
                    double interiorCheck = Math.cos(angle * 5.0) * Math.sin(angle * 3.0);
                    if (interiorCheck > 0.1) {
                        itemRGB = RGB_GARNISH; // Chopped scallions accent
                        itemChar = '▒';
                    } else {
                        itemRGB = RGB_NOODLES; // Wavy noodle fill texture
                        itemChar = '▓';
                    }
                }

                plot3DElement(px, py, pz, rNx, rNy, rNz, itemRGB, itemChar, cosX, sinX, cosY, sinY, lx, ly, lz, outBuf,
                        zBuf);
            }
        }
    }

    private void renderSushiPlatterMesh(double cx, double cz, double cosX, double sinX, double cosY, double sinY,
            double lx, double ly, double lz, String[] outBuf, double[] zBuf) {
        // Construct a rectangular stacked block structure representing Salmon Nigiri
        for (double sy = 0.0; sy <= 0.18; sy += 0.03) {
            boolean isFishLayer = sy > 0.09; // Fish meat sits stacked cleanly on top of the rice block

            for (double sx = -0.22; sx <= 0.22; sx += 0.03) {
                for (double sz = -0.14; sz <= 0.14; sz += 0.03) {

                    double px = cx + sx;
                    double py = 0.25 - sy; // Extrude upward from belt track floor bounds
                    double pz = cz + sz;

                    // Compute basic box face tracking directional vectors
                    double nx = (sx > 0) ? 1.0 : -1.0;
                    double ny = (sy > 0) ? -1.0 : 1.0;
                    double nz = (sz > 0) ? 1.0 : -1.0;

                    int[] itemRGB = isFishLayer ? RGB_SUSHI_FISH : RGB_SUSHI_RICE;
                    char itemChar = '█';

                    // Insert stylized white stripe arrays running across the salmon block
                    if (isFishLayer && Math.abs(sx + sz * 1.5) % 0.12 < 0.03) {
                        itemRGB = RGB_SUSHI_RICE;
                        itemChar = '▒';
                    }

                    plot3DElement(px, py, pz, nx, ny, nz, itemRGB, itemChar, cosX, sinX, cosY, sinY, lx, ly, lz, outBuf,
                            zBuf);
                }
            }
        }
    }

    private void plot3DElement(double x, double y, double z, double rNx, double rNy, double rNz,
            int[] rgb, char glyph, double cosX, double sinX, double cosY, double sinY,
            double lightX, double lightY, double lightZ, String[] outputBuffer, double[] zBuffer) {

        // --- AXIS 1: WORLD SPIN INTERPOLATION (Y-Axis Swing) ---
        double x1 = x * cosY + z * sinY;
        double y1 = y;
        double z1 = -x * sinY + z * cosY;

        double nx1 = rNx * cosY + rNz * sinY;
        double ny1 = rNy;
        double nz1 = -rNx * sinY + rNz * cosY;

        // --- AXIS 2: CAMERA CINEMATIC PITCH (X-Axis Tilt Down) ---
        double worldX = x1;
        double worldY = y1 * cosX - z1 * sinX;
        double worldZ = y1 * sinX + z1 * cosX;

        double worldNx = nx1;
        double worldNy = ny1 * cosX - nz1 * sinX;
        double worldNz = ny1 * sinX + nz1 * cosX;

        // --- PROJECTION SYSTEM MAPPING ---
        double ooz = 1.0 / (worldZ + CAMERA_DISTANCE);
        int xp = (int) (60 + 94 * ooz * worldX * 2.3); // Sized aspect ratio scale factor for 120x36 grid
        int yp = (int) (18 + 42 * ooz * worldY);

        if (xp >= 0 && xp < width && yp >= 0 && yp < height) {
            int idx = xp + width * yp;

            if (ooz > zBuffer[idx] + 0.0001) {
                zBuffer[idx] = ooz;

                // Lambertian reflectance color shading calculation
                double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                double shadeFactor = 0.5 + 0.5 * luminance;

                int r = (int) (rgb[0] * shadeFactor);
                int g = (int) (rgb[1] * shadeFactor);
                int b = (int) (rgb[2] * shadeFactor);

                String colorCode = String.format("\u001B[38;2;%d;%d;%dm", Math.max(0, Math.min(255, r)),
                        Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
                outputBuffer[idx] = colorCode + glyph + RESET;
            }
        }
    }
}