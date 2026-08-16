// TODO: Improve the visual clarity of those artworks

public class TexelMuseumCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] ART_STAGES = new StatusStage[] {
            new StatusStage(15, "Erecting Gallery Walls:"),
            new StatusStage(35, "Stretching Canvas Linens:"),
            new StatusStage(55, "Mixing Raw Oil Pigments:"),
            new StatusStage(75, "Applying Gold Leaf Frames:"),
            new StatusStage(90, "Adjusting Track Lighting:"),
            new StatusStage(100, "Exhibition Open!")
    };

    private static final int TEXTURE_RESOLUTION = 256;

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
        final double aa = EDGE_SOFTNESS_PX;

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

    // ---------------------------------------------------------------------------------------
    // Frame layers
    // ---------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------
    // Mona Lisa
    // ---------------------------------------------------------------------------------------

    private Texel renderMonaLisa(double x, double y) {
        // Dark, moody sfumato background
        Texel result = new Texel(35, 40, 30, '░');

        // Hazy winding aerial-perspective landscape band behind the shoulders
        double horizon = 0.46 + 0.02 * Math.sin(x * 5.0);
        double haze = smoothstep(horizon + 0.10, horizon - 0.03, y) * smoothstep(horizon - 0.12, horizon, y);
        if (haze > 0.001) {
            result = blend(result, new Texel(55, 70, 62, '░'), haze * 0.8);
        }

        // Shoulders/torso as a soft pyramid that widens toward the bottom edge. Modelling it
        // this way (rather than one ellipse shared with the face) means the head never gets
        // clipped by the torso's boundary.
        double torsoTop = 0.5;
        double halfWidth = lerpD(0.07, 0.34, smoothstep(torsoTop, 0.95, y));
        double bodyMask = smoothstep(torsoTop - 0.03, torsoTop + 0.03, y)
                * smoothstep(halfWidth + 0.02, halfWidth - 0.02, Math.abs(x - 0.5));
        if (bodyMask > 0.001) {
            result = blend(result, new Texel(30, 28, 24, '▒'), bodyMask);

            // Folded hands resting low on the torso
            double hx = x - 0.5;
            double hy = y - 0.88;
            double hands = (hy * hy * 3.0) + (hx * hx * 1.1);
            double handsMask = smoothstep(0.022, 0.008, hands) * bodyMask;
            if (handsMask > 0.001) {
                result = blend(result, new Texel(195, 165, 130, '█'), handsMask);
            }
        }

        // Dark hair framing the head — drawn first and slightly larger than the face oval so
        // only a thin, natural-looking border shows once the face is painted on top.
        double fx = x - 0.5;
        double fy = y - 0.37;
        double hairShape = (fy * fy * 1.55) + (fx * fx * 0.92);
        double hairMask = smoothstep(0.034, 0.026, hairShape);
        if (hairMask > 0.001) {
            result = blend(result, new Texel(28, 24, 19, '▓'), hairMask);
        }

        // Oval face, drawn independently of the torso so it's never accidentally masked out.
        double face = (fy * fy * 1.7) + (fx * fx);
        double faceMask = smoothstep(0.027, 0.019, face);
        if (faceMask > 0.001) {
            Texel skin = new Texel(197, 167, 127, '█');

            // Faint enigmatic smile: a shallow upward curve below face-center
            double smileCurve = (fx * fx) * 2.0 - 0.065;
            boolean smile = Math.abs(fy - smileCurve) < 0.008 && Math.abs(fx) < 0.065;

            // Soft almond eyes flanking a shadowed nose bridge
            boolean leftEye = Math.abs(fx + 0.028) < 0.016 && Math.abs(fy + 0.016) < 0.006;
            boolean rightEye = Math.abs(fx - 0.028) < 0.016 && Math.abs(fy + 0.016) < 0.006;

            if (smile) {
                skin = new Texel(115, 82, 68, '▒');
            } else if (leftEye || rightEye) {
                skin = new Texel(52, 44, 35, '▒');
            }
            result = blend(result, skin, faceMask);
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------
    // The Starry Night
    // ---------------------------------------------------------------------------------------

    private Texel renderStarryNight(double x, double y) {
        // Deep indigo night sky
        Texel result = new Texel(15, 24, 70, '▒');

        // Two large rotating swirls (the painting's signature turbulence), built from layered
        // sine fields instead of one flat diagonal band, so they actually read as spirals.
        double swirlA = swirl(x, y, 0.32, 0.28, 5.5);
        double swirlB = swirl(x, y, 0.72, 0.2, 4.5);
        double swirl = Math.max(swirlA, swirlB) * smoothstep(0.72, 0.55, y);
        if (swirl > 0.001) {
            result = blend(result, new Texel(70, 115, 175, '░'), clamp01(swirl));
        }

        // Moon: soft radial glow around a bright core (top-right)
        double moonDist = Math.hypot(x - 0.82, y - 0.2);
        double moonGlow = smoothstep(0.14, 0.0, moonDist);
        if (moonGlow > 0.001) {
            result = blend(result, new Texel(230, 205, 120, '░'), moonGlow * 0.55);
        }
        double moonCore = smoothstep(0.075, 0.05, moonDist);
        if (moonCore > 0.001) {
            result = blend(result, new Texel(250, 235, 160, '█'), moonCore);
        }

        // Radiant stars with soft halos, scattered across the upper sky
        if (y < 0.62) {
            double starField = Math.sin(x * 23.0) * Math.sin(y * 21.0 + x * 3.0);
            double starMask = smoothstep(0.78, 0.94, starField);
            if (starMask > 0.001) {
                result = blend(result, new Texel(250, 232, 150, '█'), starMask);
            }
            double haloMask = smoothstep(0.55, 0.75, starField) * 0.4;
            if (haloMask > 0.001) {
                result = blend(result, new Texel(200, 190, 140, '░'), haloMask);
            }
        }

        // Rolling blue-green hills across the lower canvas
        double hillLine = 0.72 + 0.015 * Math.sin(x * 10.0);
        double hills = smoothstep(hillLine, hillLine + 0.03, y);
        if (hills > 0.001) {
            result = blend(result, new Texel(18, 42, 52, '▓'), hills);

            // A small sleeping village along the hillside: dark rooftops with warm window lights
            double village = Math.sin(x * 40.0) * Math.sin((y - hillLine) * 60.0);
            double roofMask = smoothstep(0.5, 0.7, village) * hills * smoothstep(hillLine + 0.16, hillLine + 0.02, y);
            if (roofMask > 0.001) {
                result = blend(result, new Texel(10, 12, 16, '▓'), roofMask);
            }
            boolean windowRow = ((int) (x * 80) % 7 == 0) && y > hillLine + 0.01 && y < hillLine + 0.05;
            if (windowRow) {
                result = blend(result, new Texel(235, 190, 90, '█'), 0.85);
            }
            // A single church spire, taller than the surrounding rooftops
            double spireDx = x - 0.5;
            if (Math.abs(spireDx) < 0.012 && y > hillLine - 0.1 && y < hillLine + 0.02) {
                result = blend(result, new Texel(8, 10, 14, '█'), 0.9);
            }
        }

        // Giant dark cypress flame silhouette on the left, softened along its edge
        double cypressAxis = 0.22 + 0.03 * Math.sin(y * 14.0);
        double cypressWidth = 0.05 * (1.0 - y * 0.3);
        double cypressDist = (Math.abs(x - cypressAxis) - cypressWidth) * 8.0;
        double cypressMask = smoothstep(0.6, -0.4, cypressDist) * smoothstep(0.05, 0.15, 1.0 - y);
        cypressMask *= smoothstep(0.0, 0.15, y); // keep it clipped to the frame, not floating
        if (cypressMask > 0.001) {
            result = blend(result, new Texel(10, 20, 16, '█'), cypressMask);
        }
        return result;
    }

    /** One rotating swirl field centered at (cx, cy); returns a soft 0..1 intensity. */
    private double swirl(double x, double y, double cx, double cy, double freq) {
        double dx = x - cx;
        double dy = (y - cy) * 1.3;
        double dist = Math.hypot(dx, dy);
        double angle = Math.atan2(dy, dx) + dist * freq;
        double band = Math.sin(angle * 3.0 + dist * 10.0);
        double falloff = smoothstep(0.42, 0.05, dist);
        return clamp01(band * 0.5 + 0.5) * falloff;
    }

    // ---------------------------------------------------------------------------------------
    // The Scream
    // ---------------------------------------------------------------------------------------

    private Texel renderTheScream(double x, double y) {
        Texel result;

        if (y < 0.4) {
            // Turbulent blood-orange sky built from layered waves, not a flat gradient
            double band = Math.sin(x * 6.0 + y * 3.0) * 0.5 + Math.sin(x * 11.0 - y * 5.0) * 0.25;
            int r = clampByte((int) (225 - y * 90 + band * 20));
            int g = clampByte((int) (95 + band * 25));
            int b = clampByte((int) (45 + band * 10));
            result = new Texel(r, g, b, '█');
        } else if (y < 0.62) {
            // Turbid dark shoreline / fjord water
            double waveY = 0.5 + 0.08 * Math.sin(x * 4.0);
            double waterMask = smoothstep(waveY - 0.02, waveY + 0.02, y);
            result = blend(new Texel(160, 130, 70, '▒'), new Texel(25, 40, 80, '▒'), waterMask);
        } else {
            // Wooden bridge in steep perspective, with a railing line kept clear of the figure
            double r = 110 + x * 40;
            Texel plank = new Texel((int) r, 75, 60, '▒');
            double rail = Math.abs((y - 0.62) - (x - 0.5) * 0.55);
            double railMask = smoothstep(0.02, 0.0, rail) * smoothstep(0.21, 0.3, Math.abs(x - 0.5));
            result = blend(plank, new Texel(35, 30, 28, '▓'), railMask);

            // Two small silhouetted figures walking away, further up the bridge
            double figDist = Math.hypot((x - 0.85) * 3.0, (y - 0.68) * 6.0);
            double figMask = smoothstep(0.05, 0.0, figDist);
            if (figMask > 0.001) {
                result = blend(result, new Texel(15, 14, 18, '█'), figMask);
            }
        }

        // Distorted, elongated screaming figure in the foreground
        double fx = x - 0.5;
        double fy = y - 0.66;
        double head = (fy * fy * 2.1) + (fx * fx * 0.75);
        double headMask = smoothstep(0.028, 0.02, head);
        if (headMask > 0.001) {
            Texel face = new Texel(215, 210, 175, '█');

            // Hollow, wide eye sockets
            double eyeDist = Math.min(
                    Math.hypot((fx + 0.028), (fy + 0.03) * 1.3),
                    Math.hypot((fx - 0.028), (fy + 0.03) * 1.3));
            double eyeMask = smoothstep(0.02, 0.01, eyeDist);

            // Open, screaming oval mouth
            double mouthDist = Math.hypot(fx * 2.0, (fy - 0.05) * 1.1);
            double mouthMask = smoothstep(0.022, 0.012, mouthDist);

            double voidMask = Math.max(eyeMask, mouthMask);
            if (voidMask > 0.001) {
                face = blend(face, new Texel(28, 28, 34, '░'), voidMask);
            }
            result = blend(result, face, headMask);
        }

        // Hands pressed to either side of the face. This is a separate shape at its own
        // location, so it must be evaluated independently of headMask above (not nested
        // inside it) or it would never get a chance to draw.
        double handL = Math.hypot((fx + 0.225), (fy - 0.01) * 1.3);
        double handR = Math.hypot((fx - 0.225), (fy - 0.01) * 1.3);
        double handMask = smoothstep(0.048, 0.028, Math.min(handL, handR));
        if (handMask > 0.001) {
            result = blend(result, new Texel(232, 224, 198, '█'), handMask);
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------
    // Girl with a Pearl Earring
    // ---------------------------------------------------------------------------------------

    private Texel renderGirlPearlEarring(double x, double y) {
        // Deep, pure black studio background
        Texel result = new Texel(14, 14, 17, ' ');

        double dx = x - 0.52;
        double dy = y - 0.52;
        double body = (dy * dy * 1.2) + (dx * dx);
        double bodyMask = smoothstep(0.14, 0.11, body);
        if (bodyMask < 0.001) {
            return result;
        }

        Texel layer;
        if (y < 0.4) {
            // Ultramarine blue turban wrap, with a lemon-yellow cascading fabric tail
            double tailMask = smoothstep(0.55, 0.62, x);
            layer = blend(new Texel(22, 68, 155, '█'), new Texel(212, 162, 48, '█'), tailMask);

            // Soft fold lines across the wrap
            double fold = Math.sin((x * 0.8 + y * 2.0) * 22.0) * 0.5 + 0.5;
            layer = blend(layer, new Texel(10, 30, 90, '▒'), smoothstep(0.85, 0.97, fold) * 0.5);
        } else if (y < 0.6) {
            // Luminous pale flesh profile, with soft shadow toward the far cheek
            double shadow = smoothstep(-0.02, -0.09, dx);
            layer = blend(new Texel(228, 188, 158, '█'), new Texel(120, 92, 78, '▓'), shadow);

            // Large dark eye with a small catchlight
            double eyeDist = Math.hypot((x - 0.5) * 1.6, (y - 0.47) * 2.2);
            double eyeMask = smoothstep(0.05, 0.03, eyeDist);
            if (eyeMask > 0.001) {
                layer = blend(layer, new Texel(35, 28, 24, '▓'), eyeMask);
                double glintDist = Math.hypot(x - 0.515, y - 0.462);
                layer = blend(layer, new Texel(245, 245, 250, '█'), smoothstep(0.01, 0.004, glintDist));
            }

            // Subtly parted lips
            double lipDist = Math.hypot((x - 0.5) * 1.4, (y - 0.565) * 3.2);
            layer = blend(layer, new Texel(178, 96, 90, '▒'), smoothstep(0.045, 0.025, lipDist));

            // The famous pearl: bright highlight on top, soft dark underside
            double pearlDist = Math.hypot(x - 0.46, y - 0.56);
            double pearlMask = smoothstep(0.025, 0.017, pearlDist);
            if (pearlMask > 0.001) {
                Texel pearl = new Texel(150, 155, 165, '█');
                pearl = blend(pearl, new Texel(40, 40, 50, '▒'), smoothstep(0.0, 0.02, (y - 0.567)));
                double glint = smoothstep(0.008, 0.003, Math.hypot(x - 0.454, y - 0.553));
                pearl = blend(pearl, new Texel(250, 250, 255, '█'), glint);
                layer = blend(layer, pearl, pearlMask);
            }
        } else {
            // Earthy ochre jacket with a crisp white collar
            layer = new Texel(145, 110, 55, '▓');
            double collarMask = smoothstep(0.09, 0.06, Math.abs(dx)) * smoothstep(0.66, 0.6, y);
            layer = blend(layer, new Texel(230, 225, 220, '█'), collarMask);
        }

        return blend(result, layer, bodyMask);
    }

    // ---------------------------------------------------------------------------------------
    // The Great Wave off Kanagawa
    // ---------------------------------------------------------------------------------------

    private Texel renderGreatWave(double x, double y) {
        // Traditional flat tan print-paper background
        Texel result = new Texel(215, 200, 170, '░');

        // Deep ocean base
        double oceanMask = smoothstep(0.52, 0.6, y);
        if (oceanMask > 0.001) {
            result = blend(result, new Texel(30, 60, 115, '▒'), oceanMask);
        }

        // Distant swell contour lines (Hokusai's Prussian-blue banding)
        for (int i = 1; i <= 3; i++) {
            double bandY = 0.58 + i * 0.1;
            double band = smoothstep(0.006, 0.0, Math.abs(y - bandY - 0.02 * Math.sin(x * 8.0)));
            if (band > 0.001) {
                result = blend(result, new Texel(18, 42, 90, '▓'), band * 0.6);
            }
        }

        // Small boats tossed on the swell (dark elongated silhouettes)
        for (double bx : new double[] { 0.2, 0.34, 0.5 }) {
            double boatY = 0.68 + 0.02 * Math.sin(bx * 30.0);
            double boatDist = Math.hypot((x - bx) * 3.5, (y - boatY) * 14.0);
            double boatMask = smoothstep(0.55, 0.3, boatDist);
            if (boatMask > 0.001) {
                result = blend(result, new Texel(20, 18, 20, '▓'), boatMask);
            }
        }

        // The great crest curve, sweeping up from the lower-left
        double waveCurve = 0.82 - Math.pow(x - 0.08, 2) * 1.05 - 0.38 * x;
        double crestMask = smoothstep(waveCurve - 0.015, waveCurve + 0.015, y);
        if (crestMask > 0.001) {
            result = blend(result, new Texel(15, 45, 95, '▓'), crestMask);
        }

        // Clawed foam fingers breaking off the top lip of the wave
        if (x < 0.68) {
            double clawField = Math.sin(x * 55.0) * 0.5 + 0.5;
            double clawReach = 0.02 + clawField * 0.03;
            double foamDist = (y - waveCurve) + clawReach;
            double foamMask = smoothstep(0.045, 0.0, Math.abs(foamDist)) * smoothstep(waveCurve - 0.1, waveCurve, y);
            if (foamMask > 0.001) {
                result = blend(result, new Texel(248, 248, 242, '█'), foamMask);
            }
        }

        // Small centered Mount Fuji silhouette with a snow-capped peak. The mountain's solid
        // body is everything BELOW the slope line down to the base of its bounding box.
        double fujiSlope = 0.56 + Math.abs(x - 0.51) * 1.15;
        boolean inFujiRange = x > 0.36 && x < 0.66 && y > 0.52 && y <= 0.7;
        if (inFujiRange) {
            double fujiMask = smoothstep(fujiSlope - 0.006, fujiSlope + 0.006, y);
            if (fujiMask > 0.001) {
                // A warm slate-brown rock body reads clearly against the cool ocean blues
                Texel rock = new Texel(95, 78, 72, '▒');
                Texel snow = new Texel(250, 250, 250, '█');
                double snowMask = smoothstep(fujiSlope + 0.03, fujiSlope + 0.005, y);
                Texel fuji = blend(rock, snow, snowMask);
                result = blend(result, fuji, fujiMask);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------
    // Mondrian — Composition with Red, Blue and Yellow
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
}