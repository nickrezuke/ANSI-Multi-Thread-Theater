// TODO: This looks terrible lmao make this look more like a butterfly
// TODO: Add Butterfly Variants like monarch and the shiny blue one and etc.

public class ButterflyLoader extends Loader {
    private static final StatusStage[] BUTTERFLY_STAGES = {
        new StatusStage(20, "Segmenting chitin thorax cylinders:"),
        new StatusStage(50, "Mounting uniform dual wing hinges:"),
        new StatusStage(80, "Splicing Monarch cell vein maps:"),
        new StatusStage(100, "Lepidoptera Component Core Active!")
    };

    private static final char[] SHADE_RAMP = { '.', ',', '-', '~', ':', ';', '=', '!', '*', '#', '$', '@' };
    
    private String orangeWingColor;
    private String blackVeinColor;
    private String whiteSpotColor;
    private String bodyColor;
    
    private double rotationAngle = 0.0;

    public ButterflyLoader() {
        super(BUTTERFLY_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        // High-fidelity ANSI 24-bit direct color registers
        orangeWingColor = "\u001B[38;2;245;115;15m"; // Burning Orange
        blackVeinColor  = "\u001B[38;2;25;25;30m";    // Charcoal Black
        whiteSpotColor  = "\u001B[38;2;250;250;255m"; // Pure White Highlight
        bodyColor       = "\u001B[38;2;55;45;40m";    // Dark Chitin Brown
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Spin speed around the vertical axis
        double cosR = Math.cos(rotationAngle);
        double sinR = Math.sin(rotationAngle);

        // Fixed cinematic pitch downward tilt (X-Axis) so top textures are readable
        double pitch = 0.45; 
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);

        // SYNCED WING FLAP MECHANIC: Both wings fold upward at the exact same moment
        // Evaluates a continuous angular transformation angle for the wing matrices
        double wingFlapAngle = 0.85 * Math.sin(rotationAngle * 4.5);
        double cosF = Math.cos(wingFlapAngle);
        double sinF = Math.sin(wingFlapAngle);

        double cameraDistance = 3.2;
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        // -------------------------------------------------------------
        // COMPONENT 1: THE SEGMENTED ANATOMICAL BODY TRUNK
        // -------------------------------------------------------------
        // Loop rendering Head, Thorax, and Abdomen along the central line
        for (double bz = -0.7; bz <= 0.8; bz += 0.02) {
            double bx = 0.0;
            double by = 0.0;
            double segmentRadius = 0.08;

            if (bz < -0.4) {
                // Elongated thin Abdomen
                segmentRadius = 0.05;
            } else if (bz >= -0.4 && bz <= 0.2) {
                // Thick muscular Thorax where wings attach
                segmentRadius = 0.11;
            } else {
                // Round Head sphere chunk
                segmentRadius = 0.09;
            }

            // Draw a tiny circular cross-section for each body slice point
            for (double angle = 0; angle < 2 * Math.PI; angle += 0.4) {
                double cx = bx + segmentRadius * Math.cos(angle);
                double cy = by + segmentRadius * Math.sin(angle);
                double cz = bz;

                // Calculate surface normals for correct lighting calculations
                double nx = Math.cos(angle);
                double ny = Math.sin(angle);
                double nz = 0.0;

                plotProjectedComponent(cx, cy, cz, nx, ny, nz, 0, 0, 0, cosR, sinR, cosP, sinP, cosF, sinF, lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
            }
        }

        // -------------------------------------------------------------
        // COMPONENT 2 & 3: UNISON LEFT & RIGHT WING MATRICES
        // -------------------------------------------------------------
        // u = distance out from body trunk, v = front-to-back distribution span
        for (double u = 0.05; u <= 1.3; u += 0.025) {
            for (double v = -0.9; v <= 0.8; v += 0.025) {

                // Shape boundary profiles creating distinct butterfly wing outlines
                double wingLimit = (v < 0) ? 1.3 - Math.abs(v * 0.8) : 1.1 - Math.abs(v * 1.1);
                if (u > wingLimit) continue;

                // Loop over both distinct side components systematically
                for (int sideMultiplier : new int[] { -1, 1 }) {
                    // Local flat un-deformed coordinate sheet alignment
                    double wx = u * sideMultiplier;
                    double wy = 0.0;
                    double wz = v;

                    // Wing surface normal defaults pointing straight up vertically
                    double wNx = 0.0;
                    double wNy = -1.0;
                    double wNz = 0.0;

                    // Pass execution to projection matrix handler tagging wing textures
                    int surfaceTag = (v > 0.0) ? 1 : 2; // 1 = Forewing pattern, 2 = Hindwing pattern
                    plotProjectedComponent(wx, wy, wz, wNx, wNy, wNz, surfaceTag, sideMultiplier, u, cosR, sinR, cosP, sinP, cosF, sinF, lightX, lightY, lightZ, cameraDistance, outputBuffer, zBuffer);
                }
            }
        }

        rotationAngle += 0.015; // Slow deliberate tumble advance loop
    }

    private void plotProjectedComponent(double lx, double ly, double lz, double lnx, double lny, double lnz, 
                                        int surfaceType, int side, double wingDist,
                                        double cosR, double sinR, double cosP, double sinP, 
                                        double cosF, double sinF, double lightX, double lightY, double lightZ, 
                                        double cameraDistance, String[] outputBuffer, double[] zBuffer) {
        
        double rx = lx, ry = ly, rz = lz;
        double nx = lnx, ny = lny, nz = lnz;

        // --- STEP 1: APPLY RIGID HINGE FLAP TO WING SECTIONS ONLY ---
        if (surfaceType > 0) {
            // Both wings rotate upward on the body axis line based on side orientation
            // For Left Wing (side = -1), coordinate maps flip inverted relative to the axis
            double wingFlapAngle = (side == 1) ? -Math.sin(wingDist * 0.25) * sinF : Math.sin(wingDist * 0.25) * sinF;
            double localCosF = Math.cos(wingFlapAngle);
            double localSinF = Math.sin(wingFlapAngle);

            rx = lx * localCosF - ly * localSinF;
            ry = lx * localSinF + ly * localCosF;
            
            // Transform surface vector normals to track the spatial wing tilt rotation angle
            nx = lnx * localCosF - lny * localSinF;
            ny = lnx * localSinF + lny * localCosF;
        }

        // --- STEP 2: DUAL-AXIS GLOBAL WORLD ROTATION PIPELINE ---
        // Spin coordinates around the vertical Y-axis
        double x1 = rx * cosR + rz * sinR;
        double y1 = ry;
        double z1 = -rx * sinR + rz * cosR;

        double nx1 = nx * cosR + nz * sinR;
        double ny1 = ny;
        double nz1 = -nx * sinR + nz * cosR;

        // Pitch coordinates around horizontal X-axis
        double worldX = x1;
        double worldY = y1 * cosP - z1 * sinP;
        double worldZ = y1 * sinP + z1 * cosP;

        double worldNx = nx1;
        double worldNy = ny1 * cosP - nz1 * sinP;
        double worldNz = ny1 * sinP + nz1 * cosP;

        // --- STEP 3: SCREEN FRUSTUM PROJECTOR MAPPING ---
        double ooz = 1.0 / (worldZ + cameraDistance);
        int xp = (int) (40 + 64 * ooz * worldX * 2.3); // Fixed cell dimension correction factor
        int yp = (int) (11 + 28 * ooz * worldY);

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int bufferIndex = xp + 80 * yp;

            if (ooz > zBuffer[bufferIndex] + 0.0001) {
                zBuffer[bufferIndex] = ooz;

                // Lambertian reflectance illumination shader
                double luminance = worldNx * lightX + worldNy * lightY + worldNz * lightZ;
                int shadeIndex = (int) ((luminance + 1.0) * 5.5);
                shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                char asciiChar = SHADE_RAMP[shadeIndex];

                String chosenColor = bodyColor;

                // --- STEP 4: PROCEDURAL TEXTURING PASSTHROUGH ---
                if (surfaceType > 0) {
                    // Normalize lookup tracks across internal wing space ranges
                    double uNorm = Math.abs(lx) / 1.3;
                    double vNorm = (lz + 0.9) / 1.7;

                    // Generate distinct procedural lines mapping out structural cellular veins
                    double cellVeinPattern = Math.sin(uNorm * 18.0 + vNorm * 5.0) * Math.cos(vNorm * 22.0);

                    if (uNorm > 0.80) {
                        // High-contrast black framing trim running along the margins
                        chosenColor = blackVeinColor;
                        asciiChar = '█';

                        // Insert periodic white dot decoration cells inside outer bounds
                        if (Math.sin(vNorm * 40.0) > 0.3) {
                            chosenColor = whiteSpotColor;
                            asciiChar = '░';
                        }
                    } else if (Math.abs(cellVeinPattern) > 0.62 || uNorm < 0.08) {
                        // Thick deep dark vein borders running through internal membranes
                        chosenColor = blackVeinColor;
                        asciiChar = '▓';
                    } else {
                        // Main interior wing cell layout
                        chosenColor = orangeWingColor;
                    }
                }

                outputBuffer[bufferIndex] = chosenColor + asciiChar + RESET;
            }
        }
    }
}
