import java.util.Random;

public class MarsLoader extends Loader {
    private static final StatusStage[] MARS_STAGES = {
            new StatusStage(30, "Mapping iron oxide-rich surface regolith:"),
            new StatusStage(65, "Calibrating Tharsis volcanic plateau elevations:"),
            new StatusStage(90, "Syncing Phobos and Deimos orbital tracking:"),
            new StatusStage(100, "Martian Topographic Matrix Active!")
    };

    private static final int MAX_STARS = 35;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS];
    private double marsRotationAngle = -Math.PI / 2.0;

    // Scientifically accurate Martian palette registers
    private static final int[] C_RUST_RED       = { 190, 75, 45  }; 
    private static final int[] C_DARK_BASALT    = { 105, 40, 25  }; 
    private static final int[] C_BRIGHT_DUST    = { 225, 140, 85 }; 
    private static final int[] C_POLAR_ICE      = { 240, 240, 245 }; 
    private static final int[] C_ATMOSPHERE_GLOW = { 210, 110, 70 }; 

    // Moon palettes
    private static final int[] C_PHOBOS = { 130, 120, 115 };
    private static final int[] C_DEIMOS = { 150, 140, 130 };

    public MarsLoader() {
        super(MARS_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.marsRotationAngle = -Math.PI / 2.0;
        Random rand = new Random(4321);
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = 1 + rand.nextInt(20);
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw Deep Space Background Starfield
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int starIdx = starPositions[i];
            double twinkleFactor = Math.sin((currentTime * 0.003) + starPhases[i]);
            char starChar = ' ';
            if (twinkleFactor > 0.85) starChar = '*';
            else if (twinkleFactor > 0.30) starChar = '.';
            else if (twinkleFactor > -0.2) starChar = '·';

            if (starChar != ' ' && starIdx >= 0 && starIdx < 1760) {
                zBuffer[starIdx] = 0.0001;
                outputBuffer[starIdx] = "\u001B[37m" + starChar + RESET;
            }
        }

        // Step 2: Global Planetary Kinematics
        marsRotationAngle += 0.002;

        double axialTilt = Math.toRadians(25.2);
        double cosTilt = Math.cos(axialTilt);
        double sinTilt = Math.sin(axialTilt);

        // Normalize light vector for accurate shadow raycasting
        double lxRaw = 0.60, lyRaw = -0.70, lzRaw = 0.38;
        double lLen = Math.sqrt(lxRaw * lxRaw + lyRaw * lyRaw + lzRaw * lzRaw);
        double lightX = lxRaw / lLen, lightY = lyRaw / lLen, lightZ = lzRaw / lLen;
        
        double cameraDistance = 3.85;
        double sphereRadius = 1.0;
        double flattenFactor = 1.0; 

        // Calculate Moons' orbital positions (equatorial orbits, mathematically tilted with Mars)
        // Phobos (Inner, faster, larger)
        double pOrbitAngle = marsRotationAngle * 3.5;
        double pDist = 1.5;
        double pX_eq = pDist * Math.cos(pOrbitAngle);
        double pY_eq = pDist * Math.sin(pOrbitAngle);
        double pZ_eq = Math.sin(pOrbitAngle) * 0.08; // slight wobble
        
        double phobosX = pX_eq * cosTilt - pZ_eq * sinTilt;
        double phobosY = pY_eq;
        double phobosZ = pX_eq * sinTilt + pZ_eq * cosTilt;
        double phobosRadius = 0.12; // Exaggerated scale for visibility

        // Deimos (Outer, slower, smaller)
        double dOrbitAngle = marsRotationAngle * 0.8;
        double dDist = 2.15;
        double dX_eq = dDist * Math.cos(dOrbitAngle);
        double dY_eq = dDist * Math.sin(dOrbitAngle);
        double dZ_eq = Math.sin(dOrbitAngle) * -0.05;
        
        double deimosX = dX_eq * cosTilt - dZ_eq * sinTilt;
        double deimosY = dY_eq;
        double deimosZ = dX_eq * sinTilt + dZ_eq * cosTilt;
        double deimosRadius = 0.08; 

        // -------------------------------------------------------------
        // Step 3: Render Procedural Martian Terrain & Eclipse Shadows
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

                // Apply axial tilt
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
                            nx /= nLen; ny /= nLen; nz /= nLen;
                        }

                        // Surface features mapping
                        double currentLon = Math.atan2(uy, ux) + marsRotationAngle;
                        boolean isPolar = Math.abs(latDeg) > 68.0;

                        double terrainPattern = Math.sin(latDeg * 0.07) * Math.cos(currentLon * 2.5) 
                                              + 0.5 * Math.sin(currentLon * 5.0 - latDeg * 0.03) 
                                              + 0.3 * Math.cos(latDeg * 0.2);
                        double terrainMix = Math.max(0.0, Math.min(1.0, (terrainPattern + 1.5) / 3.0));

                        // Base Lighting
                        double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                        double baseLight = Math.max(0.0, diffuse);

                        // Eclipse Raycasting (Do the moons cast shadows on this voxel?)
                        // 1. Phobos Shadow
                        double vX = phobosX - rx, vY = phobosY - ry, vZ = phobosZ - rz;
                        double t = vX * lightX + vY * lightY + vZ * lightZ; // Ray distance
                        if (t > 0) { // Moon is between surface and light source
                            double closeX = rx + t * lightX, closeY = ry + t * lightY, closeZ = rz + t * lightZ;
                            double distSq = (closeX - phobosX)*(closeX - phobosX) + (closeY - phobosY)*(closeY - phobosY) + (closeZ - phobosZ)*(closeZ - phobosZ);
                            if (distSq < phobosRadius * phobosRadius) baseLight *= 0.1; // Umbra
                            else if (distSq < phobosRadius * phobosRadius * 1.6) baseLight *= 0.5; // Penumbra
                        }

                        // 2. Deimos Shadow
                        vX = deimosX - rx; vY = deimosY - ry; vZ = deimosZ - rz;
                        t = vX * lightX + vY * lightY + vZ * lightZ;
                        if (t > 0) {
                            double closeX = rx + t * lightX, closeY = ry + t * lightY, closeZ = rz + t * lightZ;
                            double distSq = (closeX - deimosX)*(closeX - deimosX) + (closeY - deimosY)*(closeY - deimosY) + (closeZ - deimosZ)*(closeZ - deimosZ);
                            if (distSq < deimosRadius * deimosRadius) baseLight *= 0.1; 
                            else if (distSq < deimosRadius * deimosRadius * 1.6) baseLight *= 0.5;
                        }

                        double viewAngle = Math.abs(ny);
                        double finalLuminance = 0.2 + 0.8 * Math.pow(baseLight, 1.2);

                        String palette = " .:-=+#%@";
                        int shadeIndex = Math.max(0, Math.min(palette.length() - 1, (int) (finalLuminance * (palette.length() - 1))));
                        char renderChar = palette.charAt(shadeIndex);

                        int outR, outG, outB;
                        if (isPolar) {
                            outR = C_POLAR_ICE[0]; outG = C_POLAR_ICE[1]; outB = C_POLAR_ICE[2];
                        } else {
                            if (terrainMix < 0.4) {
                                double tm = terrainMix / 0.4;
                                outR = (int) (C_DARK_BASALT[0] * (1.0 - tm) + C_RUST_RED[0] * tm);
                                outG = (int) (C_DARK_BASALT[1] * (1.0 - tm) + C_RUST_RED[1] * tm);
                                outB = (int) (C_DARK_BASALT[2] * (1.0 - tm) + C_RUST_RED[2] * tm);
                            } else {
                                double tm = (terrainMix - 0.4) / 0.6;
                                outR = (int) (C_RUST_RED[0] * (1.0 - tm) + C_BRIGHT_DUST[0] * tm);
                                outG = (int) (C_RUST_RED[1] * (1.0 - tm) + C_BRIGHT_DUST[1] * tm);
                                outB = (int) (C_RUST_RED[2] * (1.0 - tm) + C_BRIGHT_DUST[2] * tm);
                            }
                        }

                        if (viewAngle < 0.25 && !isPolar) {
                            double rimFactor = (0.25 - viewAngle) / 0.25;
                            outR = (int) (outR * (1.0 - rimFactor * 0.4) + C_ATMOSPHERE_GLOW[0] * (rimFactor * 0.4));
                            outG = (int) (outG * (1.0 - rimFactor * 0.4) + C_ATMOSPHERE_GLOW[1] * (rimFactor * 0.4));
                            outB = (int) (outB * (1.0 - rimFactor * 0.4) + C_ATMOSPHERE_GLOW[2] * (rimFactor * 0.4));
                        }

                        outR = Math.max(0, Math.min(255, (int) (outR * finalLuminance)));
                        outG = Math.max(0, Math.min(255, (int) (outG * finalLuminance)));
                        outB = Math.max(0, Math.min(255, (int) (outB * finalLuminance)));

                        outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm%c%s", outR, outG, outB, renderChar, RESET);
                    }
                }
            }
        }

        // Step 4: Render Moons (Z-Buffered to pass cleanly in front of/behind Mars)
        renderMoon(outputBuffer, zBuffer, phobosX, phobosY, phobosZ, phobosRadius, lightX, lightY, lightZ, cameraDistance, C_PHOBOS);
        renderMoon(outputBuffer, zBuffer, deimosX, deimosY, deimosZ, deimosRadius, lightX, lightY, lightZ, cameraDistance, C_DEIMOS);
    }

    private void renderMoon(String[] outputBuffer, double[] zBuffer, double mX, double mY, double mZ, 
                            double radius, double lightX, double lightY, double lightZ, 
                            double cameraDistance, int[] color) {
        
        double step = 0.015; // High density voxel rasterization
        for (double dx = -radius; dx <= radius; dx += step) {
            for (double dy = -radius; dy <= radius; dy += step) {
                for (double dz = -radius; dz <= radius; dz += step) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        double worldX = mX + dx;
                        double worldY = mY + dy;
                        double worldZ = mZ + dz;

                        // Ensure it's in front of the camera to avoid perspective division errors
                        if (worldY + cameraDistance < 0.1) continue; 

                        double ooz = 1.0 / (worldY + cameraDistance);
                        int xp = (int) (40 + 74 * ooz * worldX);
                        int yp = (int) (11 - 36 * ooz * worldZ);

                        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
                            int index = xp + 80 * yp;
                            
                            if (ooz > zBuffer[index] + 0.0001) {
                                zBuffer[index] = ooz;

                                double nx = dx / radius;
                                double ny = dy / radius;
                                double nz = dz / radius;
                                
                                double diffuse = nx * lightX + ny * lightY + nz * lightZ;
                                double finalLum = 0.2 + 0.8 * Math.max(0, diffuse);
                                
                                char glyph = (diffuse > 0.6) ? '@' : (diffuse > 0.2) ? 'O' : (diffuse > -0.2) ? '*' : '.';
                                
                                int outR = Math.max(0, Math.min(255, (int)(color[0] * finalLum)));
                                int outG = Math.max(0, Math.min(255, (int)(color[1] * finalLum)));
                                int outB = Math.max(0, Math.min(255, (int)(color[2] * finalLum)));

                                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm%c%s", outR, outG, outB, glyph, RESET);
                            }
                        }
                    }
                }
            }
        }
    }
}