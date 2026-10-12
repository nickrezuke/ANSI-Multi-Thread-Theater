import java.util.Random;

public class BagelLoader extends Loader {

    public enum Variant {
        POPPY_PLAIN,
        SESAME_STRAWBERRY
    }

    private static final StatusStage[] BAGEL_STAGES = {
            new StatusStage(20, "Proofing dough:"),
            new StatusStage(40, "Boiling in honey:"),
            new StatusStage(60, "Baking to golden:"),
            new StatusStage(80, "Slicing in half:"),
            new StatusStage(95, "Schmearing it on:"),
            new StatusStage(100, "Schmeared & Ready!")
    };

    private static final String LUMINANCE_CHARS = ":;=!*#$@▒▓█";

    private static final byte PAL_CRUST = 0;          // golden-brown baked crust
    private static final byte PAL_CRUST_DARK = 1;     // darker baked patches on the crust
    private static final byte PAL_SEED = 2;           // poppy / sesame seeds
    private static final byte PAL_CRUMB = 3;          // bare bagel interior on the cut face
    private static final byte PAL_CHEESE_THIN = 4;    // thin edges of the spread (crumb shows through)
    private static final byte PAL_CHEESE_MID = 5;
    private static final byte PAL_CHEESE_THICK = 6;   // thick, whipped middle of the spread
    private static final byte PAL_FRUIT = 7;          // strawberry bits (strawberry flavor only)
    private static final int PAL_COUNT = 8;

    private static final double TWO_PI = 2.0 * Math.PI;

    private static final double RING_RADIUS = 1.8;  // center of hole -> center of tube
    private static final double TUBE_RADIUS = 1.0;  // half-width of the tube (radial)
    private static final double TUBE_HEIGHT = 0.8;  // how tall the dome is (axial)
    private static final double CAMERA_DISTANCE = 7.0;

    // Tweak if the bagel clips the edge of the window (or looks too small in it)
    private static final double FIT = 1.0;

    // Dome sampling. THETA = around the hole, PHI = around the tube (swept 0-Pi)
    private static final double THETA_STEP = 0.025;
    private static final double PHI_STEP = 0.02;
    private static final int THETA_STEPS = (int) Math.ceil(TWO_PI / THETA_STEP);
    private static final int PHI_STEPS = (int) Math.ceil(Math.PI / PHI_STEP) + 1; // inclusive of Pi

    // Cut face sampling: radius from the inner hole edge to the outer rim, and theta
    private static final double FACE_THETA_STEP = 0.02;
    private static final double FACE_R_STEP = 0.025;
    private static final int FACE_THETA_STEPS = (int) Math.ceil(TWO_PI / FACE_THETA_STEP);
    private static final int FACE_R_STEPS = (int) Math.ceil(2.0 * TUBE_RADIUS / FACE_R_STEP) + 1;

    // ---- Look ------------------------------------------------------------------------
    private static final double Z_EPSILON = 0.0001;
    private static final double DOME_AMBIENT = 1.0;
    // The flat cut face gets lifted lighting so it reads as a solid surface, and then
    // the spread's own texture (ridges, thickness, crumb pores) nudges it up and down.
    private static final double FACE_BASE_LIGHT = 5.5;
    private static final double FACE_LIGHT_GAIN = 0.30;

    // Where cream cheese stops and bare bagel crumb starts, and the thin/mid/thick bands
    private static final double COVER_THRESHOLD = 0.40;
    private static final double COVER_THIN_MAX = 0.50;
    private static final double COVER_MID_MAX = 0.64;

    // ---- Animation -------------------------------------------------------------------
    private static final boolean FULL_TUMBLE = false;
    private static final double TILT_CENTER = 0.85;  // radians the cut face leans away from the viewer
    private static final double TILT_SWING = 0.35;   // +/- rocking around that

    // ---- Precomputed tables ----------------------------------------------------------
    private final double[] cosTheta = new double[THETA_STEPS];
    private final double[] sinTheta = new double[THETA_STEPS];
    private final double[] ringDist = new double[PHI_STEPS];     // distance from the axis at each phi
    private final double[] heightAt = new double[PHI_STEPS];     // height above the cut plane at each phi
    private final double[] normRadial = new double[PHI_STEPS];   // surface normal, radial part
    private final double[] normAxial = new double[PHI_STEPS];    // surface normal, axial part

    private final double[] faceCos = new double[FACE_THETA_STEPS];
    private final double[] faceSin = new double[FACE_THETA_STEPS];
    private final double[] faceRadius = new double[FACE_R_STEPS];

    // Palette slot for every sample on the dome / face, built once in initialize().
    // Indexed [row][theta] so the inner render loops walk memory in order.
    private final byte[][] domeTone = new byte[PHI_STEPS][THETA_STEPS];
    private final byte[][] faceTone = new byte[FACE_R_STEPS][FACE_THETA_STEPS];
    private final float[][] faceShade = new float[FACE_R_STEPS][FACE_THETA_STEPS];

    private String[][] cellCache;  // [palette slot][luminance index]

    // Perlin permutation table (reshuffled each run so every schmear is different)
    private final int[] perm = new int[512];

    private final Variant variant;

    // Screen mapping (computed from the window size so non-80x22 windows still work)
    private double screenCX, screenCY, scaleX, scaleY;

    private double A = 0;          // tilt
    private double B = 0;          // spin
    private double tiltPhase = 0;  // drives the tilt rocking

    public BagelLoader() {
        // Random flavor, 80x22 by default
        this(80, 22);
    }

    public BagelLoader(Variant variant) {
        // This uses 80x22 by default
        this(variant, 80, 22);
    }

    public BagelLoader(Variant variant, int w, int h) {
        super(BAGEL_STAGES, w, h);
        this.variant = variant;
    }

    public BagelLoader(int w, int h) {
        this(Variant.values()[(int) (Math.random() * Variant.values().length)], w, h);
    }

    private static String fg256(int code) {
        return "\u001B[38;5;" + code + "m";
    }

    @Override
    protected void initialize() {
        // ---- Trig tables (so we dont recompute these ~94,000 times per frame lmao)
        for (int t = 0; t < THETA_STEPS; t++) {
            cosTheta[t] = Math.cos(t * THETA_STEP);
            sinTheta[t] = Math.sin(t * THETA_STEP);
        }
        for (int p = 0; p < PHI_STEPS; p++) {
            double phi = Math.min(Math.PI, p * PHI_STEP); // half sweep, ending exactly on the cut
            double cp = Math.cos(phi), sp = Math.sin(phi);
            ringDist[p] = RING_RADIUS + TUBE_RADIUS * cp;
            heightAt[p] = TUBE_HEIGHT * sp;
            // Normal of an ellipse (radial semi-axis TUBE_RADIUS, axial semi-axis TUBE_HEIGHT)
            double nx = cp / TUBE_RADIUS;
            double nz = sp / TUBE_HEIGHT;
            double len = Math.sqrt(nx * nx + nz * nz);
            normRadial[p] = nx / len;
            normAxial[p] = nz / len;
        }
        double rInner = RING_RADIUS - TUBE_RADIUS;
        for (int t = 0; t < FACE_THETA_STEPS; t++) {
            faceCos[t] = Math.cos(t * FACE_THETA_STEP);
            faceSin[t] = Math.sin(t * FACE_THETA_STEP);
        }
        for (int r = 0; r < FACE_R_STEPS; r++) {
            faceRadius[r] = rInner + Math.min(2.0 * TUBE_RADIUS, r * FACE_R_STEP);
        }

        // ---- Flavor palette ----
        String[] palette = new String[PAL_COUNT];
        int seedCount;
        double seedMajor, seedMinor;

        switch (variant) {
            case SESAME_STRAWBERRY:
                palette[PAL_CRUST] = fg256(179);        // Light golden tan
                palette[PAL_CRUST_DARK] = fg256(137);   // Toasty brown patches
                palette[PAL_SEED] = fg256(230);         // Pale cream sesame
                palette[PAL_CRUMB] = fg256(180);        // Soft tan crumb
                palette[PAL_CHEESE_THIN] = fg256(181);  // Dusty pink where it's spread thin
                palette[PAL_CHEESE_MID] = fg256(218);   // Pastel strawberry pink
                palette[PAL_CHEESE_THICK] = fg256(224); // Whipped, pale pink
                palette[PAL_FRUIT] = fg256(167);        // Strawberry bits
                seedCount = 110;
                seedMajor = 0.22; // Sesame seeds: longer than wide, tilted every which way
                seedMinor = 0.11;
                break;

            case POPPY_PLAIN:
            default:
                palette[PAL_CRUST] = fg256(172);        // Golden brown
                palette[PAL_CRUST_DARK] = fg256(130);   // Deep baked brown
                palette[PAL_SEED] = fg256(60);          // Blue-grey poppy seeds
                palette[PAL_CRUMB] = fg256(180);        // Soft tan crumb
                palette[PAL_CHEESE_THIN] = fg256(187);  // Grey-cream where it's spread thin
                palette[PAL_CHEESE_MID] = fg256(230);   // Ivory
                palette[PAL_CHEESE_THICK] = fg256(231); // Bright white, whipped
                palette[PAL_FRUIT] = fg256(231);        // Unused for plain
                seedCount = 150;
                seedMajor = 0.12; // Poppy seeds: tiny and round
                seedMinor = 0.12;
                break;
        }

        // Build the color/char lookup cache for this run's flavor
        cellCache = new String[PAL_COUNT][LUMINANCE_CHARS.length()];
        for (int c = 0; c < PAL_COUNT; c++) {
            for (int ch = 0; ch < LUMINANCE_CHARS.length(); ch++) {
                cellCache[c][ch] = palette[c] + LUMINANCE_CHARS.charAt(ch) + RESET;
            }
        }

        // ---- Textures ----
        Random rng = new Random();
        seedNoise(rng);
        buildDome(rng, seedCount, seedMajor, seedMinor);
        buildFace(rng);

        // ---- Screen mapping (identical to the donut's 42/21 at 80x22) ----
        double unit = Math.min(window_height * 0.955, window_width * 0.2625) * FIT;
        scaleY = unit;
        scaleX = unit * 2.0; // terminal cells are roughly twice as tall as wide
        screenCX = window_width / 2.0;
        screenCY = window_height / 2.0;

        A = FULL_TUMBLE ? 0.0 : TILT_CENTER;
        B = 0.0;
        tiltPhase = 0.0;
    }

    // ===================================================================================
    // Texture generation
    // ===================================================================================

    private void buildDome(Random rng, int seedCount, double seedMajor, double seedMinor) {
        // Baked crust: golden brown with darker toasty patches, from low-frequency noise
        // sampled in the bagel's flat (u,v) plane so it wraps around with no seam.
        for (int p = 0; p < PHI_STEPS; p++) {
            for (int t = 0; t < THETA_STEPS; t++) {
                double u = ringDist[p] * cosTheta[t];
                double v = ringDist[p] * sinTheta[t];
                double n = fbm(u * 1.4, v * 1.4);
                domeTone[p][t] = (n < -0.12) ? PAL_CRUST_DARK : PAL_CRUST;
            }
        }

        // Scatter the seeds
        final double edgeMargin = 0.06;      // keep seeds off the very edge of the cut line
        final double hMax = RING_RADIUS + TUBE_RADIUS;
        for (int s = 0; s < seedCount; s++) {
            // Pick phi weighted by circumference so seeds spread evenly over the surface
            // (otherwise they'd bunch up on the small inner ring around the hole)
            double phi0;
            do {
                phi0 = edgeMargin + rng.nextDouble() * (Math.PI - 2.0 * edgeMargin);
            } while (rng.nextDouble() * hMax > RING_RADIUS + TUBE_RADIUS * Math.cos(phi0));

            int p0 = (int) Math.round(phi0 / PHI_STEP);
            int t0 = rng.nextInt(THETA_STEPS);
            double h0 = ringDist[p0];
            // Surface distance covered per radian of phi at this spot
            double arcSpeed = Math.hypot(TUBE_RADIUS * Math.sin(phi0), TUBE_HEIGHT * Math.cos(phi0));

            double angle = rng.nextDouble() * Math.PI;
            double ca = Math.cos(angle), sa = Math.sin(angle);
            int dtMax = (int) Math.ceil(seedMajor / (h0 * THETA_STEP));
            int dpMax = (int) Math.ceil(seedMajor / (arcSpeed * PHI_STEP));

            for (int dp = -dpMax; dp <= dpMax; dp++) {
                int p = p0 + dp;
                if (p < 0 || p >= PHI_STEPS) continue;
                double dv = dp * PHI_STEP * arcSpeed;
                for (int dt = -dtMax; dt <= dtMax; dt++) {
                    double du = dt * THETA_STEP * h0;
                    // Rotate into the seed's own axes, then test against its ellipse
                    double along = du * ca + dv * sa;
                    double across = -du * sa + dv * ca;
                    double e = (along * along) / (seedMajor * seedMajor)
                            + (across * across) / (seedMinor * seedMinor);
                    if (e <= 1.0) {
                        int t = ((t0 + dt) % THETA_STEPS + THETA_STEPS) % THETA_STEPS;
                        domeTone[p][t] = PAL_SEED;
                    }
                }
            }
        }
    }

    private void buildFace(Random rng) {
        final boolean strawberry = (variant == Variant.SESAME_STRAWBERRY);
        // Random offset into the noise field so every run gets a different schmear
        final double ox = rng.nextDouble() * 64.0;
        final double oy = rng.nextDouble() * 64.0;

        for (int r = 0; r < FACE_R_STEPS; r++) {
            final double rad = faceRadius[r];
            // -1 at the hole's edge, 0 mid-ring, +1 at the outer rim
            final double rel = (rad - RING_RADIUS) / TUBE_RADIUS;
            // 1.0 along the middle of the ring, falling to 0 at both edges: a person
            // spreading cream cheese leaves the very edges thinner (or bare)
            final double radial = 1.0 - rel * rel;

            for (int t = 0; t < FACE_THETA_STEPS; t++) {
                // Sample noise in the face's flat plane so the spread has no seam at theta = 0
                final double u = rad * faceCos[t] + ox;
                final double v = rad * faceSin[t] + oy;

                // How much cream cheese is here: a big lumpy splotch of Perlin noise,
                // biased toward the middle of the ring
                final double base = 0.5 + 0.5 * fbm(u * 0.9, v * 0.9);
                final double cover = 0.35 * radial + 0.65 * base;

                if (cover < COVER_THRESHOLD) {
                    // Bare bagel crumb, with little pores from higher frequency noise
                    faceTone[r][t] = PAL_CRUMB;
                    faceShade[r][t] = (float) (1.1 * perlin(u * 5.5, v * 5.5));
                    continue;
                }

                byte tone;
                if (cover < COVER_THIN_MAX) {
                    tone = PAL_CHEESE_THIN;
                } else if (cover < COVER_MID_MAX) {
                    tone = PAL_CHEESE_MID;
                } else {
                    tone = PAL_CHEESE_THICK;
                }

                // Knife swirl marks: wobbly concentric ridges (rings, warped by noise)
                final double warp = perlin(u * 1.2 + 9.0, v * 1.2 + 9.0);
                final double ridge = Math.sin(rad * 13.0 + 3.5 * warp);
                double shade = 0.7 * ridge + 1.6 * (cover - 0.62); // thicker = a bit brighter

                // Little chunks of strawberry in the strawberry cream cheese
                if (strawberry && perlin(u * 5.0 + 31.0, v * 5.0 + 17.0) > 0.36) {
                    tone = PAL_FRUIT;
                    shade -= 0.6;
                }

                faceTone[r][t] = tone;
                faceShade[r][t] = (float) shade;
            }
        }
    }

    // ===================================================================================
    // Perlin noise (classic gradient noise, 2D)
    // ===================================================================================

    private void seedNoise(Random rng) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        switch (hash & 7) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }

    // Roughly -1..1 (in practice mostly within +/-0.5)
    private double perlin(double x, double y) {
        double fx = Math.floor(x), fy = Math.floor(y);
        int xi = (int) fx & 255;
        int yi = (int) fy & 255;
        double xf = x - fx;
        double yf = y - fy;
        double u = fade(xf), v = fade(yf);

        int aa = perm[perm[xi] + yi];
        int ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi];
        int bb = perm[perm[xi + 1] + yi + 1];

        double x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
        double x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
        return lerp(x1, x2, v);
    }

    // Two octaves: a big lumpy shape plus finer detail on top
    private double fbm(double x, double y) {
        return 0.65 * perlin(x, y) + 0.35 * perlin(2.1 * x + 11.7, 2.1 * y + 3.3);
    }

    // ===================================================================================
    // Rendering
    // ===================================================================================

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        final double sinA = Math.sin(A), cosA = Math.cos(A);
        final double sinB = Math.sin(B), cosB = Math.cos(B);
        final int lumMax = LUMINANCE_CHARS.length() - 1;

        // ---- 1. The domed top half (phi swept 0..PI only) ----
        // Same math as the donut: tilt about the x-axis by A, then spin about the
        // view axis by B. The torus' own axis is the "w" direction, so w > 0 is the
        // dome and w = 0 is the flat cut plane.
        for (int p = 0; p < PHI_STEPS; p++) {
            final double h = ringDist[p];
            final double w = heightAt[p];
            final double nr = normRadial[p];
            final double nw = normAxial[p];
            final byte[] toneRow = domeTone[p];

            for (int t = 0; t < THETA_STEPS; t++) {
                final double cT = cosTheta[t];
                final double sT = sinTheta[t];
                final double u = h * cT;
                final double v = h * sT;

                final double vr = v * cosA - w * sinA;
                final double D = 1.0 / (CAMERA_DISTANCE + v * sinA + w * cosA);
                final double xs = screenCX + scaleX * D * (u * cosB - vr * sinB);
                final double ys = screenCY + scaleY * D * (u * sinB + vr * cosB);
                if (xs < 0 || ys < 0) continue;
                final int x = (int) xs;
                final int y = (int) ys;
                if (x >= window_width || y >= window_height) continue;

                final int o = x + window_width * y;
                if (D > zBuffer[o] + Z_EPSILON) {
                    zBuffer[o] = D;

                    // Rotate the surface normal the same way, then light it from
                    // above and in front (same light as the donut)
                    final double nvr = nr * sT * cosA - nw * sinA;
                    final double nwr = nr * sT * sinA + nw * cosA;
                    final double nyScreen = nr * cT * sinB + nvr * cosB;
                    int lum = (int) Math.round(8.0 * (-nyScreen - nwr) + DOME_AMBIENT);
                    if (lum < 0) lum = 0; else if (lum > lumMax) lum = lumMax;

                    outputBuffer[o] = cellCache[toneRow[t]][lum];
                }
            }
        }

        // ---- 2. The flat cut face (w = 0): a ring from hole edge to outer rim ----
        // Its normal points straight "down" the torus axis (toward the viewer when the
        // face is tilted toward us), so the whole face shares one base brightness that
        // changes as it spins under the light. Texture detail rides on top of that.
        final double faceLight = FACE_BASE_LIGHT + FACE_LIGHT_GAIN * 8.0 * (cosA - sinA * cosB);

        for (int r = 0; r < FACE_R_STEPS; r++) {
            final double rad = faceRadius[r];
            final byte[] toneRow = faceTone[r];
            final float[] shadeRow = faceShade[r];

            for (int t = 0; t < FACE_THETA_STEPS; t++) {
                final double u = rad * faceCos[t];
                final double v = rad * faceSin[t];

                final double vr = v * cosA;  // w = 0 for every point on the face
                final double D = 1.0 / (CAMERA_DISTANCE + v * sinA);
                final double xs = screenCX + scaleX * D * (u * cosB - vr * sinB);
                final double ys = screenCY + scaleY * D * (u * sinB + vr * cosB);
                if (xs < 0 || ys < 0) continue;
                final int x = (int) xs;
                final int y = (int) ys;
                if (x >= window_width || y >= window_height) continue;

                final int o = x + window_width * y;
                // Strictly-greater (with epsilon): where the face meets the dome's rim
                // the crust wins ties, which gives the cut face a nice crust outline
                if (D > zBuffer[o] + Z_EPSILON) {
                    zBuffer[o] = D;
                    int lum = (int) Math.round(faceLight + shadeRow[t]);
                    if (lum < 0) lum = 0; else if (lum > lumMax) lum = lumMax;

                    outputBuffer[o] = cellCache[toneRow[t]][lum];
                }
            }
        }

        // ---- 3. Advance the animation ----
        if (FULL_TUMBLE) {
            A += 0.04;
        } else {
            tiltPhase += 0.03;
            A = TILT_CENTER + TILT_SWING * Math.sin(tiltPhase);
        }
        B += 0.02;
    }
}