//TODO: Make these look more like dogs and fix the eye balls, and expand the breeds to include more dogs

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
    private String secondaryFurColor; // Mask / spots / saddle
    private String tertiaryFurColor;  // Muzzle highlight / Blaze / Tan patches
    private String earColor;
    private String noseColor;
    private String eyeBorderColor;
    private String eyePupilColor;
    private String whiskerColor;
    private int breedVariant = 1;
    private double A = 0;

    // Breed-specific dynamic parameters
    private double crownFeatherSide;
    private double dalmatianEyeSpotSide;
    private double[] spotX, spotY, spotZ, spotR;
    private double huskyMaskWidth;
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

        int spotCount = 18;
        spotX = new double[spotCount];
        spotY = new double[spotCount];
        spotZ = new double[spotCount];
        spotR = new double[spotCount];
        for (int i = 0; i < spotCount; i++) {
            spotX[i] = (Math.random() * 2 - 1) * 0.55;
            spotY[i] = (Math.random() * 2 - 1) * 0.45;
            spotZ[i] = (Math.random() * 2 - 1) * 0.40;
            spotR[i] = 0.08 + Math.random() * 0.06;
        }

        switch (breedVariant) {
            case 1: // --- 1. GOLDEN RETRIEVER ---
                furColor = "\u001B[38;5;179m";          // Honey Gold
                secondaryFurColor = "\u001B[38;5;172m"; // Amber Feathering
                tertiaryFurColor = "\u001B[38;5;223m";  // Pale Muzzle
                earColor = "\u001B[38;5;173m";          // Caramel Floppy Ears
                noseColor = "\u001B[38;5;232m";         // Black Nose
                eyeBorderColor = "\u001B[38;5;94m";     // Chestnut Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.25;
                break;

            case 2: // --- 2. DALMATIAN ---
                furColor = "\u001B[38;5;255m";          // Crisp White
                secondaryFurColor = "\u001B[38;5;232m"; // Black Spots
                tertiaryFurColor = secondaryFurColor;
                earColor = "\u001B[38;5;232m";          // Black Ear
                noseColor = "\u001B[38;5;232m";
                eyeBorderColor = "\u001B[38;5;94m";     // Deep Brown Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.18;
                break;

            case 3: // --- 3. HUSKY ---
                furColor = "\u001B[38;5;251m";          // Silver-Grey
                secondaryFurColor = "\u001B[38;5;236m"; // Charcoal Cap/Mask
                tertiaryFurColor = "\u001B[38;5;255m";  // Bright White Muzzle/Cheeks
                earColor = "\u001B[38;5;239m";          // Dark Grey Erect Ears
                noseColor = "\u001B[38;5;232m";
                eyeBorderColor = "\u001B[38;5;153m";    // Icy Blue Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.12;
                break;

            case 4:
            default: // --- 4. AMERICAN FOXHOUND ---
                furColor = "\u001B[38;5;255m";          // White Base
                secondaryFurColor = "\u001B[38;5;232m"; // Black Saddle
                tertiaryFurColor = "\u001B[38;5;173m";  // Tan Patches
                earColor = "\u001B[38;5;173m";          // Long Tan Ears
                noseColor = "\u001B[38;5;232m";
                eyeBorderColor = "\u001B[38;5;130m";    // Amber Eyes
                eyePupilColor = "\u001B[30m";
                whiskerColor = "\u001B[38;5;255m";
                snoutLength = 1.38;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        double cosA = Math.cos(A), sinA = Math.sin(A);
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // --- 1. CRANIUM & SKULL DOME ---
        for (int i = 0; i <= 36; i++) {
            double u = (double) i / 36.0;
            double lat = -Math.PI / 2.0 + u * Math.PI;
            double sinLat = Math.sin(lat), cosLat = Math.cos(lat);

            for (int j = 0; j < 72; j++) {
                double v = (double) j / 72.0;
                double lon = v * 2.0 * Math.PI;
                double cosLon = Math.cos(lon), sinLon = Math.sin(lon);

                double rx = 0.52;
                double ry = 0.42;
                double rz = 0.50;

                // Flatten crown top slightly
                if (lat < -0.3) {
                    ry *= 0.88;
                }

                double localX = rx * cosLat * cosLon;
                double localY = -0.28 + ry * sinLat;
                double localZ = 0.05 + rz * cosLat * sinLon;

                // Brow ridge contouring above eyes
                if (localZ < -0.20 && localY < -0.15) {
                    localY -= 0.04 * Math.cos(lon);
                }

                plotProjectedPoint(localX, localY, localZ, cosLat * cosLon, sinLat, cosLat * sinLon,
                        0, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // --- 2. FOREHEAD STOP & UPPER MUZZLE BRIDGE ---
        double muzzleStartZ = -0.45;
        double muzzleLen = snoutLength * 0.68;

        for (double sz = 0.0; sz <= 1.0; sz += 0.025) {
            double z = muzzleStartZ - sz * muzzleLen;
            double taper = 1.0 - 0.38 * sz; // Taper towards nose tip
            double bridgeY = -0.12 + 0.08 * sz;
            double lipY = 0.12 + 0.02 * sz;

            for (double angle = 0; angle < Math.PI * 2; angle += 0.10) {
                double cosAng = Math.cos(angle);
                double sinAng = Math.sin(angle);

                // Boxy cross-section profile
                double lx = cosAng * 0.25 * taper;
                double ly = (sinAng < 0)
                        ? bridgeY + sinAng * 0.12 * taper
                        : lipY + sinAng * 0.08 * taper;

                double rNx = cosAng;
                double rNy = sinAng;
                double rNz = -0.35 * sz;

                plotProjectedPoint(lx, ly, z, rNx, rNy, rNz, 0, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // --- 3. LOWER JAW / CHIN ---
        double lowerJawLen = muzzleLen * 0.82;
        for (double sz = 0.0; sz <= 1.0; sz += 0.03) {
            double z = muzzleStartZ - sz * lowerJawLen;
            double taper = 0.85 - 0.32 * sz;
            double jawY = 0.14 + 0.04 * sz;

            for (double angle = 0; angle <= Math.PI; angle += 0.15) {
                double cosAng = Math.cos(angle - Math.PI / 2.0);
                double sinAng = Math.sin(angle - Math.PI / 2.0);

                double lx = cosAng * 0.20 * taper;
                double ly = jawY + Math.max(0, sinAng) * 0.08 * taper;

                plotProjectedPoint(lx, ly, z, cosAng, 0.8, -0.2, 0, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // --- 4. NOSE LEATHER CAP (Rhinarium) ---
        double noseZ = muzzleStartZ - muzzleLen - 0.01;
        for (double ny = -0.07; ny <= 0.07; ny += 0.012) {
            double widthFactor = (ny < 0) ? (1.0 + ny * 3.0) : (1.0 - ny * 5.0);
            widthFactor = Math.max(0.2, Math.min(1.1, widthFactor));
            double noseWidth = 0.11 * widthFactor;

            for (double nx = -noseWidth; nx <= noseWidth; nx += 0.012) {
                boolean isNostril = (Math.abs(Math.abs(nx) - 0.04) < 0.02) && (ny > 0.01 && ny < 0.05);
                if (isNostril) continue; // Indent nostrils

                double nz = noseZ - Math.sqrt(Math.max(0, 0.008 - nx * nx - ny * ny));
                plotProjectedPoint(nx, -0.04 + ny, nz, nx * 2.0, ny * 2.0, -1.0, 2, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
            }
        }

        // --- 5. CHEEKS & NECK ANCHOR ---
        for (double ny = 0.10; ny <= 0.70; ny += 0.035) {
            double progress = (ny - 0.10) / 0.60;
            double neckRadiusX = 0.42 + 0.18 * progress;
            double neckRadiusZ = 0.45 + 0.20 * progress;
            double neckCenterZ = 0.10 + 0.22 * progress;

            for (double angle = 0; angle < Math.PI * 2; angle += 0.12) {
                double lx = Math.cos(angle) * neckRadiusX;
                double lz = neckCenterZ + Math.sin(angle) * neckRadiusZ;

                if (lz > -0.20 || Math.abs(lx) > 0.22) {
                    plotProjectedPoint(lx, ny, lz, Math.cos(angle), 0.2, Math.sin(angle), 0, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
                }
            }
        }

        // --- 6. EARS ---
        if (breedVariant == 3) {
            renderPointyEars(cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
        } else {
            renderFloppyEars(cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
        }

        // --- 7. EYES & WHISKERS ---
        double localEyeY = -0.20;
        double localEyeZ = -0.58;
        trackAndRenderSqueezedEye(-0.28, localEyeY, localEyeZ, cosA, sinA, outputBuffer, zBuffer);
        trackAndRenderSqueezedEye(0.28, localEyeY, localEyeZ, cosA, sinA, outputBuffer, zBuffer);

        for (double side : new double[] { -1.0, 1.0 }) {
            double rootX = side * 0.18;
            double rootY = -0.02;
            double rootZ = -0.88;

            renderWhiskerLine(rootX, rootY - 0.02, rootZ, side * 0.65, rootY - 0.08, rootZ + 0.10, side, cosA, sinA, outputBuffer, zBuffer);
            renderWhiskerLine(rootX, rootY + 0.02, rootZ, side * 0.65, rootY + 0.08, rootZ + 0.10, side, cosA, sinA, outputBuffer, zBuffer);
        }

        A += 0.015;
    }

    private void renderPointyEars(double cosA, double sinA, double lightX, double lightY, double lightZ,
            String[] outputBuffer, double[] zBuffer) {
        for (double side = -1.0; side <= 1.0; side += 2.0) {
            double baseAnchorX = side * 0.32;
            double baseAnchorY = -0.58;
            double baseAnchorZ = -0.05;

            for (double h = 0.0; h <= 1.0; h += 0.02) {
                double height = h * 0.60;
                double widthScale = (1.0 - h);
                double tipForward = Math.pow(h, 1.8) * 0.10;
                double tiltX = side * h * 0.14;

                for (double w = -1.0; w <= 1.0; w += 0.06) {
                    double earW = w * 0.18 * widthScale;
                    double earDepth = (1.0 - w * w) * 0.08 * widthScale;

                    double lx = baseAnchorX + tiltX + side * Math.abs(earW) * 0.6;
                    double ly = baseAnchorY - height;
                    double lz = baseAnchorZ - tipForward + earDepth;

                    plotProjectedPoint(lx, ly, lz, side * 0.5 + w * 0.4, -0.4, -0.6, 1, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
                }
            }
        }
    }

    private void renderFloppyEars(double cosA, double sinA, double lightX, double lightY, double lightZ,
            String[] outputBuffer, double[] zBuffer) {
        double earLength = (breedVariant == 4) ? 1.05 : (breedVariant == 1 ? 0.95 : 0.85);
        double earBroadness = (breedVariant == 1) ? 0.26 : (breedVariant == 4 ? 0.24 : 0.22);

        for (double side = -1.0; side <= 1.0; side += 2.0) {
            double attachX = side * 0.44;
            double attachY = -0.42;
            double attachZ = -0.05;

            for (double h = 0.0; h <= 1.0; h += 0.02) {
                double curLen = h * earLength;
                double foldOut = Math.sin(h * Math.PI * 0.5) * 0.12 * side;
                double foldForward = Math.sin(h * Math.PI) * 0.08;
                double widthProfile = Math.sin(Math.pow(h, 0.7) * Math.PI) * earBroadness + 0.04 * (1.0 - h);

                for (double w = -1.0; w <= 1.0; w += 0.08) {
                    double earW = w * widthProfile;

                    double lx = attachX + foldOut + side * Math.abs(earW) * 0.4;
                    double ly = attachY + curLen;
                    double lz = attachZ - foldForward + earW * 0.2;

                    plotProjectedPoint(lx, ly, lz, side * 0.7, 0.2, 0.4, 1, cosA, sinA, lightX, lightY, lightZ, outputBuffer, zBuffer);
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

            if (rz > 0.3) continue;

            double ooz = 1.0 / (rz + 3.8);

            int xp = (int) (40 + 38 * ooz * rx * 1.85);
            int yp = (int) (14 + 19 * ooz * ry * 1.25);

            if (yp >= 0 && yp < 22 && xp >= 0 && xp < 80) {
                int bufferIndex = xp + 80 * yp;
                if (ooz > (zBuffer[bufferIndex] - 0.02)) {
                    zBuffer[bufferIndex] = ooz;

                    char whiskerChar = '-';
                    double slope = (y2 - y1) / Math.abs(x2 - x1);
                    if (slope < -0.05) whiskerChar = (side > 0) ? '/' : '\\';
                    else if (slope > 0.05) whiskerChar = (side > 0) ? '\\' : '/';

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

        double ooz = 1.0 / (rz + 3.8);
        double eyeOozBias = ooz + 0.015;

        int cx = (int) (40 + 38 * ooz * rx * 1.85);
        int cy = (int) (14 + 19 * ooz * ry * 1.25);

        double surfaceNormalZ = (lx > 0) ? -cosA : cosA;
        double horizontalSqueeze = Math.max(0.18, Math.abs(surfaceNormalZ));

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

        double distanceToCamera = 3.8;
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
        if (breedVariant == 1) { // Golden Retriever
            if (localZ < -0.55 && localY > -0.10) return tertiaryFurColor; // Pale muzzle
            if ((localX * crownFeatherSide > 0.15) && localY < -0.40 && localZ > -0.10) return secondaryFurColor;
            return baseColor;
        }

        if (breedVariant == 2) { // Dalmatian
            double eyeSpotX = dalmatianEyeSpotSide * 0.28;
            double dex = localX - eyeSpotX, dey = localY - (-0.20), dez = localZ - (-0.55);
            if ((dex * dex * 1.3) + (dey * dey * 1.7) + (dez * dez * 1.1) < 0.05) return secondaryFurColor;

            for (int i = 0; i < spotX.length; i++) {
                double ddx = localX - spotX[i], ddy = localY - spotY[i], ddz = localZ - spotZ[i];
                if (ddx * ddx + ddy * ddy + ddz * ddz < spotR[i] * spotR[i]) return secondaryFurColor;
            }
            return baseColor;
        }

        if (breedVariant == 3) { // Husky
            // White muzzle/cheeks and eyebrow dots
            boolean onMuzzle = localZ < -0.35 && localY > -0.12 && Math.abs(localX) < 0.24;
            boolean eyebrowDots = localY < -0.24 && localY > -0.36 && Math.abs(Math.abs(localX) - 0.20) < 0.07 && localZ < -0.40;
            if (onMuzzle || eyebrowDots) return tertiaryFurColor;

            // Dark widow's peak cap/mask
            double maskEdge = huskyMaskWidth + 0.12 * Math.sin(localY * 4.0);
            if (localY < -0.05 && Math.abs(localX) < maskEdge && localZ < -0.15) return secondaryFurColor;
            return baseColor;
        }

        // American Foxhound
        boolean saddle = localY < 0.10 && localZ > -0.10;
        if (saddle) return secondaryFurColor; // Black saddle

        boolean tanPatch = Math.abs(localX) > 0.22 && localY < 0.25;
        if (tanPatch) return tertiaryFurColor; // Tan cheek/ear transition

        return baseColor;
    }
}