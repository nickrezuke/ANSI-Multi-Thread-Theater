public class MengerSpongeLoader extends Loader { 
    private static final StatusStage[] FRACTAL_STAGES = { 
        new StatusStage(25, "Seeding recursive coordinate cells:"), 
        new StatusStage(50, "Carving voxel sub-grid arrays:"), 
        new StatusStage(75, "Tracing infinite ray intersections:"), 
        new StatusStage(100, "Fractal Spatial Loop Stable!") 
    }; 

    private static final char[] SHADE_RAMP = { ' ', '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' }; 
    private double zoomTimer = 0.0; 
    private double rotationAngle = 0.0; 

    public MengerSpongeLoader() { 
        super(FRACTAL_STAGES, 130, 32); 
    } 

    @Override 
    protected void initialize() { 
        zoomTimer = 0.0; 
        rotationAngle = 0.0; 
    } 

    private double maxComponent(double x, double y, double z) {
        return Math.max(x, Math.max(y, z));
    }

    // --- CONTINUOUS MENGER SPONGE SIGNED DISTANCE FIELD (SDF) --- 
    private double evaluateMengerSDF(double x, double y, double z) {
        double spacing = 2.0;
        x = (x % spacing + spacing) % spacing - spacing * 0.5;
        y = (y % spacing + spacing) % spacing - spacing * 0.5;
        z = (z % spacing + spacing) % spacing - spacing * 0.5;

        double bX = Math.abs(x) - 1.0;
        double bY = Math.abs(y) - 1.0;
        double bZ = Math.abs(z) - 1.0;
        double d = maxComponent(bX, bY, bZ);

        double scale = 1.0;
        double rAngle = rotationAngle * 0.2; 
        double cosR = Math.cos(rAngle), sinR = Math.sin(rAngle);

        for (int i = 0; i < 3; i++) {
            x = Math.abs(x); y = Math.abs(y); z = Math.abs(z);
            if (x < y) { double t = x; x = y; y = t; }
            if (x < z) { double t = x; x = z; z = t; }
            if (y < z) { double t = y; y = z; z = t; }

            double tx = x * cosR - y * sinR;
            y = x * sinR + y * cosR;
            x = tx;

            x = x * 3.0 - 1.0;
            y = y * 3.0 - 1.0;
            z = z * 3.0 - 1.0;
            scale *= 3.0;

            if (x < -1.0) x += 2.0;
            if (y < -1.0) y += 2.0;
            if (z < -1.0) z += 2.0;

            double cX = 1.0 - Math.abs(x);
            double cY = 1.0 - Math.abs(y);
            double cZ = 1.0 - Math.abs(z);
            
            double crossDist = maxComponent(Math.min(cX, cY), Math.min(cY, cZ), Math.min(cX, cZ)) / scale;
            d = Math.max(d, crossDist);
        }
        return d;
    }

    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        zoomTimer += 0.015; 
        rotationAngle += 0.008; 

        double progress = zoomTimer % 1.0; 
        double currentScale = Math.pow(3.0, progress); 

        // We only need one rotation angle now for the barrel roll effect
        double cosA = Math.cos(rotationAngle), sinA = Math.sin(rotationAngle); 

        double lightX = 0.577, lightY = -0.577, lightZ = -0.577; 

        double[] rawBrightnessValues = new double[4160]; 
        int[][] hitRGBs = new int[4160][3]; 
        boolean[] pixelHits = new boolean[4160]; 
        double maxCalculatedBrightness = 0.01;

        for (int screenY = 0; screenY < 32; screenY++) { 
            double uvY = (screenY - 16.0) / 16.0; 
            for (int screenX = 0; screenX < 130; screenX++) { 
                int index = screenX + 130 * screenY; 
                double uvX = (screenX - 65.0) / 65.0 * 2.3; 

                double rayDirX = uvX; 
                double rayDirY = uvY; 
                double rayDirZ = 1.5; 

                double rLen = Math.sqrt(rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ); 
                rayDirX /= rLen; rayDirY /= rLen; rayDirZ /= rLen; 

                // Stare straight down the Z-axis (tunnel) and apply a barrel roll to X/Y.
                double rx = rayDirX * cosA - rayDirY * sinA; 
                double ry = rayDirX * sinA + rayDirY * cosA; 
                double rz = rayDirZ; 

                // Camera remains perfectly anchored inside the open airway lane.
                double cameraX = 0.50 / currentScale; 
                double cameraY = 0.50 / currentScale;
                double cameraZ = -1.5 / currentScale; 

                double distanceMarched = 0.01; 
                boolean hitFound = false; 
                int maxSteps = 65; 
                int stepCount = 0;

                for (int step = 0; step < maxSteps; step++) { 
                    stepCount++;
                    double px = cameraX + rx * distanceMarched; 
                    double py = cameraY + ry * distanceMarched; 
                    double pz = cameraZ + rz * distanceMarched; 

                    double safeDistance = evaluateMengerSDF(px * currentScale, py * currentScale, pz * currentScale) / currentScale; 

                    if (safeDistance < 0.0004) { 
                        hitFound = true; 
                        break; 
                    } 
                    distanceMarched += safeDistance; 
                    if (distanceMarched > 4.0) break; 
                } 

                if (hitFound) { 
                    double inverseDepth = 1.0 / distanceMarched; 
                    if (inverseDepth > zBuffer[index]) { 
                        zBuffer[index] = inverseDepth; 
                        pixelHits[index] = true;

                        double hitX = cameraX + rx * distanceMarched; 
                        double hitY = cameraY + ry * distanceMarched; 
                        double hitZ = cameraZ + rz * distanceMarched; 

                        double eps = 0.0005; 
                        double nX = evaluateMengerSDF((hitX + eps) * currentScale, hitY * currentScale, hitZ * currentScale) - evaluateMengerSDF((hitX - eps) * currentScale, hitY * currentScale, hitZ * currentScale); 
                        double nY = evaluateMengerSDF(hitX * currentScale, (hitY + eps) * currentScale, hitZ * currentScale) - evaluateMengerSDF(hitX * currentScale, (hitY - eps) * currentScale, hitZ * currentScale); 
                        double nZ = evaluateMengerSDF(hitX * currentScale, hitY * currentScale, (hitZ + eps) * currentScale) - evaluateMengerSDF(hitX * currentScale, hitY * currentScale, (hitZ - eps) * currentScale); 

                        double nMag = Math.sqrt(nX * nX + nY * nY + nZ * nZ); 
                        if (nMag > 0) { nX /= nMag; nY /= nMag; nZ /= nMag; } 

                        double dotLight = Math.max(0.0, nX * lightX + nY * lightY + nZ * lightZ); 
                        
                        double aoFactor = 1.0 - ((double) stepCount / maxSteps * 0.45);
                        double shadowWeight = (0.35 + 0.65 * dotLight) * aoFactor; 

                        double colorHue = (progress * 0.25 + (distanceMarched * 0.15)) % 1.0;
                        hitRGBs[index] = hsvToRgb(colorHue, 0.90, 0.95); 
                        rawBrightnessValues[index] = shadowWeight;

                        if (shadowWeight > maxCalculatedBrightness) { 
                            maxCalculatedBrightness = shadowWeight; 
                        }
                    } 
                } 
            } 
        } 

        // --- FINAL EMBED RENDER BLITTING PASS ---
        for (int k = 0; k < 4160; k++) { 
            if (!pixelHits[k]) continue; 

            double normalizedLuminance = rawBrightnessValues[k] / maxCalculatedBrightness; 
            normalizedLuminance = Math.pow(normalizedLuminance, 0.65); 

            int r = (int) (hitRGBs[k][0] * normalizedLuminance * 1.5); 
            int g = (int) (hitRGBs[k][1] * normalizedLuminance * 1.5); 
            int b = (int) (hitRGBs[k][2] * normalizedLuminance * 1.5); 

            if (r > 255) r = 255; if (g > 255) g = 255; if (b > 255) b = 255; 

            int shadeIndex = (int) (normalizedLuminance * (SHADE_RAMP.length - 1)); 
            shadeIndex = Math.clamp(shadeIndex, 0, SHADE_RAMP.length - 1);

            char renderChar = SHADE_RAMP[shadeIndex]; 
            String colorCode = String.format("\u001B[38;2;%d;%d;%dm", r, g, b); 
            outputBuffer[k] = colorCode + renderChar + "\u001B[0m"; // Replaced RESET with exact ANSI
        } 
    } 

    private int[] hsvToRgb(double h, double s, double v) { 
        int r = 0, g = 0, b = 0; 
        int i = (int) (h * 6); 
        double f = h * 6 - i; 
        double p = v * (1 - s); 
        double q = v * (1 - f * s); 
        double t = v * (1 - (1 - f) * s); 
        switch (i % 6) { 
            case 0: r = (int) (v*255); g = (int) (t*255); b = (int) (p*255); break; 
            case 1: r = (int) (q*255); g = (int) (v*255); b = (int) (p*255); break; 
            case 2: r = (int) (p*255); g = (int) (v*255); b = (int) (t*255); break; 
            case 3: r = (int) (p*255); g = (int) (q*255); b = (int) (v*255); break; 
            case 4: r = (int) (t*255); g = (int) (p*255); b = (int) (v*255); break; 
            case 5: r = (int) (v*255); g = (int) (p*255); b = (int) (q*255); break; 
        } 
        return new int[] { r, g, b }; 
    } 
}