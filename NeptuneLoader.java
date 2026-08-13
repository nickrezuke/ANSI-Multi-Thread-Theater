// TODO: Improve the graphical accuracy of Neptune.  The surface should fade white dependign on distance to the poles?
// https://science.nasa.gov/resource/neptune-3d-model/

import java.util.Random;

public class NeptuneLoader extends Loader {
    private static final StatusStage[] NEPTUNE_STAGES = {
            new StatusStage(30, "Simulating deep methane absorption matrices:"),
            new StatusStage(65, "Extruding hyper-thin 1-pixel dust rings:"),
            new StatusStage(90, "Projecting the Great Dark Spot anticyclone:"),
            new StatusStage(100, "Neptune Atmospheric Matrix Active!")
    };

    private static final int MAX_STARS = 45;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double neptuneRotationAngle = Math.PI;

    // Scientifically accurate low-contrast palette registers
    private static final int[] C_BASE_COBALT = { 45, 95, 230 }; // Dominant Cobalt Blue Body
    private static final int[] C_GDS_CORE = { 15, 35, 110 }; // Deep Navy Indigo Spot Core
    private static final int[] C_SCOUTER = { 240, 245, 255 }; // Stark White High-Altitude Clouds
    private static final int[] RGB_RING = { 90, 115, 140 }; // Faint, Dusty Slate-Grey Ring Filaments

    public NeptuneLoader() {
        super(NEPTUNE_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.neptuneRotationAngle = Math.PI;
        Random rand = new Random(1111);
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = 1 + rand.nextInt(20);
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw Twinkling Background Starfield
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int starIdx = starPositions[i];
            double twinkleFactor = Math.sin((currentTime * 0.004) + starPhases[i]);
            char starChar = ' ';
            if (twinkleFactor > 0.82)
                starChar = '*';
            else if (twinkleFactor > 0.20)
                starChar = '.';
            else if (twinkleFactor > -0.3)
                starChar = '·';

            if (starChar != ' ' && starIdx >= 0 && starIdx < 1760) {
                zBuffer[starIdx] = 0.0001;
                outputBuffer[starIdx] = "\u001B[37m" + starChar + RESET;
            }
        }

        // Step 2: Global Planetary Kinematics
        neptuneRotationAngle += 0.006;

        // Neptune's physical base axial tilt (28.3 degrees)
        double axialTilt = Math.toRadians(28.3);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Overhead directional spotlight vectors
        double lightX = 0.50, lightY = -0.75, lightZ = 0.42;
        double cameraDistance = 3.85;
        double sphereRadius = 1.0;
        double flattenFactor = 0.983;

        // Pre-compute 3D Location Vector of Great Dark Spot Center (at 20° S Latitude)
        double spotLatRad = Math.toRadians(-20.0);
        double spotLonRad = Math.toRadians(45.0) + neptuneRotationAngle;

        // Pre-compute 3D Location Vector for the fast-drifting Scooter Cloud (at 42° S
        // Latitude)
        double scooterLatRad = Math.toRadians(-42.0);
        double scooterLonRad = Math.toRadians(12.0) + (neptuneRotationAngle * 1.55);

        // -------------------------------------------------------------
        // Step 3: Render Faint 1-Pixel Wide Dust Ring Filaments (With Dynamic Pitch &
        // Tilt)
        // -------------------------------------------------------------
        // Base planar tilt oscillation (roll around the camera Z axis)
        double dynamicRingTilt = axialTilt + Math.toRadians(3.0) * Math.sin(neptuneRotationAngle * 1.8);
        double cosRingTilt = Math.cos(dynamicRingTilt);
        double sinRingTilt = Math.sin(dynamicRingTilt);

        // NEW: Pitch oscillation (tilts the ring toward/away from the camera
        // perspective around the X axis)
        double ringPitchWobble = Math.toRadians(2.5) * Math.cos(neptuneRotationAngle * 0.7);
        double cosPitch = Math.cos(ringPitchWobble);
        double sinPitch = Math.sin(ringPitchWobble);

        for (double rRad = 1.55; rRad <= 1.62; rRad += 0.07) {
            double stepSize = 0.005 / rRad;
            for (double phi = 0; phi < 2 * Math.PI; phi += stepSize) {
                // Flat equatorial dust coordinates
                double lx = rRad * Math.cos(phi);
                double ly = rRad * Math.sin(phi);
                double lz = 0.0;

                // NEW: Apply pitch transformation first (rotates Y and Z relative to camera
                // look-vector)
                double px = lx;
                double py = ly * cosPitch - lz * sinPitch;
                double pz = ly * sinPitch + lz * cosPitch;

                // Apply planar axial tilt roll transformation
                double rx = px * cosRingTilt - pz * sinRingTilt;
                double ry = py;
                double rz = px * sinRingTilt + pz * cosRingTilt;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        // Recalculate shading normal based on combined transformations
                        double ringDot = Math.abs(-sinRingTilt * lightY + cosRingTilt * lightZ);
                        double ringShade = 0.15 + ringDot;
                        int r = (int) (RGB_RING[0] * ringShade);
                        int g = (int) (RGB_RING[1] * ringShade);
                        int b = (int) (RGB_RING[2] * ringShade);
                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
                        outputBuffer[index] = colorCode + "·" + RESET;
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Step 4: Render Uniform Methane-Absorbing Spheroid Globe
        // -------------------------------------------------------------
        for (double theta = 0.008; theta < Math.PI; theta += 0.008) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            double latDeg = Math.toDegrees((Math.PI / 2.0) - theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.008) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double ux = sinTheta * cosPhi;
                double uy = sinTheta * sinPhi;
                double uz = cosTheta;

                double lx = sphereRadius * ux;
                double ly = sphereRadius * uy;
                double lz = sphereRadius * uz * flattenFactor;

                double rx = lx * cosTilt - lz * sinTilt;
                double ry = ly;
                double rz = lx * sinTilt + lz * cosTilt;

                double ooz = 1.0 / (ry + cameraDistance);
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz);

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22 && ry < 0) {
                    int index = xp + 80 * yp;
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double nx = rx / sphereRadius;
                        double ny = ry / sphereRadius;
                        double nz = rz / (sphereRadius * flattenFactor * flattenFactor);
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        if (nLen > 0) {
                            nx /= nLen;
                            ny /= nLen;
                            nz /= nLen;
                        }

                        double dLatGDS = Math.toRadians(latDeg - (-20.0));
                        double dLonGDS = normalizeAngleRad(Math.atan2(uy, ux) - spotLonRad);

                        double scaledLonGDS = (dLonGDS * Math.cos(spotLatRad)) / Math.toRadians(17.0);
                        double scaledLatGDS = dLatGDS / Math.toRadians(10.0);
                        double gdsVortex = scaledLonGDS * scaledLonGDS + scaledLatGDS * scaledLatGDS;

                        double dLatCompanion = Math.toRadians(latDeg - (-25.5));
                        double dLonCompanion = normalizeAngleRad(
                                Math.atan2(uy, ux) - (spotLonRad + Math.toRadians(4.0)));
                        double compLon = (dLonCompanion * Math.cos(Math.toRadians(-25.5))) / Math.toRadians(8.0);
                        double compLat = dLatCompanion / Math.toRadians(2.5);
                        double companionCirrus = compLon * compLon + compLat * compLat;

                        double dLatScoot = Math.toRadians(latDeg - (-42.0));
                        double dLonScoot = normalizeAngleRad(Math.atan2(uy, ux) - scooterLonRad);
                        double scaledLonScoot = (dLonScoot * Math.cos(scooterLatRad)) / Math.toRadians(7.0);
                        double scaledLatScoot = dLatScoot / Math.toRadians(3.5);
                        double scooterVortex = scaledLonScoot * scaledLonScoot + scaledLatScoot * scaledLatScoot;

                        double smoothGasBlend = 0.5 + 0.3 * Math.sin(latDeg * 0.06);
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);
                        double viewAngle = Math.abs(ny);
                        double limbDarkening = 0.45 + 0.55 * Math.pow(viewAngle, 0.95);
                        double finalLuminance = (0.20 + 0.80 * Math.pow(baseLight, 1.25)) * limbDarkening;

                        String palette = " .:-=+#%@";
                        int shadeIndex = (int) (finalLuminance * (palette.length() - 1));
                        shadeIndex = Math.max(0, Math.min(palette.length() - 1, shadeIndex));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;

                        if (scooterVortex < 1.0) {
                            double coreFade = 1.0 - scooterVortex;
                            outR = (int) (C_SCOUTER[0] * (0.92 + 0.08 * coreFade));
                            outG = (int) (C_SCOUTER[1] * (0.92 + 0.08 * coreFade));
                            outB = (int) (C_SCOUTER[2]);
                        } else if (companionCirrus < 1.0) {
                            double coreFade = 1.0 - companionCirrus;
                            outR = (int) (C_SCOUTER[0] * (0.90 + 0.10 * coreFade));
                            outG = (int) (C_SCOUTER[1] * (0.90 + 0.10 * coreFade));
                            outB = (int) (C_SCOUTER[2]);
                        } else if (gdsVortex < 1.0) {
                            double coreFade = 1.0 - gdsVortex;
                            outR = (int) (C_GDS_CORE[0] * (0.9 + 0.1 * coreFade));
                            outG = (int) (C_GDS_CORE[1] * (0.9 + 0.1 * coreFade));
                            outB = (int) (C_GDS_CORE[2]);
                        } else {
                            outR = (int) (C_BASE_COBALT[0] * (0.85 + 0.15 * smoothGasBlend));
                            outG = (int) (C_BASE_COBALT[1] * (0.85 + 0.15 * smoothGasBlend));
                            outB = (int) (C_BASE_COBALT[2]);
                        }

                        outR = Math.max(0, Math.min(255, (int) (outR * finalLuminance)));
                        outG = Math.max(0, Math.min(255, (int) (outG * finalLuminance)));
                        outB = Math.max(0, Math.min(255, (int) (outB * finalLuminance)));
                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }
    }

    private double normalizeAngleRad(double angle) {
        while (angle < -Math.PI)
            angle += 2.0 * Math.PI;
        while (angle > Math.PI)
            angle -= 2.0 * Math.PI;
        return angle;
    }
}