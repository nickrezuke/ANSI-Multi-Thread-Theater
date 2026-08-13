// TODO: Give the Magnifying Glass a "Handle" and maybe a little lens white reflection?  Like the classic magnifying glass look
// TODO: Improve the background, more things to zoom in on?
// TODO: Perhaps also apply this magnifying glass to other 2D environment loaders?

public class RefractiveMagnifierLoader extends Loader {
    private static final StatusStage[] INTENSE_MAG_STAGES = {
        new StatusStage(25, "Rendering procedural photograph:"),
        new StatusStage(50, "Molding extreme convex glass curvature:"),
        new StatusStage(75, "Compounding refractive Snell vectors:"),
        new StatusStage(100, "High-Power Optical Matrix Operational!")
    };

    private double timeClock = 0.0;
    private final int width = 80;
    private final int height = 22;

    // Fixed x position (in scene-U space, 0..1) of the pine tree and boat.
    private static final double TREE_X = 0.16;
    private static final double BOAT_X = 0.50;

    // The lake's flat waterline (in scene-V space, 0..1).
    private static final double WATERLINE = 0.62;

    // Sky gradient stops: deep dusk violet up top, warming to a pale golden
    // glow at the horizon.
    private static final double[] SKY_STOPS = {0.00, 0.25, 0.50, 0.72, 0.90, 1.00};
    private static final int[][] SKY_COLORS = {
        {45, 18, 80}, {110, 35, 120}, {200, 70, 95}, {240, 120, 60}, {250, 180, 90}, {255, 215, 140}
    };

    public RefractiveMagnifierLoader() {
        super(INTENSE_MAG_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        this.timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        timeClock += 0.012;

        // Lissajous-style drift so the lens wanders broadly across the whole
        // scene rather than orbiting a small patch of it. Amplitudes are
        // kept a safe margin inside the screen bounds so the glass (plus its
        // rim) is never clipped by the edge of the canvas.
        double glassCenterX = 1.60 * Math.sin(timeClock * 0.45);
        double glassCenterY = 0.38 * Math.sin(timeClock * 0.71 + 1.3);
        double glassCenterZ = -0.6;

        double lensRadius = 0.56;
        double rimThickness = 0.035;
        double lightX = 0.577, lightY = -0.707, lightZ = -0.408;

        for (int y = 0; y < height; y++) {
            double screenY = ((double) y / height) * 2.0 - 1.0;

            for (int x = 0; x < width; x++) {
                int idx = x + y * width;
                double screenX = (((double) x / width) * 2.0 - 1.0) * 2.3;

                double paperU = (screenX + 2.3) / 4.6;
                double paperV = (screenY + 1.0) / 2.0;

                double dx = screenX - glassCenterX;
                double dy = screenY - glassCenterY;
                double distToCenter = Math.sqrt(dx * dx + dy * dy);

                // -------------------------------------------------------------
                // OBJECT A: BRASS LENS RIM
                // -------------------------------------------------------------
                if (distToCenter >= lensRadius && distToCenter < lensRadius + rimThickness) {
                    double depthZ = 1.0 / (-glassCenterZ);
                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;
                        double nx = dx / distToCenter;
                        double ny = dy / distToCenter;
                        double spec = Math.pow(Math.max(0, nx * lightX + ny * lightY), 12);

                        int r = clamp((int) (175 + spec * 80));
                        int g = clamp((int) (145 + spec * 90));
                        int b = clamp((int) (85 + spec * 70));

                        char renderChar = (spec > 0.4) ? '█' : '▓';
                        outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // OBJECT B: HIGH-POWER CONVEX MAGNIFYING LENS
                // -------------------------------------------------------------
                if (distToCenter < lensRadius) {
                    double depthZ = 1.0 / (-glassCenterZ);
                    if (depthZ > zBuffer[idx]) {
                        zBuffer[idx] = depthZ;

                        double t = distToCenter / lensRadius; // 0 at center .. 1 at rim

                        // Radial magnification: strong zoom near the optical
                        // center, easing to an exact 1:1 sample at the rim so
                        // the magnified view melts into the backdrop with no
                        // hard seam.
                        double zoomStrength = 5.5;
                        double falloff = (1.0 - t) * (1.0 - t);
                        double magFactor = 1.0 / (1.0 + zoomStrength * falloff);

                        // A gentle swirl that grows toward the center gives
                        // the content a "warped through curved glass" look
                        // instead of a flat crop-and-scale.
                        double swirlAngle = (1.0 - t) * 0.35;
                        double cosA = Math.cos(swirlAngle), sinA = Math.sin(swirlAngle);
                        double rotX = dx * cosA - dy * sinA;
                        double rotY = dx * sinA + dy * cosA;

                        double sampleX = glassCenterX + rotX * magFactor;
                        double sampleY = glassCenterY + rotY * magFactor;
                        double lensU = (sampleX + 2.3) / 4.6;
                        double lensV = (sampleY + 1.0) / 2.0;

                        ScenePixel px = sampleScene(lensU, lensV);
                        int r = px.r, g = px.g, b = px.b;
                        char renderChar = px.glyph;

                        // Spherical convex-glass normal, used purely for
                        // shading (never for sampling), so the lens reads as
                        // a rounded 3D dome.
                        double curveness = 0.9;
                        double nz = Math.sqrt(Math.max(0.0, 1.0 - t * t * curveness * curveness));
                        double nx = -(dx / lensRadius) * curveness;
                        double ny = -(dy / lensRadius) * curveness;
                        double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                        nx /= nLen; ny /= nLen; nz /= nLen;

                        double spec = Math.max(0.0, nx * lightX + ny * lightY + nz * lightZ);
                        double glint = Math.pow(spec, 24);
                        double fresnel = (1.0 - nz) * (1.0 - nz);

                        // Cool glass tint over whatever the scene sampled.
                        r = (int) (r * 0.90 + 210 * 0.10);
                        g = (int) (g * 0.90 + 230 * 0.10);
                        b = (int) (b * 0.90 + 255 * 0.10);

                        r = clamp(r + (int) (glint * 190) + (int) (fresnel * 35));
                        g = clamp(g + (int) (glint * 190) + (int) (fresnel * 40));
                        b = clamp(b + (int) (glint * 190) + (int) (fresnel * 45));

                        if (glint > 0.55) {
                            renderChar = '*'; // bright glass sparkle
                        }

                        outputBuffer[idx] = ansi(r, g, b) + renderChar + RESET;
                        continue;
                    }
                }

                // -------------------------------------------------------------
                // UNMAGNIFIED BACKDROP
                // -------------------------------------------------------------
                ScenePixel px = sampleScene(paperU, paperV);
                outputBuffer[idx] = ansi(px.r, px.g, px.b) + px.glyph + RESET;
            }
        }
    }

    // -----------------------------------------------------------------------
    // PROCEDURAL SCENE: a small dusk landscape sampled by (u, v) in [0,1]^2.
    // Every band returns a solid, colored glyph - nothing is left as blank
    // space - so the whole canvas reads as a dense, colorful image rather
    // than sparse ASCII art on a black field. Swap this out for real
    // pixel-buffer sampling if/when image loading is available - everything
    // else (lens warp, shading, rim) is agnostic to where the color data
    // actually comes from.
    // -----------------------------------------------------------------------

    private ScenePixel sampleScene(double u, double v) {
        // Foreground pine tree - checked first since it stands in front of
        // everything behind it.
        if (isTree(u, v)) {
            double patch = hashNoise(Math.floor(u * 300.0), Math.floor(v * 150.0));
            int gr = patch > 0.5 ? 34 : 22;
            int gg = patch > 0.5 ? 92 : 64;
            int gb = patch > 0.5 ? 46 : 32;
            return new ScenePixel(gr, gg, gb, '♣');
        }

        // A tiny sailboat resting on the lake - easy to miss unmagnified,
        // clearly a boat once the lens passes over it.
        if (isBoatSail(u, v)) {
            return new ScenePixel(245, 235, 220, '▲');
        }
        if (isBoatMast(u, v)) {
            return new ScenePixel(60, 45, 35, '│');
        }
        if (isBoatHull(u, v)) {
            return new ScenePixel(80, 50, 30, '▄');
        }

        // Grassy shore, with scattered flowers.
        double shore = shoreLevel(u);
        if (v >= shore) {
            double n = hashNoise(Math.floor(u * 220.0), Math.floor(v * 110.0));
            if (n > 0.965) {
                double pick = hashNoise(Math.floor(u * 220.0) + 7.0, Math.floor(v * 110.0) + 11.0);
                int[] fc = pick < 0.34 ? new int[]{235, 70, 100}
                         : pick < 0.67 ? new int[]{250, 210, 70}
                         : new int[]{255, 255, 255};
                return new ScenePixel(fc[0], fc[1], fc[2], '✿');
            }
            double depth = Math.min(1.0, (v - shore) / (1.0 - shore));
            double patch = hashNoise(Math.floor(u * 90.0), Math.floor(v * 45.0));
            int r = (int) (18 + depth * 12);
            int g = (int) (75 + patch * 30 - depth * 15);
            int b = (int) (28 + depth * 6);
            return new ScenePixel(r, g, b, blockForShade(0.35 + depth * 0.5 + patch * 0.15));
        }

        // A couple of distant birds.
        if (isBird(u, v, 0.40, 0.19) || isBird(u, v, 0.47, 0.15)) {
            return new ScenePixel(55, 42, 40, '^');
        }

        // Sun - positioned clear of both ridgelines, so no occlusion check
        // is needed; the mountains never cut into it.
        double sunX = 0.66, sunY = 0.22, sunR = 0.095;
        double sdx = u - sunX, sdy = (v - sunY) * 1.7; // aspect-corrected disc
        double distSun = Math.sqrt(sdx * sdx + sdy * sdy);
        if (distSun < sunR) {
            double glow = 1.0 - (distSun / sunR);
            int r = 255, g = (int) (170 + glow * 70), b = (int) (70 + glow * 60);
            char glyph = glow > 0.6 ? '█' : (glow > 0.3 ? '▓' : '▒');
            return new ScenePixel(r, g, b, glyph);
        }
        if (distSun < sunR * 1.7) {
            double glow = 1.0 - ((distSun - sunR) / (sunR * 0.7));
            int r = 255, g = (int) (150 + glow * 65), b = (int) (95 + glow * 55);
            return new ScenePixel(r, g, b, glow > 0.5 ? '▒' : '░');
        }

        double farRidge = farRidgeLevel(u);
        double nearRidge = nearRidgeLevel(u);

        // Distant ridge - cool, hazy, atmospheric-perspective colors.
        if (v >= farRidge && v < nearRidge && v < WATERLINE) {
            double shade = Math.min(1.0, (v - farRidge) * 6.0);
            int r = (int) (95 + shade * 20);
            int g = (int) (95 + shade * 15);
            int b = (int) (140 + shade * 20);
            char glyph = shade < 0.2 ? '^' : blockForShade(shade);
            return new ScenePixel(r, g, b, glyph);
        }

        // Near ridge - warmer, richer colors, with an occasional mineral
        // glint (barely visible unmagnified, a clear fleck under the lens).
        if (v >= nearRidge && v < WATERLINE) {
            double gl = hashNoise(Math.floor(u * 320.0) + 3.0, Math.floor(v * 160.0) + 9.0);
            if (gl > 0.988) {
                return new ScenePixel(215, 240, 255, '✦');
            }
            double shade = Math.min(1.0, (v - nearRidge) * 5.0);
            int r = (int) (60 + shade * 45);
            int g = (int) (38 + shade * 30);
            int b = (int) (75 + shade * 35);
            char glyph = shade < 0.15 ? '^' : blockForShade(shade);
            return new ScenePixel(r, g, b, glyph);
        }

        // Lake - reflects the sky's palette, with ripples, a shimmering
        // sun-glint reflection, and its own hidden sparkle layer.
        if (v >= WATERLINE) {
            double shore2 = shoreLevel(u);
            double depth = Math.min(1.0, (v - WATERLINE) / Math.max(0.02, shore2 - WATERLINE));

            int[] farColor = {150, 118, 120};
            int[] nearColor = {22, 78, 88};
            int[] water = lerpColor(farColor, nearColor, depth);

            double ripple = Math.sin(v * 55.0 + u * 6.0 + timeClock * 2.2);
            water[0] = clamp(water[0] + (int) (ripple * 10));
            water[1] = clamp(water[1] + (int) (ripple * 10));
            water[2] = clamp(water[2] + (int) (ripple * 14));

            double wobble = 0.02 * Math.sin(v * 60.0 + timeClock * 3.0);
            if (Math.abs(u - sunX - wobble) < 0.02) {
                double fade = 1.0 - depth * 0.6;
                return new ScenePixel(255, (int) (180 + fade * 60), (int) (110 + fade * 50), '~');
            }

            double gl = hashNoise(Math.floor(u * 260.0) + 21.0, Math.floor(v * 130.0) + 5.0);
            if (gl > 0.985) {
                return new ScenePixel(230, 245, 255, '✦');
            }

            char glyph = ripple > 0.4 ? '▒' : blockForShade(0.3 + depth * 0.3);
            return new ScenePixel(water[0], water[1], water[2], glyph);
        }

        // Dusk sky - a warm multi-stop gradient with soft drifting clouds
        // and a scattering of stars.
        double tt = Math.min(1.0, v / Math.max(0.05, farRidge));
        int[] sky = skyColor(tt);

        double cloud = cloudDensity(u, v);
        if (cloud > 0.05) {
            int[] cloudColor = {255, 225, 215};
            sky = lerpColor(sky, cloudColor, Math.min(1.0, cloud) * 0.85);
            return new ScenePixel(sky[0], sky[1], sky[2], blockForShade(0.12 + cloud * 0.5));
        }
        if (tt < 0.45) {
            double n = hashNoise(Math.floor(u * 160.0), Math.floor(v * 80.0));
            if (n > 0.988) {
                double pick = hashNoise(Math.floor(u * 160.0) + 50.0, Math.floor(v * 80.0) + 50.0);
                int[] starColor = pick < 0.5 ? new int[]{255, 255, 240} : new int[]{255, 225, 180};
                return new ScenePixel(starColor[0], starColor[1], starColor[2], n > 0.996 ? '*' : '.');
            }
        }

        return new ScenePixel(sky[0], sky[1], sky[2], '░');
    }

    private double farRidgeLevel(double u) {
        return 0.38 + 0.05 * Math.sin(u * 5.0 + 0.5) + 0.02 * Math.sin(u * 13.0 + 1.0);
    }

    private double nearRidgeLevel(double u) {
        return 0.50 + 0.06 * Math.sin(u * 8.0) + 0.025 * Math.sin(u * 19.0 + 2.0);
    }

    private double shoreLevel(double u) {
        return 0.85 + 0.01 * Math.sin(u * 25.0);
    }

    private double cloudDensity(double u, double v) {
        double drift = (timeClock * 0.015) % 1.4;
        double[][] puffs = {
            {0.12, 0.16, 0.09, 0.032},
            {0.20, 0.19, 0.07, 0.028},
            {0.52, 0.11, 0.11, 0.036},
            {0.82, 0.20, 0.08, 0.030},
            {0.34, 0.30, 0.10, 0.034},
        };
        double density = 0.0;
        for (double[] p : puffs) {
            double cx = p[0] + drift;
            if (cx > 1.15) {
                cx -= 1.4;
            }
            double ddx = (u - cx) / p[2];
            double ddy = (v - p[1]) / p[3];
            double d = ddx * ddx + ddy * ddy;
            density += Math.max(0.0, 1.0 - d);
        }
        return Math.min(1.0, density);
    }

    private boolean isTree(double u, double v) {
        double base = 0.865;
        double du = u - TREE_X;

        // Trunk.
        if (Math.abs(du) < 0.006 && v > base - 0.05 && v <= base) {
            return true;
        }

        // Three stacked triangles make a simple pine-tree canopy.
        for (int i = 0; i < 3; i++) {
            double layerBase = base - 0.05 - i * 0.045;
            double layerTop = layerBase - 0.06;
            double halfWidth = 0.05 - i * 0.012;
            if (v >= layerTop && v <= layerBase) {
                double frac = (v - layerTop) / (layerBase - layerTop);
                if (Math.abs(du) <= halfWidth * frac) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBoatHull(double u, double v) {
        double by = WATERLINE + 0.015;
        return v >= by && v <= by + 0.014 && Math.abs(u - BOAT_X) <= 0.045;
    }

    private boolean isBoatMast(double u, double v) {
        double by = WATERLINE + 0.015;
        return v >= by - 0.075 && v <= by && Math.abs(u - BOAT_X) <= 0.004;
    }

    private boolean isBoatSail(double u, double v) {
        double by = WATERLINE + 0.015;
        double top = by - 0.07;
        if (v < top || v > by) {
            return false;
        }
        double frac = (v - top) / (by - top);
        double du = u - BOAT_X;
        return du >= 0.004 && du <= 0.004 + 0.032 * frac;
    }

    private boolean isBird(double u, double v, double cx, double cy) {
        double du = u - cx;
        double size = 0.02;
        if (Math.abs(du) > size) {
            return false;
        }
        // Shallow upward chevron - a common distant-bird silhouette.
        double expected = cy - Math.abs(du) * 0.7;
        return Math.abs(v - expected) < 0.006;
    }

    private static char blockForShade(double shade) {
        if (shade < 0.15) return '░';
        if (shade < 0.4) return '▒';
        if (shade < 0.7) return '▓';
        return '█';
    }

    private static int[] skyColor(double tt) {
        tt = Math.max(0.0, Math.min(1.0, tt));
        for (int i = 0; i < SKY_STOPS.length - 1; i++) {
            if (tt <= SKY_STOPS[i + 1]) {
                double localT = (tt - SKY_STOPS[i]) / (SKY_STOPS[i + 1] - SKY_STOPS[i]);
                return lerpColor(SKY_COLORS[i], SKY_COLORS[i + 1], localT);
            }
        }
        return SKY_COLORS[SKY_COLORS.length - 1];
    }

    private static int[] lerpColor(int[] a, int[] b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return new int[]{
            (int) lerp(a[0], b[0], t),
            (int) lerp(a[1], b[1], t),
            (int) lerp(a[2], b[2], t)
        };
    }

    private static double hashNoise(double u, double v) {
        double n = Math.sin(u * 127.1 + v * 311.7) * 43758.5453;
        return n - Math.floor(n);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static String ansi(int r, int g, int b) {
        return String.format("\u001B[38;2;%d;%d;%dm", clamp(r), clamp(g), clamp(b));
    }

    private static final class ScenePixel {
        final int r, g, b;
        final char glyph;

        ScenePixel(int r, int g, int b, char glyph) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.glyph = glyph;
        }
    }
}