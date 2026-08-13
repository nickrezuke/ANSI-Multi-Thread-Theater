// TODO: Improve the Moon Surface accuracy to real life crater map

import java.util.Random;

public class MoonLoader extends Loader {
    private static final StatusStage[] MOON_STAGES = {
        new StatusStage(30, "Calibrating orbital axis tilt:"),
        new StatusStage(65, "Sharpening vacuum terminator line:"),
        new StatusStage(90, "Simulating tidal locking constraints:"),
        new StatusStage(100, "Lunar Phase Matrix Operational!")
    };

    // 12-Step Micro-Granular Shading Scale
    private static final char[] SHADE_RAMP = {
        '\u00B7', // 0: · (Faint Shadow Rim / Surface Dust) 
        '\u2058', // 1: ⁘ (Four-Dot Scatter Punctuation) 
        '\u2022', // 2: • (Defined Outer Bullet Filament) 
        '\u00A4', // 3: ¤ (Particle Node) 
        '\u205C', // 4: ⁜ (Dotted Cross - Airy Technical Grain) 
        ':',      // 5: : (Standard Density Anchor) 
        '=',      // 6: = (Mid-Weight Structure) 
        '\u2591', // 7: ░ (Light Vapor Block) 
        '\u2592', // 8: ▒ (Fluid Mid-Tone Block) 
        '\u2593', // 9: ▓ (Dense Crater Layer Block) 
        '\u2588', // 10: █ (Blazing Peak Core Block) 
        '\u2588'  // 11: █ (Overdrive Core Guard) 
    };

    private static final int MAX_STARS = 45;
    private final int[] starPositions = new int[MAX_STARS];
    private final double[] starPhases = new double[MAX_STARS]; 

    // Time variable driving the synodic solar month phase evolution
    private double solarOrbitAngle = 0.0;

    // Standard baseline Lunar gray profile
    private static final int MOON_R = 195;
    private static final int MOON_G = 200;
    private static final int MOON_B = 205;

    public MoonLoader() {
        // This uses 80x22 specifically
        super(MOON_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.solarOrbitAngle = 0.0;
        
        // Procedurally generate a fixed random star field that skips text margins
        Random rand = new Random(4321); 
        for (int i = 0; i < MAX_STARS; i++) {
            int rx = rand.nextInt(80);
            int ry = 1 + rand.nextInt(20); 
            starPositions[i] = ry * 80 + rx;
            starPhases[i] = rand.nextDouble() * Math.PI * 2.0; 
        }

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Step 1: Draw the Twinkling Background Starfield
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_STARS; i++) {
            int starIdx = starPositions[i];
            double twinkleFactor = Math.sin((currentTime * 0.004) + starPhases[i]);
            
            char starChar = ' ';
            if (twinkleFactor > 0.82)      starChar = '*';
            else if (twinkleFactor > 0.20) starChar = '.';
            else if (twinkleFactor > -0.3) starChar = '·';

            if (starChar != ' ' && starIdx >= 0 && starIdx < 1760) {
                zBuffer[starIdx] = 0.0001; 
                outputBuffer[starIdx] = "\u001B[37m" + starChar + RESET;
            }
        }

        // Step 2: COMPUTE TRUE SYNODIC LUNAR PHASE SUN MATRIX
        solarOrbitAngle -= 0.025; 

        double unshiftedX = Math.sin(solarOrbitAngle);
        double unshiftedY = -Math.cos(solarOrbitAngle); 
        double unshiftedZ = 0.0;

        double tiltAngle = Math.toRadians(23.5);
        double cosTilt = Math.cos(tiltAngle);
        double sinTilt = Math.sin(tiltAngle);

        double sunX = unshiftedX * cosTilt - unshiftedZ * sinTilt;
        double sunY = unshiftedY; 
        double sunZ = unshiftedX * sinTilt + unshiftedZ * cosTilt;

        double cameraDistance = 3.45;
        double sphereRadius = 1.0;

        // Step 3: Render the Texturized 3D Sphere Geometry
        for (double theta = 0; theta < Math.PI; theta += 0.02) {
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (double phi = 0; phi < 2 * Math.PI; phi += 0.02) {
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double rx = sphereRadius * sinTheta * cosPhi;
                double ry = sphereRadius * sinTheta * sinPhi;
                double rz = sphereRadius * cosTheta;

                double ooz = 1.0 / (ry + cameraDistance); 
                
                int xp = (int) (40 + 74 * ooz * rx);
                int yp = (int) (11 - 36 * ooz * rz); 

                if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22 && ry < 0) {
                    int index = xp + 80 * yp;
                    
                    if (ooz > zBuffer[index] + 0.0001) {
                        zBuffer[index] = ooz;

                        double nx = rx / sphereRadius;
                        double ny = ry / sphereRadius;
                        double nz = rz / sphereRadius;

                        // -------------------------------------------------------------
                        // GEOGRAPHY ENGINE: BLURRED SMOOTHSTEP COALESCENCE
                        // -------------------------------------------------------------
                        double albedo = 1.05; // Base high-reflective highland profile

                        // Seed a fast, high-frequency inline noise function to act as our digital blur filter
                        double dBlur = 0.03 * Math.sin(nx * 45.0) * Math.cos(nz * 45.0);

                        // A. SMOOTHED LUNAR MARIA PATCHES
                        // 1. Mare Imbrium
                        double distImbrium = Math.sqrt(Math.pow(nx + 0.35, 2) + Math.pow(nz - 0.35, 2)) + dBlur;
                        if (distImbrium < 0.36) {
                            // Smoothstep factor: Tapers from 1.0 at center to 0.0 at edge boundary
                            double weight = (0.36 - distImbrium) / 0.36;
                            albedo -= 0.40 * (weight * weight * (3.0 - 2.0 * weight));
                        }

                        // 2. Oceanus Procellarum
                        if (nx < -0.05 && nz < 0.25 && nz > -0.65) {
                            // Calculate a distance gradient away from the Procellarum core center
                            double distPro = Math.sqrt(Math.pow(nx + 0.5, 2) + Math.pow(nz + 0.2, 2)) + dBlur;
                            if (distPro < 0.55) {
                                double weight = (0.55 - distPro) / 0.55;
                                albedo -= 0.36 * (weight * weight * (3.0 - 2.0 * weight));
                            }
                        }

                        // 3. Mare Serenitatis & Tranquillitatis
                        double distSeren = Math.sqrt(Math.pow(nx - 0.35, 2) + Math.pow(nz - 0.30, 2)) + dBlur;
                        double distTranq = Math.sqrt(Math.pow(nx - 0.45, 2) + Math.pow(nz - 0.02, 2)) + dBlur;
                        if (distSeren < 0.24) {
                            double w = (0.24 - distSeren) / 0.24;
                            albedo -= 0.38 * (w * w * (3.0 - 2.0 * w));
                        }
                        if (distTranq < 0.26) {
                            double w = (0.26 - distTranq) / 0.26;
                            albedo -= 0.38 * (w * w * (3.0 - 2.0 * w));
                        }

                        // 4. Mare Crisium
                        double distCrisium = Math.sqrt(Math.pow(nx - 0.75, 2) + Math.pow(nz - 0.20, 2)) + dBlur;
                        if (distCrisium < 0.16) {
                            double w = (0.16 - distCrisium) / 0.16;
                            albedo -= 0.42 * (w * w * (3.0 - 2.0 * w));
                        }

                        // 5. Mare Fecunditatis
                        double distFecund = Math.sqrt(Math.pow(nx - 0.60, 2) + Math.pow(nz + 0.25, 2)) + dBlur;
                        if (distFecund < 0.22) {
                            double w = (0.22 - distFecund) / 0.22;
                            albedo -= 0.35 * (w * w * (3.0 - 2.0 * w));
                        }


                        // B. SMOOTHED MAJOR IMPACT CRATERS
                        // 1. Tycho Crater (Southern Highlands)
                        double distTycho = Math.sqrt(Math.pow(nx - 0.15, 2) + Math.pow(nz + 0.60, 2)) + (dBlur * 0.4);
                        if (distTycho < 0.11) {
                            // Smoothly transition from bright rim, down into a dark central bowl shadow, up to a bright peak core
                            double w = distTycho / 0.11; // 0 at center, 1 at edge
                            double craterProfile = Math.sin(w * Math.PI * 1.5); // Oscillating signature curve
                            albedo += 0.32 * (craterProfile * (1.0 - w)); 
                        }
                        // Tycho's Rays: Softly layered alpha line blends
                        if (distTycho > 0.06 && distTycho < 0.85) {
                            double rayAngle = Math.atan2(nz + 0.60, nx - 0.15);
                            double deltaRay1 = Math.abs(Math.sin(rayAngle * 8.0));
                            double deltaRay2 = Math.abs(Math.sin(rayAngle * 5.0 + 1.2));
                            double minRayDist = Math.min(deltaRay1, deltaRay2);
                            
                            if (minRayDist < 0.08) {
                                // Fade the rays smoothly sideways out of their center-line axes and down their running lengths
                                double radialFade = 1.0 - distTycho / 0.85;
                                double lateralFade = (0.08 - minRayDist) / 0.08;
                                albedo += 0.20 * (radialFade * lateralFade * lateralFade);
                            }
                        }

                        // 2. Copernicus Crater
                        double distCopernicus = Math.sqrt(Math.pow(nx + 0.20, 2) + Math.pow(nz - 0.15, 2)) + (dBlur * 0.4);
                        if (distCopernicus < 0.09) {
                            double w = distCopernicus / 0.09;
                            double craterProfile = Math.sin(w * Math.PI * 1.5);
                            albedo += 0.24 * (craterProfile * (1.0 - w));
                        }

                        // Safety clamp to anchor final color balance margins
                        albedo = Math.max(0.22, Math.min(1.45, albedo));

                        // -------------------------------------------------------------
                        // SHADING & LIGHT EXECUTION
                        // -------------------------------------------------------------
                        double sunDiffuse = nx * sunX + ny * sunY + nz * sunZ;

                        double baseLight = Math.max(0.0, sunDiffuse);
                        double highContrastLight = Math.pow(baseLight, 0.2);

                        double finalLuminance = 0.04 + 0.96 * (highContrastLight * albedo);

                        int shadeIndex = (int) (finalLuminance * (SHADE_RAMP.length - 1));
                        shadeIndex = Math.max(0, Math.min(SHADE_RAMP.length - 1, shadeIndex));
                        char renderChar = SHADE_RAMP[shadeIndex];

                        int outR = (int) (MOON_R * finalLuminance * (albedo * 0.9));
                        int outG = (int) (MOON_G * finalLuminance * (albedo * 0.9));
                        int outB = (int) (MOON_B * finalLuminance * (albedo * 0.95));

                        outR = Math.max(0, Math.min(255, outR));
                        outG = Math.max(0, Math.min(255, outG));
                        outB = Math.max(0, Math.min(255, outB));

                        String colorCode = String.format("\u001B[38;2;%d;%d;%dm", outR, outG, outB);
                        outputBuffer[index] = colorCode + renderChar + RESET;
                    }
                }
            }
        }
    }
}
