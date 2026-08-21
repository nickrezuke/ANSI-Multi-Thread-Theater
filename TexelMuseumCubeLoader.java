// TODO: Fix / Improve the Girl with the Pearl Earring 
// TODO: Fix / Improve the Scream (Details Unclear)
// TODO: Fix / Improve the Mona Lisa (Zoom in?  Details unclear)

public class TexelMuseumCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] ART_STAGES = new StatusStage[] {
            new StatusStage(15, "Erecting Gallery Walls:"),
            new StatusStage(35, "Stretching Canvas Linens:"),
            new StatusStage(55, "Mixing Raw Oil Pigments:"),
            new StatusStage(75, "Applying Gold Leaf Frames:"),
            new StatusStage(90, "Adjusting Track Lighting:"),
            new StatusStage(100, "Exhibition Open!")
    };

    private static final int TEXTURE_RESOLUTION = 512;

    private static final double OUTER_FRAME_FRAC = 12.0 / 128.0;
    private static final double GOLD_TRIM_FRAC = 16.0 / 128.0;

    private static final double EDGE_SOFTNESS_PX = 1.6;

    public TexelMuseumCubeLoader() {
        super(ART_STAGES, 80, 22);
    }

    @Override
    protected int getTextureResolution() {
        return TEXTURE_RESOLUTION;
    }

    @Override
    protected void initialize() {
        // No random colors needed; textures are assigned deterministically per face
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        final int size = TEXTURE_RESOLUTION;
        final double outerFramePx = OUTER_FRAME_FRAC * size;
        final double goldTrimPx = GOLD_TRIM_FRAC * size;
        final double aa = EDGE_SOFTNESS_PX * (size / 256.0);

        final int edgeDist = Math.min(Math.min(x, size - 1 - x), Math.min(y, size - 1 - y));

        // --- 1. MUSEUM PICTURE FRAME RENDERER (anti-aliased transitions) ---
        if (edgeDist < outerFramePx - aa) {
            return mahoganyFrame(edgeDist).toVoxel();
        }
        if (edgeDist < outerFramePx + aa) {
            double t = smoothstep(outerFramePx - aa, outerFramePx + aa, edgeDist);
            return blend(mahoganyFrame(edgeDist), goldLeafTrim(x, y), t).toVoxel();
        }
        if (edgeDist < goldTrimPx - aa) {
            return goldLeafTrim(x, y).toVoxel();
        }

        // Normalize inner canvas coordinates to 0.0 -> 1.0
        double cx = (x - goldTrimPx) / (size - 2 * goldTrimPx);
        double cy = (y - goldTrimPx) / (size - 2 * goldTrimPx);
        Texel canvas = renderFace(face, cx, cy);

        if (edgeDist < goldTrimPx + aa) {
            double t = smoothstep(goldTrimPx - aa, goldTrimPx + aa, edgeDist);
            return blend(goldLeafTrim(x, y), canvas, t).toVoxel();
        }
        return canvas.toVoxel();
    }

    // --- 2. THE 6 MASTERPIECES (One distinct painting mapped per cube face) ---
    private Texel renderFace(int face, double cx, double cy) {
        switch (face) {
            case 0: // FRONT: Mona Lisa (Leonardo da Vinci)
                return renderMonaLisa(cx, cy);
            case 1: // BACK: The Starry Night (Vincent van Gogh)
                return renderStarryNight(cx, cy);
            case 2: // TOP: The Scream (Edvard Munch)
                return renderTheScream(cx, cy);
            case 3: // BOTTOM: Girl with a Pearl Earring (Johannes Vermeer)
                return renderGirlPearlEarring(cx, cy);
            case 4: // LEFT: The Great Wave off Kanagawa (Hokusai)
                return renderGreatWave(cx, cy);
            case 5: // RIGHT: Composition with Red, Blue and Yellow (Piet Mondrian)
            default:
                return renderMondrian(cx, cy);
        }
    }

    // ------------
    // Frame layers
    // ------------

    private Texel mahoganyFrame(double edgeDist) {
        // Ornate carved mahogany/walnut outer frame with a soft, continuous carved-groove
        // shimmer (the old version stair-stepped every 3rd pixel, which aliased badly).
        double groove = Math.sin(edgeDist * 1.9) * 0.5 + 0.5;
        int shadow = (int) Math.round(-20 * groove);
        return new Texel(70 + shadow, 45 + shadow, 25 + shadow, '▓');
    }

    private Texel goldLeafTrim(int x, int y) {
        // Antique gold leaf inner trim with a soft diagonal specular sheen.
        double shine = Math.sin((x + y) * 0.10) * 0.5 + 0.5;
        int glow = (int) Math.round(30 * shine);
        return new Texel(215 + glow, 175 + glow, 55 - glow, '█');
    }

    // ---------
    // Mona Lisa
    // ---------

    private Texel renderMonaLisa(double x, double y) {
        // ---- Background: hazy Renaissance landscape ----
        Texel farColor = new Texel(64, 76, 92, '░');   // distant blue-grey haze
        Texel nearColor = new Texel(38, 56, 40, '░');  // deeper green foreground
        double depthT = smoothstep(0.0, 0.5, y);
        Texel result = blend(farColor, nearColor, depthT);

        // Faint distant mountain ridge (kept low-contrast so it reads as aerial-perspective haze)
        double ridge = 0.16 + 0.045 * Math.sin(x * 7.3) + 0.02 * Math.sin(x * 17.0 + 1.3);
        double ridgeMask = smoothstep(ridge + 0.02, ridge - 0.02, y) * smoothstep(0.02, 0.10, y);
        if (ridgeMask > 0.001) {
            result = blend(result, new Texel(80, 92, 104, '░'), ridgeMask * 0.55);
        }

        // Winding path glinting through the landscape
        double path = 0.30 + 0.09 * Math.sin(x * 3.1 + 0.4) + 0.04 * Math.sin(x * 6.7);
        double pathMask = smoothstep(0.024, 0.0, Math.abs(y - path)) * smoothstep(0.44, 0.20, y);
        if (pathMask > 0.001) {
            result = blend(result, new Texel(128, 138, 116, '░'), pathMask * 0.25);
        }

        // Soft horizon haze band
        double horizon = 0.44 + 0.015 * Math.sin(x * 5.0);
        double haze = smoothstep(horizon + 0.09, horizon - 0.05, y) * smoothstep(horizon - 0.14, horizon, y);
        if (haze > 0.001) {
            result = blend(result, new Texel(95, 106, 100, '░'), haze * 0.5);
        }

        // ---- Torso: dress with rounded shoulders, folded sleeves, and hands ----
        double torsoTop = 0.53;
        double shoulderDrop = 0.05 * smoothstep(0.07, 0.20, Math.abs(x - 0.5));
        double topEdge = torsoTop + shoulderDrop;
        double halfWidth = lerpD(0.075, 0.36, smoothstep(torsoTop, 0.97, y));
        double bodyMask = smoothstep(topEdge - 0.025, topEdge + 0.025, y)
                * smoothstep(halfWidth + 0.02, halfWidth - 0.02, Math.abs(x - 0.5));

        double handCx = 0.5, handCy = 0.855;

        if (bodyMask > 0.001) {
            double foldShimmer = Math.sin((x - 0.5) * 10.0 + y * 2.0) * 0.5 + 0.5;
            Texel dressBase = new Texel(24, 22, 19, '▒');
            Texel dressFold = new Texel(40, 36, 30, '▒');
            Texel dress = blend(dressBase, dressFold, smoothstep(0.4, 0.9, foldShimmer) * 0.5);
            result = blend(result, dress, bodyMask);

            // Crossed-arm / folded-sleeve shading leading down to the hands
            double armL = distToSegment(x, y, 0.34, topEdge + 0.02, handCx - 0.05, handCy - 0.06);
            double armR = distToSegment(x, y, 0.66, topEdge + 0.02, handCx + 0.05, handCy - 0.06);
            double armMask = smoothstep(0.055, 0.03, Math.min(armL, armR)) * bodyMask;
            if (armMask > 0.001) {
                result = blend(result, new Texel(50, 43, 36, '▒'), armMask * 0.6);
            }

            // Neckline shadow
            double necklineDist = Math.hypot((x - 0.5) * 2.2, (y - (topEdge + 0.01)) * 7.0);
            double necklineMask = smoothstep(0.05, 0.0, necklineDist) * bodyMask;
            if (necklineMask > 0.001) {
                result = blend(result, new Texel(12, 11, 10, '▓'), necklineMask * 0.65);
            }

            // Folded hands
            double hx = x - handCx, hy = y - handCy;
            double hands = (hy * hy * 2.6) + (hx * hx * 1.15);
            double handsMask = smoothstep(0.026, 0.010, hands) * bodyMask;
            if (handsMask > 0.001) {
                Texel skinLight = new Texel(205, 175, 138, '█');
                Texel skinShadow = new Texel(152, 122, 94, '▒');
                double handShade = smoothstep(-0.010, 0.018, hy);
                Texel handTexel = blend(skinLight, skinShadow, handShade);

                // Soft curved seam where the two hands overlap
                double seamCurve = 0.09 * hx * hx - 0.004;
                double seamMask = smoothstep(0.016, 0.0, Math.abs(hy - seamCurve)) * 0.32;
                handTexel = blend(handTexel, new Texel(122, 96, 75, '▒'), seamMask);

                // A single faint finger-crease hint below the seam
                double creaseMask = smoothstep(0.010, 0.0, Math.abs(hy - (seamCurve + 0.018))) * 0.16;
                handTexel = blend(handTexel, new Texel(130, 103, 80, '▒'), creaseMask);

                result = blend(result, handTexel, handsMask);
            }
        }

        // ---- Neck ----
        double neckTop = 0.478, neckBottom = topEdge + 0.010, neckHalf = 0.045;
        double neckMask = smoothstep(neckHalf + 0.012, neckHalf - 0.012, Math.abs(x - 0.5))
                * smoothstep(neckTop - 0.015, neckTop + 0.012, y)
                * smoothstep(neckBottom + 0.02, neckBottom - 0.01, y);
        if (neckMask > 0.001) {
            Texel neckSkin = new Texel(190, 158, 122, '█');
            Texel neckShadow = new Texel(140, 112, 86, '▒');
            double neckShade = smoothstep(0.02, 0.045, Math.abs(x - 0.5));
            result = blend(result, blend(neckSkin, neckShadow, neckShade), neckMask);
        }

        // ---- Hair / veil, draped over the shoulders ----
        double fx = x - 0.5;
        double fy = y - 0.37;
        double wave = 0.004 * Math.sin(fy * 40.0 + fx * 10.0);
        double hairShape = (fy * fy * 1.55) + (fx * fx * 0.92) + wave;
        double hairMask = smoothstep(0.034, 0.026, hairShape);
        if (hairMask > 0.001) {
            double hairShimmer = Math.sin(fy * 26.0 + fx * 8.0) * 0.5 + 0.5;
            Texel hairDark = new Texel(24, 20, 16, '▓');
            Texel hairLit = new Texel(46, 38, 30, '▓');
            Texel hair = blend(hairDark, hairLit, hairShimmer * 0.4);
            // Center part
            double partMask = smoothstep(0.008, 0.0, Math.abs(fx)) * smoothstep(-0.06, -0.11, fy);
            if (partMask > 0.001) {
                hair = blend(hair, new Texel(58, 46, 36, '▓'), partMask * 0.6);
            }
            result = blend(result, hair, hairMask);
        }

        // ---- Face ----
        double faceEllipse = (fy * fy * 1.7) + (fx * fx);
        double faceMask = smoothstep(0.027, 0.019, faceEllipse);
        if (faceMask > 0.001) {
            double shade = smoothstep(0.0, 0.024, faceEllipse);
            Texel skinBright = new Texel(214, 184, 147, '█');
            Texel skinDark = new Texel(168, 138, 105, '▒');
            Texel skin = blend(skinBright, skinDark, shade * 0.65);

            // Cheek blush
            double cheekL = Math.hypot((fx + 0.055) * 1.4, (fy - 0.035) * 1.8);
            double cheekR = Math.hypot((fx - 0.055) * 1.4, (fy - 0.035) * 1.8);
            double blushMask = Math.max(smoothstep(0.045, 0.0, cheekL), smoothstep(0.045, 0.0, cheekR)) * 0.22;
            if (blushMask > 0.001) {
                skin = blend(skin, new Texel(205, 140, 120, '▒'), blushMask);
            }

            // Nose: soft shadow down one side of the bridge, subtle highlight on the other
            double noseShadow = smoothstep(0.012, 0.004, Math.abs(fx - 0.008))
                    * smoothstep(-0.015, 0.0, fy) * smoothstep(0.06, 0.045, fy);
            if (noseShadow > 0.001) {
                skin = blend(skin, new Texel(148, 118, 90, '▒'), noseShadow * 0.35);
            }
            double noseHighlight = smoothstep(0.008, 0.0, Math.abs(fx + 0.006))
                    * smoothstep(-0.010, 0.010, fy) * smoothstep(0.050, 0.035, fy);
            if (noseHighlight > 0.001) {
                skin = blend(skin, new Texel(226, 199, 166, '█'), noseHighlight * 0.3);
            }

            // Faint brows (Mona Lisa is famous for having almost none — kept subtle)
            double browDist = Math.min(
                    distToSegment(fx, fy, -0.050, -0.045, -0.012, -0.058),
                    distToSegment(fx, fy, 0.012, -0.058, 0.050, -0.045));
            double browMask = smoothstep(0.004, 0.0, browDist);
            if (browMask > 0.001) {
                skin = blend(skin, new Texel(115, 90, 68, '▒'), browMask * 0.22);
            }

            // Almond eyes with iris, pupil and lid shadow
            double eyeY = -0.022;
            double leftEyeShape = sq((fx + 0.032) / 0.020) + sq((fy - eyeY) / 0.009);
            double rightEyeShape = sq((fx - 0.032) / 0.020) + sq((fy - eyeY) / 0.009);
            double eyeShape = Math.min(leftEyeShape, rightEyeShape);
            double eyeWhiteMask = smoothstep(1.15, 0.85, eyeShape);
            if (eyeWhiteMask > 0.001) {
                skin = blend(skin, new Texel(222, 210, 190, '█'), eyeWhiteMask);
            }
            double irisDistL = Math.hypot(fx + 0.032, (fy - eyeY) * 1.4);
            double irisDistR = Math.hypot(fx - 0.032, (fy - eyeY) * 1.4);
            double irisDist = Math.min(irisDistL, irisDistR);
            double irisMask = smoothstep(0.009, 0.005, irisDist);
            if (irisMask > 0.001) {
                skin = blend(skin, new Texel(64, 48, 36, '▓'), irisMask);
            }
            double pupilMask = smoothstep(0.004, 0.002, irisDist);
            if (pupilMask > 0.001) {
                skin = blend(skin, new Texel(20, 15, 12, '█'), pupilMask);
            }
            double lidDist = Math.min(
                    distToSegment(fx, fy, -0.050, eyeY - 0.008, -0.014, eyeY - 0.010),
                    distToSegment(fx, fy, 0.014, eyeY - 0.010, 0.050, eyeY - 0.008));
            double lidMask = smoothstep(0.005, 0.0, lidDist);
            if (lidMask > 0.001) {
                skin = blend(skin, new Texel(142, 112, 86, '▒'), lidMask * 0.5);
            }

            // The enigmatic smile: closed lips, corners curling gently upward
            double mouthY = 0.082;
            double mouthLine = mouthY - 1.6 * fx * fx;
            double mouthWidth = smoothstep(0.06, 0.0, Math.abs(fx));
            double mouthMask = smoothstep(0.009, 0.003, Math.abs(fy - mouthLine)) * mouthWidth;
            if (mouthMask > 0.001) {
                skin = blend(skin, new Texel(150, 95, 85, '▒'), mouthMask * 0.6);
            }
            double dimpleL = Math.hypot(fx + 0.05, (fy - mouthLine) * 2.0);
            double dimpleR = Math.hypot(fx - 0.05, (fy - mouthLine) * 2.0);
            double dimpleMask = smoothstep(0.012, 0.0, Math.min(dimpleL, dimpleR)) * 0.3;
            if (dimpleMask > 0.001) {
                skin = blend(skin, new Texel(130, 85, 72, '▒'), dimpleMask);
            }

            // Soft chin shadow for volume
            double chinShadow = smoothstep(0.085, 0.105, fy) * smoothstep(0.03, 0.0, Math.abs(fx) * 0.6);
            if (chinShadow > 0.001) {
                skin = blend(skin, new Texel(150, 120, 95, '▒'), chinShadow * 0.35);
            }
            result = blend(result, skin, faceMask);
        }
        return result;
    }

    // ----------------
    // The Starry Night
    // ----------------

    private Texel renderStarryNight(double x, double y) {
        // Rich deep indigo and cobalt night sky base
        Texel result = new Texel(12, 22, 65, '▒');

        // 1. Dynamic Van Gogh Impasto Swirls (layered cyan-blue and luminous yellow-white)
        double swirlA = swirl(x, y, 0.35, 0.26, 6.0);
        double swirlB = swirl(x, y, 0.68, 0.22, 5.0);
        double combinedSwirl = Math.max(swirlA, swirlB) * smoothstep(0.70, 0.45, y);
        if (combinedSwirl > 0.001) {
            Texel skyTurbulence = new Texel(65, 125, 195, '░');
            Texel yellowStroke = new Texel(220, 200, 90, '▒');
            double strokePattern = Math.sin(x * 45.0 + y * 35.0) * 0.5 + 0.5;
            Texel vortexTexel = blend(skyTurbulence, yellowStroke, strokePattern * combinedSwirl);
            result = blend(result, vortexTexel, clamp01(combinedSwirl * 1.2));
        }

        // 2. Radiant Crescent Moon & Soft Corona Glow (Top-Right)
        double moonDist = Math.hypot(x - 0.83, y - 0.18);
        double moonGlow = smoothstep(0.16, 0.0, moonDist);
        if (moonGlow > 0.001) {
            result = blend(result, new Texel(240, 215, 110, '░'), moonGlow * 0.6);
        }
        double moonCore = smoothstep(0.07, 0.04, moonDist);
        if (moonCore > 0.001) {
            result = blend(result, new Texel(255, 245, 180, '█'), moonCore);
        }

        // 3. Glowing Stars with Multi-Layered Halos (Upper Sky)
        if (y < 0.58) {
            double starField = Math.sin(x * 28.0) * Math.sin(y * 24.0 + x * 4.0);
            double starMask = smoothstep(0.80, 0.96, starField);
            double haloMask = smoothstep(0.50, 0.80, starField) * 0.5;

            if (haloMask > 0.001) {
                result = blend(result, new Texel(210, 195, 125, '░'), haloMask);
            }
            if (starMask > 0.001) {
                result = blend(result, new Texel(255, 240, 140, '█'), starMask);
            }
        }

        // 4. Rolling Blue-Green Hills & Alpilles Mountains + Quiet Village
        double hillLine = 0.70 + 0.02 * Math.sin(x * 12.0) + 0.01 * Math.cos(x * 25.0);
        double hills = smoothstep(hillLine, hillLine + 0.04, y);
        if (hills > 0.001) {
            Texel hillBase = new Texel(20, 45, 55, '▓');
            Texel hillHighlight = new Texel(35, 70, 75, '▒');
            double hillShade = Math.sin(x * 30.0) * 0.5 + 0.5;
            Texel rollingHill = blend(hillBase, hillHighlight, hillShade * 0.4);
            result = blend(result, rollingHill, hills);

            // Village rooftops with warm glowing windows
            double village = Math.sin(x * 45.0) * Math.sin((y - hillLine) * 55.0);
            double roofMask = smoothstep(0.55, 0.75, village) * hills * smoothstep(hillLine + 0.18, hillLine + 0.02, y);
            if (roofMask > 0.001) {
                result = blend(result, new Texel(12, 15, 22, '▓'), roofMask);
            }
            boolean windowRow = ((int) (x * 90) % 6 == 0) && y > hillLine + 0.015 && y < hillLine + 0.045;
            if (windowRow) {
                result = blend(result, new Texel(250, 200, 80, '█'), 0.9);
            }

            // Prominent church spire
            double spireDx = x - 0.48;
            if (Math.abs(spireDx) < 0.010 && y > hillLine - 0.12 && y < hillLine + 0.02) {
                result = blend(result, new Texel(10, 12, 18, '█'), 0.95);
            }
        }

        // 5. Foreground Cypress Flame Silhouette (Textured with vertical impasto ridges)
        double cypressAxis = 0.20 + 0.035 * Math.sin(y * 12.0);
        double cypressWidth = 0.055 * (1.0 - y * 0.25);
        double cypressDist = (Math.abs(x - cypressAxis) - cypressWidth) * 7.5;
        double cypressMask = smoothstep(0.5, -0.5, cypressDist) * smoothstep(0.02, 0.18, 1.0 - y);
        cypressMask *= smoothstep(0.0, 0.12, y);

        if (cypressMask > 0.001) {
            Texel cypressCore = new Texel(8, 18, 14, '█');
            Texel cypressRidge = new Texel(18, 35, 26, '▓');
            double grain = Math.sin(y * 50.0 + x * 20.0) * 0.5 + 0.5;
            Texel cypressTexel = blend(cypressCore, cypressRidge, grain);
            result = blend(result, cypressTexel, cypressMask);
        }

        return result;
    }

    

    // ----------
    // The Scream
    // ----------

    private Texel renderTheScream(double x, double y) {
        Texel result;

        // 1. Turbulent Expressionist Sky (Top Region)
        if (y < 0.42) {
            double wave1 = Math.sin(x * 8.0 + y * 5.0);
            double wave2 = Math.sin(x * 14.0 - y * 8.0) * 0.5;
            double wave = wave1 + wave2;

            int r = clampByte((int) (240 - y * 80 + wave * 15));
            int g = clampByte((int) (80 + y * 180 + wave * 25));
            int b = clampByte((int) (30 + wave * 10));

            char ch = (wave > 0.3) ? '█' : ((wave > -0.3) ? '▒' : '░');
            result = new Texel(r, g, b, ch);
        } 
        // 2. Swirling Dark Fjord & Distant Hills (Middle Region)
        else if (y < 0.64) {
            double shoreCurve = 0.50 + 0.05 * Math.sin(x * 5.0);
            double waterMask = smoothstep(shoreCurve - 0.02, shoreCurve + 0.02, y);

            Texel darkHill = new Texel(45, 40, 55, '▓');
            Texel swirlingWater = new Texel(20, 42, 85, '▒');

            // Sky reflections glinting off the water
            double reflection = Math.sin(x * 12.0 + y * 20.0) * 0.5 + 0.5;
            if (reflection > 0.65 && waterMask > 0.5) {
                swirlingWater = blend(swirlingWater, new Texel(160, 90, 45, '░'), (reflection - 0.65) * 2.0);
            }

            result = blend(darkHill, swirlingWater, waterMask);
        } 
        // 3. Diagonal Wooden Pier & Background Passersby (Bottom Region)
        else {
            int r = clampByte((int) (125 + x * 35 - (y - 0.64) * 50));
            int g = clampByte((int) (80 + x * 20));
            int b = clampByte((int) (60 + x * 15));
            Texel bridgePlank = new Texel(r, g, b, '▒');

            // Sharp diagonal railing line
            double railLine = (y - 0.64) - (x - 0.55) * 0.6;
            double railMask = smoothstep(0.018, 0.0, Math.abs(railLine));
            if (railMask > 0.001) {
                bridgePlank = blend(bridgePlank, new Texel(35, 28, 25, '█'), railMask);
            }

            // Silhouetted figures walking away in the background
            double fig1Dist = Math.hypot((x - 0.82) * 3.5, (y - 0.67) * 7.0);
            double fig2Dist = Math.hypot((x - 0.90) * 3.5, (y - 0.69) * 7.0);
            double figuresMask = Math.max(smoothstep(0.045, 0.01, fig1Dist), smoothstep(0.04, 0.01, fig2Dist));
            if (figuresMask > 0.001) {
                bridgePlank = blend(bridgePlank, new Texel(15, 14, 20, '█'), figuresMask * 0.9);
            }

            result = bridgePlank;
        }

        // 4. Distorted Screaming Figure (Flowing Robe, Skull Head, & Clasped Hands)
        double fx = x - 0.48;
        double fy = y - 0.70;

        // Dark flowing robe connecting body to bridge
        double robeWidth = 0.06 + (y - 0.60) * 0.35;
        double robeSway = 0.02 * Math.sin(y * 18.0);
        double robeDist = Math.abs(fx - robeSway) - robeWidth;
        double robeMask = smoothstep(0.02, -0.02, robeDist) * smoothstep(0.52, 0.58, y);
        if (robeMask > 0.001) {
            Texel robeBase = new Texel(22, 26, 42, '█');
            Texel robeHighlight = new Texel(40, 48, 70, '▒');
            double fold = Math.sin((x + y) * 30.0) * 0.5 + 0.5;
            result = blend(result, blend(robeBase, robeHighlight, fold * 0.4), robeMask);
        }

        // Elongated skull face
        double headY = fy + 0.12;
        double headEllipse = (headY * headY * 2.2) + (fx * fx * 0.85);
        double headMask = smoothstep(0.026, 0.018, headEllipse);
        if (headMask > 0.001) {
            Texel faceSkin = new Texel(225, 218, 175, '█');

            // Dark hollow eye sockets
            double leftEye = Math.hypot((fx + 0.026), (headY + 0.030) * 1.3);
            double rightEye = Math.hypot((fx - 0.026), (headY + 0.030) * 1.3);
            double eyeMask = smoothstep(0.022, 0.010, Math.min(leftEye, rightEye));

            // Wide open screaming oval mouth
            double mouthDist = Math.hypot(fx * 2.2, (headY - 0.048) * 1.15);
            double mouthMask = smoothstep(0.025, 0.012, mouthDist);

            double facialVoids = Math.max(eyeMask, mouthMask);
            if (facialVoids > 0.001) {
                faceSkin = blend(faceSkin, new Texel(25, 22, 30, '░'), facialVoids);
            }
            result = blend(result, faceSkin, headMask);
        }

        // Hands pressed against the sides of the face
        double handL = Math.hypot((fx + 0.055), (headY - 0.01) * 1.2);
        double handR = Math.hypot((fx - 0.055), (headY - 0.01) * 1.2);
        double handMask = smoothstep(0.026, 0.012, Math.min(handL, handR));
        if (handMask > 0.001) {
            Texel handSkin = new Texel(235, 225, 185, '█');
            result = blend(result, handSkin, handMask);
        }

        return result;
    }

    // -------------------------
    // Girl with a Pearl Earring
    // -------------------------

    private Texel renderGirlPearlEarring(double x, double y) {
        // 1. Full dark studio background across the entire canvas
        Texel result = new Texel(10, 12, 16, ' ');

        // 2. Ochre Jacket & White Collar (Sweeps naturally across bottom region)
        double jacketDist = distToSegment(x, y, 0.25, 0.95, 0.72, 0.62);
        double jacketMask = smoothstep(0.28, 0.0, jacketDist);
        if (jacketMask > 0.001) {
            Texel jacket = new Texel(150, 110, 48, '▓');

            // Crisp white linen collar angled at the neck
            double collarDist = distToSegment(x, y, 0.36, 0.61, 0.58, 0.67);
            double collarMask = smoothstep(0.038, 0.0, collarDist);
            if (collarMask > 0.001) {
                jacket = blend(jacket, new Texel(240, 235, 225, '█'), collarMask);
            }
            result = blend(result, jacket, jacketMask);
        }

        // 3. Face & Neck (Natural sfumato profile fading into dark shadow)
        double fx = x - 0.50;
        double headDist = distToSegment(x, y, 0.48, 0.38, 0.46, 0.60);
        double headMask = smoothstep(0.12, 0.02, headDist);

        if (headMask > 0.001) {
            // Chiaroscuro key light: lit on left, deep shadow on right
            double shadow = smoothstep(-0.05, 0.08, fx);
            Texel skinLight = new Texel(238, 198, 168, '█');
            Texel skinShadow = new Texel(115, 82, 68, '▒');
            Texel skin = blend(skinLight, skinShadow, shadow * 0.7);

            // Expressive liquid eye & specular catchlight
            double eyeDist = Math.hypot((x - 0.48) * 1.8, (y - 0.46) * 2.3);
            double eyeMask = smoothstep(0.04, 0.02, eyeDist);
            if (eyeMask > 0.001) {
                skin = blend(skin, new Texel(28, 22, 20, '▓'), eyeMask);
                double glint = Math.hypot(x - 0.492, y - 0.452);
                skin = blend(skin, new Texel(255, 255, 255, '█'), smoothstep(0.008, 0.002, glint));
            }

            // Subtly parted lips
            double lipDist = Math.hypot((x - 0.47) * 1.5, (y - 0.56) * 3.2);
            double lipMask = smoothstep(0.035, 0.018, lipDist);
            if (lipMask > 0.001) {
                skin = blend(skin, new Texel(180, 88, 82, '▒'), lipMask);
            }

            result = blend(result, skin, headMask);
        }

        // 4. Ultramarine Blue Turban & Golden Yellow Tail
        double turbanDist = distToSegment(x, y, 0.35, 0.22, 0.60, 0.38);
        double turbanMask = smoothstep(0.16, 0.03, turbanDist);
        if (turbanMask > 0.001) {
            Texel blueTurban = new Texel(20, 70, 170, '█');
            Texel blueShadow = new Texel(10, 30, 90, '▓');
            double fold = Math.sin(x * 15.0 + y * 20.0) * 0.5 + 0.5;
            Texel turban = blend(blueTurban, blueShadow, fold * 0.5);

            // Golden-yellow fabric tail draped behind the shoulder
            double tailDist = distToSegment(x, y, 0.58, 0.30, 0.62, 0.58);
            double tailMask = smoothstep(0.05, 0.01, tailDist);
            if (tailMask > 0.001) {
                turban = blend(turban, new Texel(225, 175, 40, '█'), tailMask);
            }

            result = blend(result, turban, turbanMask);
        }

        // 5. The Pearl Earring (Rendered directly at the neck/jaw junction)
        double px = 0.435, py = 0.568;
        double pearlDist = Math.hypot(x - px, y - py);
        double pearlMask = smoothstep(0.024, 0.015, pearlDist);
        if (pearlMask > 0.001) {
            Texel pearl = new Texel(170, 175, 185, '█');
            // Underside shadow reflection
            double pearlShade = smoothstep(-0.005, 0.018, y - py);
            pearl = blend(pearl, new Texel(30, 30, 40, '▒'), pearlShade * 0.7);
            // Sharp specular glint
            double glint = Math.hypot(x - (px - 0.005), y - (py - 0.006));
            pearl = blend(pearl, new Texel(255, 255, 255, '█'), smoothstep(0.008, 0.002, glint));

            result = blend(result, pearl, pearlMask);
        }

        return result;
    }

    // ---------------------------
    // The Great Wave off Kanagawa
    // ---------------------------

    private Texel renderGreatWave(double x, double y) {
        // 1. Brighter, authentic Edo woodblock warm parchment background
        Texel result = new Texel(230, 206, 165, '▓');

        // 2. Distant Mount Fuji (Centered low beneath the hollow arc of the wave)
        double fujiCx = 0.51, fujiBaseY = 0.72;
        double dxFuji = Math.abs(x - fujiCx);
        double fujiSlope = 0.54 + dxFuji * 1.25;
        if (dxFuji < 0.16 && y > 0.52 && y < fujiBaseY) {
            double fujiMask = smoothstep(fujiSlope - 0.005, fujiSlope + 0.005, y);
            if (fujiMask > 0.001) {
                Texel rockBase = new Texel(65, 75, 88, '▓');
                Texel snowPeak = new Texel(255, 255, 255, '█');
                double snowMask = smoothstep(0.60, 0.54, y);
                Texel fuji = blend(rockBase, snowPeak, snowMask);
                result = blend(result, fuji, fujiMask);
            }
        }

        // 3. Secondary Background Wave (Swell rising on the right horizon)
        double backWaveLine = 0.78 - 0.25 * Math.sin((x - 0.35) * 3.8);
        double backWaveMask = smoothstep(backWaveLine - 0.015, backWaveLine + 0.015, y) * smoothstep(0.35, 0.95, x);
        if (backWaveMask > 0.001) {
            Texel indigoDeep = new Texel(20, 50, 105, '▓');
            Texel foamEdge = new Texel(252, 252, 248, '█');
            double foamMask = smoothstep(backWaveLine + 0.02, backWaveLine - 0.01, y);
            result = blend(result, blend(indigoDeep, foamEdge, foamMask * 0.8), backWaveMask);
        }

        // 4. Slender Oshiokuri-bune Boats (Tossed in the troughs of the swell)
        double boat1Dist = distToSegment(x, y, 0.22, 0.72, 0.42, 0.78);
        double boat2Dist = distToSegment(x, y, 0.48, 0.70, 0.72, 0.76);
        double boatMask = Math.max(smoothstep(0.014, 0.0, boat1Dist), smoothstep(0.014, 0.0, boat2Dist));
        if (boatMask > 0.001) {
            result = blend(result, new Texel(140, 105, 65, '▒'), boatMask * 0.85);
        }

        // 5. Solid Prussian-Blue Wave Body (Flat woodblock tone without shimmering ripples)
        double waveArch = 0.88 - 1.2 * x + 1.8 * sq(x - 0.1) - 0.3 * Math.sin(x * 6.0);
        double waveMask = smoothstep(waveArch - 0.02, waveArch + 0.02, y);

        if (waveMask > 0.001) {
            Texel prussianBlue = new Texel(16, 45, 100, '█');
            Texel deepIndigo = new Texel(10, 28, 70, '█');
            // Flat vertical ink gradation rather than high-frequency sine shimmer
            double depthGradient = smoothstep(waveArch, waveArch + 0.25, y);
            Texel waveBody = blend(prussianBlue, deepIndigo, depthGradient * 0.45);

            result = blend(result, waveBody, waveMask);
        }

        // 6. Thick White Frothy Crest & Clawed Foam Tentacles ("Fluff" wave break)
        double crestX = 0.45, crestY = 0.32;
        double distCrest = Math.hypot(x - crestX, y - crestY);

        // Broad white frothy border hugging the entire upper contour of the wave lip
        double frothyCrest = smoothstep(waveArch - 0.06, waveArch - 0.01, y) * smoothstep(waveArch + 0.05, waveArch - 0.01, y);

        // Jagged foam claws breaking downward off the wave
        double clawPattern = Math.sin(x * 42.0) * Math.sin(y * 36.0 - x * 18.0);
        double clawMask = smoothstep(0.05, 0.65, clawPattern) * smoothstep(0.38, 0.0, distCrest);

        // Additional froth mass spilling along the left curl
        double frothCurl = smoothstep(0.32, 0.0, Math.hypot(x - 0.22, y - 0.52));

        double totalFoam = Math.max(frothyCrest * 1.3, Math.max(clawMask, frothCurl * 0.7));
        if (totalFoam > 0.001) {
            Texel pureWhiteFoam = new Texel(255, 255, 248, '█');
            result = blend(result, pureWhiteFoam, clamp01(totalFoam));
        }

        // Thick sea spray droplets suspended in the sky around the wave arch
        if (y < waveArch && x < 0.68) {
            double sprayField = Math.sin(x * 75.0) * Math.sin(y * 65.0 + x * 25.0);
            double sprayMask = smoothstep(0.80, 0.95, sprayField) * smoothstep(0.48, 0.08, distCrest);
            if (sprayMask > 0.001) {
                result = blend(result, new Texel(255, 255, 255, '█'), sprayMask);
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------------------------
    // Mondrian — Composition with Red, Blue and Yellow
    // (The original looks mostly of a red square, so this isnt the original this
    // is actually a variant that shows off more of a "1/3rds" grid for visual clarity)
    // ---------------------------------------------------------------------------------------

    private Texel renderMondrian(double x, double y) {
        // Off-white panels by default
        Texel result = new Texel(240, 235, 225, '█');

        if (x > 0.28 && x < 0.88 && y < 0.32) {
            result = new Texel(210, 35, 35, '█'); // Large red block
        } else if (x < 0.28 && y > 0.76) {
            result = new Texel(30, 65, 145, '█'); // Bottom-left blue block
        } else if (x > 0.88 && y > 0.76) {
            result = new Texel(235, 195, 20, '█'); // Bottom-right yellow block
        } else if (x < 0.28 && y < 0.32) {
            result = new Texel(25, 25, 30, '█'); // Small accent black block
        }

        // Crisp but anti-aliased black structural divider lines on top of the fills
        double lineX1 = Math.abs(x - 0.28) - 0.012;
        double lineX2 = Math.abs(x - 0.88) - 0.009;
        double lineY1 = Math.abs(y - 0.32) - 0.012;
        double lineY2 = Math.abs(y - 0.76) - 0.009;
        double lineDist = Math.min(Math.min(lineX1, lineX2), Math.min(lineY1, lineY2));
        double lineMask = smoothstep(0.006, -0.006, lineDist);
        if (lineMask > 0.001) {
            result = blend(result, new Texel(15, 15, 20, '█'), lineMask);
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------
    // Small compositing helpers
    // ---------------------------------------------------------------------------------------

    /** Lightweight internal color+glyph pair used for compositing before converting to VoxelTexel. */
    private static final class Texel {
        final int r, g, b;
        final char ch;

        Texel(int r, int g, int b, char ch) {
            this.r = clampByte(r);
            this.g = clampByte(g);
            this.b = clampByte(b);
            this.ch = ch;
        }

        VoxelTexel toVoxel() {
            return new VoxelTexel(r, g, b, ch);
        }
    }

    private static Texel blend(Texel a, Texel b, double t) {
        t = clamp01(t);
        if (t <= 0.001) return a;
        if (t >= 0.999) return b;
        int r = lerp(a.r, b.r, t);
        int g = lerp(a.g, b.g, t);
        int bl = lerp(a.b, b.b, t);
        char ch = t < 0.5 ? a.ch : b.ch;
        return new Texel(r, g, bl, ch);
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private static double lerpD(double a, double b, double t) {
        return a + (b - a) * clamp01(t);
    }

    /** Standard smoothstep; works even when edge0 > edge1 (gives an inverted ramp). */
    private static double smoothstep(double edge0, double edge1, double x) {
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3 - 2 * t);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Shortest distance from point (px,py) to the segment (ax,ay)-(bx,by). */
    private static double distToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double abx = bx - ax, aby = by - ay;
        double t = ((px - ax) * abx + (py - ay) * aby) / (abx * abx + aby * aby + 1e-9);
        t = clamp01(t);
        double cx = ax + abx * t, cy = ay + aby * t;
        return Math.hypot(px - cx, py - cy);
    }

    // One rotating swirl field centered at (cx, cy); returns a soft 0..1 intensity.
    private double swirl(double x, double y, double cx, double cy, double freq) {
        double dx = x - cx;
        double dy = (y - cy) * 1.3;
        double dist = Math.hypot(dx, dy);
        double angle = Math.atan2(dy, dx) + dist * freq;
        double band = Math.sin(angle * 3.0 + dist * 10.0);
        double falloff = smoothstep(0.42, 0.05, dist);
        return clamp01(band * 0.5 + 0.5) * falloff;
    }
}