//TODO: Mke these looks like dogs right now theyre complete trash lmao

public class DogHeadLoader extends Loader {
    private static final StatusStage[] DOG_STAGES = {
            new StatusStage(20, "Sniffing the yard:"),
            new StatusStage(40, "Chasing squirrels:"),
            new StatusStage(70, "Fetching the ball:"),
            new StatusStage(95, "Waiting by the door:"),
            new StatusStage(100, "Woof! Woof!! Bark!!")
    };
    private static final char[] SHADE_RAMP = { ':', ';', '=', '!', '*', '#', '$', '@', '▒', '▓', '█' };

    private String furColor;
    private String secondaryFurColor; // Mask / spots / saddle, depending on breed
    private String tertiaryFurColor;  // Third tone (tri-color patches / white blaze)
    private String earColor;
    private String noseColor;
    private String eyeBorderColor;
    private String eyePupilColor;
    private String whiskerColor;
    private int breedVariant = 1;
    private double A = 0;

    // Golden Retriever: which side gets slightly heavier crown feathering
    private double crownFeatherSide;

    // Dalmatian: oversized, iconic "over-the-eye" spot side & randomized body spots
    private double dalmatianEyeSpotSide;
    private double[] spotX, spotY, spotZ, spotR;

    // Husky: per-instance mask width so no two huskies have an identical mask edge
    private double huskyMaskWidth;

    // American Foxhound: how far down the saddle patch extends
    private double saddleDepth;

    // Snout length varies by breed
    private double snoutLength;

    public DogHeadLoader() {
        super(DOG_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        breedVariant = (int) (Math.random() * 4) + 1;

        crownFeatherSide = Math.random() < 0.5 ? -1.0 : 1.0;
        dalmatianEyeSpotSide = Math.random() < 0.5 ? -1.0 : 1.0;
        huskyMaskWidth = 0.28 + Math.random() * 0.08;
        saddleDepth = 0.05 + Math.random() * 0.12;

        int spotCount = 14;
        spotX = new double[spotCount];
        spotY = new double[spotCount];
        spotZ = new double[spotCount];
        spotR = new double[spotCount];
        for (int i = 0; i < spotCount; i++) {
            spotX[i] = (Math.random() * 2 - 1) * 1.10;
            spotY[i] = (Math.random() * 2 - 1) * 0.85;
            spotZ[i] = (Math.random() * 2 - 1) * 0.90;
            spotR[i] = 0.10 + Math.random() * 0.07;
        }

        switch (breedVariant) {
            case 1: // --- 1. GOLDEN RETRIEVER ---
                furColor = "\u001B[38;5;179m";          // Warm Honey Gold
                secondaryFurColor = "\u001B[38;5;172m"; // Slightly Darker Amber (crown feathering)
                tertiaryFurColor = "\u001B[38;5;223m";  // Pale Golden (muzzle highlight)
                earColor = "\u001B[38;5;173m";          // Rich Caramel Floppy Ears
                noseColor = "\u001B[38;5;232m";          // Deep Black Nose Leather
                eyeBorderColor = "\u001B[38;5;94m";     // Warm Chestnut Brown Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.28;
                break;

            case 2: // --- 2. DALMATIAN ---
                furColor = "\u001B[38;5;255m";          // Crisp White Base Coat
                secondaryFurColor = "\u001B[38;5;232m"; // Ink Black Spots
                tertiaryFurColor = secondaryFurColor;
                earColor = "\u001B[38;5;232m";          // Classic Solid Black Ear
                noseColor = "\u001B[38;5;232m";
                eyeBorderColor = "\u001B[38;5;94m";     // Deep Brown Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.18;
                break;

            case 3: // --- 3. HUSKY ---
                furColor = "\u001B[38;5;251m";          // Cool Silver-Grey Base
                secondaryFurColor = "\u001B[38;5;236m"; // Charcoal/Black Mask & Cap
                tertiaryFurColor = "\u001B[38;5;255m";  // Bright White Muzzle/Cheeks
                earColor = "\u001B[38;5;239m";          // Dark Grey Erect Ears
                noseColor = "\u001B[38;5;232m";
                eyeBorderColor = "\u001B[38;5;153m";    // Striking Icy Blue Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.10;
                break;

            case 4:
            default: // --- 4. AMERICAN FOXHOUND ---
                furColor = "\u001B[38;5;255m";          // White Base
                secondaryFurColor = "\u001B[38;5;232m"; // Black Saddle
                tertiaryFurColor = "\u001B[38;5;173m";  // Tan/Brown Patches
                earColor = "\u001B[38;5;173m";          // Tan Floppy Ears
                noseColor = "\u001B[38;5;232m";
                eyeBorderColor = "\u001B[38;5;130m";    // Amber-Brown Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.45;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // STEP 1: RENDER SCULPTED CANINE SKULL (Replaces generic spheroid)
        for (int tIndex = 0; tIndex < 120; tIndex++) {
            double theta = (tIndex / 120.0) * Math.PI;
            double sinTheta = Math.sin(theta), cosTheta = Math.cos(theta);
            for (int pIndex = 0; pIndex < 240; pIndex++) {
                double phi = (pIndex / 240.0) * 2.0 * Math.PI;
                double sinPhi = Math.sin(phi), cosPhi = Math.max(-1.0, Math.min(1.0, Math.cos(phi)));

                // Base shape: slightly narrower, natural depth
                double localX = 1.10 * sinTheta * cosPhi;
                double localY = cosTheta;
                double localZ = 0.95 * sinTheta * sinPhi;

                // Sculpting the skull: flatter crown, wider cheeks/jowls
                if (localY > 0) {
                    localX *= 1.15; // Thicker cheeks
                    localY *= 1.05; // Slightly elongated jaw line
                } else {
                    localY *= 0.85; // Flatter forehead/crown
                    localZ *= 1.05; // Longer back of skull
                }

                double rNx = sinTheta * cosPhi;
                double rNy = cosTheta;
                double rNz = sinTheta * sinPhi;

                plotProjectedPoint(localX, localY, localZ, rNx, rNy, rNz, 0, cosA, sinA, lightX, lightY, lightZ,
                        outputBuffer, zBuffer);
            }
        }

        // STEP 2: ROUNDED TAPERED MUZZLE (Replaces the blocky rectangular snout)
        double actualSnoutLen = snoutLength * 0.65; // Scaled for better proportion
        for (double sz = 0.0; sz <= actualSnoutLen; sz += 0.035) {
            double progress = sz / actualSnoutLen;
            double taper = 1.0 - 0.35 * progress; // Muzzle naturally narrows towards the nose
            
            // Creates a gentle dip/arc on the bridge of the nose
            double bridgeArc = Math.sin(progress * Math.PI) * 0.04;

            for (double angle = 0; angle < Math.PI * 2; angle += 0.12) {
                double sx = Math.cos(angle);
                double sy = Math.sin(angle);
                
                double localX = sx * 0.32 * taper;
                // Offset Y to build out upper bridge vs lower jaw
                double localY = 0.12 + (sy > 0 ? sy * 0.28 * taper : sy * 0.20 * taper) + (sy < 0 ? bridgeArc : 0);
                double localZ = -0.75 - sz + (sy > 0 ? 0.05 * progress : 0); // Slight underbite slant

                int surfaceType = 0;
                double rNx = sx;
                double rNy = sy;
                double rNz = -0.3; // Slight forward normal

                // Black nose leather cap right at the tip
                if (progress > 0.92) {
                    surfaceType = 2;
                    localX *= 1.08; // Slightly bulbous nose
                    localY *= 1.08;
                }

                plotProjectedPoint(localX, localY, localZ, rNx, rNy, rNz, surfaceType, cosA, sinA, lightX,
                        lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // STEP 3: BREED-SPECIFIC EAR LAYER
        if (breedVariant == 3) {
            renderPointyEars(cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
        } else {
            renderFloppyEars(cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }

        // STEP 4: PERSPECTIVE-SQUEEZED DYNAMIC EYE BILLBOARD TRACKER
        // Adjusted anchor points to sit flush on the newly sculpted skull
        double localEyeY = -0.22;
        double localEyeZ = -0.80;
        double leftLocalEyeX = -0.35;
        double rightLocalEyeX = 0.35;
        trackAndRenderSqueezedEye(leftLocalEyeX, localEyeY, localEyeZ, cosA, sinA, outputBuffer, zBuffer);
        trackAndRenderSqueezedEye(rightLocalEyeX, localEyeY, localEyeZ, cosA, sinA, outputBuffer, zBuffer);

        // STEP 5: 4-WHISKER LINEAR LAYER
        for (double side : new double[] { -1.0, 1.0 }) {
            double rootX = side * 0.14;
            double rootY = 0.15;
            double rootZ = -0.75 - actualSnoutLen * 0.85; // Rooted dynamically near nose tip

            renderWhiskerLine(rootX, rootY - 0.02, rootZ, side * 1.10, -0.05, rootZ + 0.05, side, cosA, sinA,
                    outputBuffer, zBuffer);
            renderWhiskerLine(rootX, rootY + 0.02, rootZ, side * 1.10, 0.20, rootZ + 0.05, side, cosA, sinA,
                    outputBuffer, zBuffer);
        }

        A += 0.015;
    }

    private void renderPointyEars(double cosA, double sinA, double lightX, double lightY, double lightZ,
            String[] outputBuffer, double[] zBuffer) {
        for (double side = -1.0; side <= 1.0; side += 2.0) {
            for (double h = 0.0; h <= 1.0; h += 0.02) {
                for (double w = -1.0; w <= 1.0; w += 0.05) {
                    double earBaseX = side * 0.50;
                    double earBaseY = -0.75; // Lowered to rest on flatter crown
                    double earBaseZ = 0.0;
                    double currentWidthScale = 1.25 - h;
                    double localX = earBaseX + (side * 0.28 * h) + (w * 0.20 * currentWidthScale);
                    double localY = earBaseY - (0.55 * h);
                    double localZ = earBaseZ + (Math.abs(w) * 0.12 * currentWidthScale);
                    double rNx = side * 0.6;
                    double rNy = -0.55;
                    double rNz = 0.30;
                    plotProjectedPoint(localX, localY, localZ, rNx, rNy, rNz, 1, cosA, sinA, lightX, lightY, lightZ,
                            outputBuffer, zBuffer);
                }
            }
        }
    }

    private void renderFloppyEars(double cosA, double sinA, double lightX, double lightY, double lightZ,
            String[] outputBuffer, double[] zBuffer) {
        for (double side = -1.0; side <= 1.0; side += 2.0) {
            for (double h = 0.0; h <= 1.0; h += 0.03) {
                double droop = h * h; 
                for (double w = -1.0; w <= 1.0; w += 0.10) {
                    double earBaseX = side * 0.88; // Tucked slightly closer to narrower head
                    double earBaseY = -0.15;
                    double earBaseZ = 0.0;
                    double currentWidthScale = 1.0 - 0.35 * h;
                    double localX = earBaseX + (side * 0.12 * h) + (w * 0.20 * currentWidthScale);
                    double localY = earBaseY + (0.95 * h) + (0.25 * droop);
                    double localZ = earBaseZ - (0.04 * h) + (Math.abs(w) * 0.10 * currentWidthScale);
                    double rNx = side * 0.65;
                    double rNy = 0.45;
                    double rNz = 0.20;
                    plotProjectedPoint(localX, localY, localZ, rNx, rNy, rNz, 1, cosA, sinA, lightX, lightY, lightZ,
                            outputBuffer, zBuffer);
                }
            }
        }
    }

    private void renderWhiskerLine(double x1, double y1, double z1, double x2, double y2, double z2,
            double side, double cosA, double sinA, String[] outputBuffer, double[] zBuffer) {
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

    private void trackAndRenderSqueezedEye(double lx, double ly, double lz, double cosA, double sinA,
            String[] outputBuffer, double[] zBuffer) {
        double rx = lx * cosA + lz * sinA;
        double ry = ly;
        double rz = -lx * sinA + lz * cosA;

        if (rz > 0.0) return;

        double distanceToCamera = 4.0;
        double ooz = 1.0 / (rz + distanceToCamera);
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
                        if (eyeOozBias > (zBuffer[targetIndex] - 0.001)) {
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
                if (surfaceType == 1) {
                    chosenColor = earColor;
                } else if (surfaceType == 2) {
                    chosenColor = noseColor;
                } else if (surfaceType == 0) {
                    chosenColor = resolveBreedMarking(localX, localY, localZ, chosenColor);
                }

                outputBuffer[bufferIndex] = chosenColor + asciiChar + RESET;
            }
        }
    }

    private String resolveBreedMarking(double localX, double localY, double localZ, String baseColor) {
        if (breedVariant == 1) {
            // GOLDEN RETRIEVER: Added pale highlight around the lower muzzle for depth
            boolean muzzleHighlight = localZ < -0.7 && localY > 0.05;
            if (muzzleHighlight) {
                return tertiaryFurColor;
            }
            boolean crownFeather = (localX * crownFeatherSide > 0.15) && localY < -0.45 && localZ > -0.10;
            if (crownFeather) {
                return secondaryFurColor;
            }
            return baseColor;
        }

        if (breedVariant == 2) {
            // DALMATIAN: Maintained classic single eye-drape spot & random scatter
            double eyeSpotX = dalmatianEyeSpotSide * 0.38;
            double dex = localX - eyeSpotX;
            double dey = localY - (-0.30);
            double dez = localZ - (-0.65);
            double eyeSpotDist = (dex * dex * 1.3) + (dey * dey * 1.7) + (dez * dez * 1.1);
            if (eyeSpotDist < 0.065) {
                return secondaryFurColor;
            }

            for (int i = 0; i < spotX.length; i++) {
                double ddx = localX - spotX[i];
                double ddy = localY - spotY[i];
                double ddz = localZ - spotZ[i];
                double distSq = ddx * ddx + ddy * ddy + ddz * ddz;
                if (distSq < spotR[i] * spotR[i]) {
                    return secondaryFurColor;
                }
            }
            return baseColor;
        }

        if (breedVariant == 3) {
            // HUSKY: Overhauled to produce a classic "widow's peak" and eye spectacles
            boolean onMuzzleBridge = localZ < -0.45 && Math.abs(localX) < 0.22;
            boolean onLowerCheek = localY > 0.10 && localZ < 0.0;
            boolean eyeBrowDots = localY < -0.30 && localY > -0.40 && Math.abs(Math.abs(localX) - 0.22) < 0.06 && localZ < -0.65;
            
            if (onMuzzleBridge || onLowerCheek || eyeBrowDots) {
                return tertiaryFurColor; // bright white
            }

            // Cap and mask extending organically down the snout sides
            double maskEdge = huskyMaskWidth + 0.2 * Math.sin(Math.max(0, localY) * 3.14);
            boolean onMask = localY < 0.05 && Math.abs(localX) < maskEdge && localZ < -0.2;
            if (onMask) {
                return secondaryFurColor; // dark mask/cap
            }
            return baseColor; // grey sides
        }

        // AMERICAN FOXHOUND: Improved saddle flow and distinct tan cheek patches
        boolean saddle = localY < (-0.05 - saddleDepth) && localZ > -0.10;
        if (saddle) {
            return secondaryFurColor; // black
        }

        boolean tanPatch = Math.abs(localX) > 0.30 && localY < 0.20 && localZ < 0.15;
        if (tanPatch) {
            return tertiaryFurColor; // tan
        }

        return baseColor; // white
    }
}