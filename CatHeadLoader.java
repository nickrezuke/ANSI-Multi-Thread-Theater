// TODO: Add some Cat Breed Variants?

public class CatHeadLoader extends Loader {
    private static final StatusStage[] CAT_STAGES = {
            new StatusStage(20, "Chasing Butterflies:"),
            new StatusStage(50, "Scratching Yarn Balls:"),
            new StatusStage(80, "Staring at you from the dark:"),
            new StatusStage(100, "MeowMrrrreeoww!  Meow!!")
    };
    private static final char[] SHADE_RAMP = { ':', ';', '=', '!', '*', '#', '$', '@', '▒', '▓', '█' };
    private String furColor;
    private String earColor;
    private String noseColor;
    private String eyeBorderColor;
    private String eyePupilColor;
    private String whiskerColor;
    private double A = 0;

    public CatHeadLoader() {
        // This uses 80x22 specifically
        super(CAT_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        furColor = "\u001B[38;5;235m"; // Deep Inkwell Black/Dark Gray
        earColor = "\u001B[38;5;238m"; // Muted Charcoal Outer Ear
        noseColor = "\u001B[38;5;211m"; // Pop-Art Pastel Pink Nose
        eyeBorderColor = "\u001B[38;5;112m"; // Spooky Witch-Hazel Green / Slime Lime Iris
        eyePupilColor = "\u001B[30m"; // Stark Solid Black for vertical center slit
        whiskerColor = "\u001B[38;5;255m"; // Stark White for thin whiskers
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // STEP 1: RENDER COMPLETE UNBROKEN SPHEROID HEAD
        for (int tIndex = 0; tIndex < 120; tIndex++) {
            double theta = (tIndex / 120.0) * Math.PI;
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
            for (int pIndex = 0; pIndex < 240; pIndex++) {
                double phi = (pIndex / 240.0) * 2.0 * Math.PI;
                double sinPhi = Math.sin(phi), cosPhi = Math.max(-1.0, Math.min(1.0, Math.cos(phi)));

                double localX = 1.35 * sinTheta * cosPhi;
                double localY = 0.95 * cosTheta;
                double localZ = 0.95 * sinTheta * sinPhi;
                int surfaceType = 0;

                double rNx = sinTheta * cosPhi;
                double rNy = cosTheta;
                double rNz = sinTheta * sinPhi;

                // RETRO BUTTON NOSE TRIANGLE
                if (localY > -0.02 && localY < 0.14 && localZ < -0.80) {
                    double nDistX = Math.abs(localX);
                    if (nDistX < 0.12) {
                        localZ = -1.12 + (nDistX * 1.6) + (Math.abs(localY) * 1.6);
                        surfaceType = 2;
                        rNx = (localX > 0) ? 0.7 : -0.7;
                        rNy = (localY > 0) ? 0.4 : -0.4;
                        rNz = -0.6;
                    }
                }
                plotProjectedPoint(localX, localY, localZ, rNx, rNy, rNz, surfaceType, cosA, sinA, lightX, lightY,
                        lightZ, outputBuffer, zBuffer);
            }
        }

        // STEP 2: STANDALONE INDEPENDENT POINTY EAR LAYER
        for (double side = -1.0; side <= 1.0; side += 2.0) {
            for (double h = 0.0; h <= 1.0; h += 0.02) {
                for (double w = -1.0; w <= 1.0; w += 0.05) {
                    double earBaseX = side * 0.75;
                    double earBaseY = -0.65;
                    double earBaseZ = -0.10;
                    double currentWidthScale = 1.4 - h;
                    double localX = earBaseX + (side * 0.45 * h) + (w * 0.28 * currentWidthScale);
                    double localY = earBaseY - (0.75 * h);
                    double localZ = earBaseZ + (Math.abs(w) * 0.20 * currentWidthScale);
                    double rNx = side * 0.75;
                    double rNy = -0.60;
                    double rNz = 0.30;
                    plotProjectedPoint(localX, localY, localZ, rNx, rNy, rNz, 1, cosA, sinA, lightX, lightY, lightZ,
                            outputBuffer, zBuffer);
                }
            }
        }

        // STEP 3: PERSPECTIVE-SQUEEZED DYNAMIC EYE BILLBOARD TRACKER
        double localEyeY = -0.26;
        double localEyeZ = -0.82;
        double leftLocalEyeX = -0.38;
        double rightLocalEyeX = 0.38;
        trackAndRenderSqueezedEye(leftLocalEyeX, localEyeY, localEyeZ, cosA, sinA, outputBuffer, zBuffer);
        trackAndRenderSqueezedEye(rightLocalEyeX, localEyeY, localEyeZ, cosA, sinA, outputBuffer, zBuffer);

        // STEP 4: 4-WHISKER LINEAR LAYER
        for (double side : new double[] { -1.0, 1.0 }) {
            // Anchor roots directly next to the nose edges
            double rootX = side * 0.16;
            double rootY = 0.08;
            double rootZ = -0.88;

            // Rendered only 2 clean, distinct strands per side with wide angling spreads
            renderWhiskerLine(rootX, rootY - 0.02, rootZ, side * 1.15, -0.05, -0.85, side, cosA, sinA, outputBuffer,
                    zBuffer); // Angled Up
            renderWhiskerLine(rootX, rootY + 0.02, rootZ, side * 1.15, 0.20, -0.85, side, cosA, sinA, outputBuffer,
                    zBuffer); // Angled Down
        }

        A += 0.015;
    }

    private void renderWhiskerLine(double x1, double y1, double z1, double x2, double y2, double z2,
            double side, double cosA, double sinA, String[] outputBuffer, double[] zBuffer) {
        // Reduced sample density (t += 0.05) to keep terminal lines naturally thin and
        // un-clumped
        for (double t = 0.0; t <= 1.0; t += 0.05) {
            double localX = x1 + (x2 - x1) * t;
            double localY = y1 + (y2 - y1) * t;
            double localZ = z1 + (z2 - z1) * t;

            double rx = localX * cosA + localZ * sinA;
            double ry = localY;
            double rz = -localX * sinA + localZ * cosA;

            if (rz > 0.3)
                continue;

            double distanceToCamera = 4.0;
            double ooz = 1.0 / (rz + distanceToCamera);

            int xp = (int) (40 + 38 * ooz * rx * 1.85);
            int yp = (int) (14 + 19 * ooz * ry * 1.25);

            if (yp >= 0 && yp < 22 && xp >= 0 && xp < 80) {
                int bufferIndex = xp + 80 * yp;

                if (ooz > (zBuffer[bufferIndex] - 0.02)) {
                    zBuffer[bufferIndex] = ooz;

                    char whiskerChar = '-';
                    double slope = (y2 - y1) / Math.abs(x2 - x1);

                    if (slope < -0.05) {
                        whiskerChar = (side > 0) ? '/' : '\\';
                    } else if (slope > 0.05) {
                        whiskerChar = (side > 0) ? '\\' : '/';
                    }

                    outputBuffer[bufferIndex] = whiskerColor + whiskerChar + RESET;
                }
            }
        }
    }

    private void trackAndRenderSqueezedEye(double lx, double ly, double lz, double cosA, double sinA, String[] outputBuffer, double[] zBuffer) {
        double rx = lx * cosA + lz * sinA;
        double ry = ly;
        double rz = -lx * sinA + lz * cosA;
        
        // 1. LOOSEN CULLING: Only clip if the eye rotates to the true back-half of the head
        if (rz > 0.0) return; 
    
        double distanceToCamera = 4.0;
        double ooz = 1.0 / (rz + distanceToCamera);
        
        // Apply a subtle forward depth bias to cleanly sit ahead of the head spheroid
        double eyeOozBias = ooz + 0.0125; 
    
        int cx = (int) (40 + 38 * ooz * rx * 1.85);
        int cy = (int) (14 + 19 * ooz * ry * 1.25);
        
        double surfaceNormalZ = (lx > 0) ? -cosA : cosA;
        double horizontalSqueeze = Math.max(0.20, Math.abs(surfaceNormalZ));
        
        for (int dy = -2; dy <= 2; dy++) {
            int maxDX = (int) (4 * horizontalSqueeze);
            for (int dx = -maxDX; dx <= maxDX; dx++) {
                int px = cx + dx;
                int py = cy + dy;
                
                if (px >= 0 && px < 80 && py >= 0 && py < 22) {
                    int targetIndex = px + 80 * py;
                    
                    double normX = dx / (4.0 * horizontalSqueeze);
                    double normY = dy / 2.0;
                    double distanceMetric = (normX * normX) + (normY * normY);
                    
                    if (distanceMetric <= 1.0) {
                        // 2. TIGHTEN COMPARISON: Compare using biased eye depth with a tiny floating-point margin
                        if (eyeOozBias > (zBuffer[targetIndex] - 0.001)) {
                            
                            // 3. WRITE TO Z-BUFFER: Claim this pixel space so head geometry doesn't bleed through
                            zBuffer[targetIndex] = eyeOozBias; 
                            
                            String color;
                            char glyph;
                            
                            if (distanceMetric > 0.65) {
                                color = eyeBorderColor;
                                glyph = '█';
                            } else if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                                color = eyePupilColor;
                                glyph = '█';
                            } else {
                                color = "\u001B[38;5;255m";
                                glyph = ' ';
                            }
                            outputBuffer[targetIndex] = color + glyph + RESET;
                        }
                    }
                }
            }
        }
    }    

    private void plotProjectedPoint(double localX, double localY, double localZ, double rNx, double rNy, double rNz,
            int surfaceType, double cosA, double sinA, double lightX, double lightY, double lightZ,
            String[] outputBuffer, double[] zBuffer) {
        double rx = localX * cosA + localZ * sinA;
        double ry = localY;
        double rz = -localX * sinA + localZ * cosA;

        double nx = rNx * cosA + rNz * sinA;
        double ny = rNy;
        double nz = -rNx * sinA + rNz * cosA;

        double nMag = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nMag > 0) {
            nx /= nMag;
            ny /= nMag;
            nz /= nMag;
        }
        double distanceToCamera = 4.0;
        double ooz = 1.0 / (rz + distanceToCamera);
        int xp = (int) (40 + 38 * ooz * rx * 1.85);
        int yp = (int) (14 + 19 * ooz * ry * 1.25);

        if (yp >= 0 && yp < 22 && xp >= 0 && xp < 80) {
            int bufferIndex = xp + 80 * yp;
            if (ooz > (zBuffer[bufferIndex] + 0.0001)) {
                zBuffer[bufferIndex] = ooz;
                double luminance = nx * lightX + ny * lightY + nz * lightZ;
                int shadeIndex = (int) ((luminance + 1.0) * 5.5);
                shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                char asciiChar = SHADE_RAMP[shadeIndex];
                String chosenColor = furColor;
                if (surfaceType == 1)
                    chosenColor = earColor;
                if (surfaceType == 2)
                    chosenColor = noseColor;
                outputBuffer[bufferIndex] = chosenColor + asciiChar + RESET;
            }
        }
    }
}