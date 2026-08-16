// TODO make this a little brighter to see details better (slower?)

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Renders an orb-weaver spider constructing a web, following the real
 * biological build sequence, in a fixed 3D world that a slowly orbiting
 * camera re-projects every frame (perspective divide + z-buffer, in the
 * style of Andy Sloan's donut.c). Nothing here is decorative: every thread
 * drawn corresponds to a specific, named step of the construction algorithm.
 *
 * Build sequence:
 *   1. Bridge thread   - the first line across two anchor points
 *   2. Y drop line      - a thread dropped from the bridge down to the hub
 *   3. Frame            - threads connecting the (irregular) anchor points
 *                          into a closed perimeter polygon
 *   4. Radii            - cast as rays from the hub; each length is found
 *                          by intersecting the ray with the frame polygon,
 *                          not assumed
 *   5. Capture spiral   - straight segments walked spoke-to-spoke at a
 *                          shrinking fraction of each spoke's own length
 */
public class SpiderWebLoader extends Loader {
    private static final StatusStage[] SIM_STAGES = {
        new StatusStage(25, "Deploying anchor state matrices:"),
        new StatusStage(50, "Extruding radial thread agents:"),
        new StatusStage(75, "Simulating sticky spiral loops:"),
        new StatusStage(100, "Automata Grid System Balanced!")
    };

    private final int width = 80;
    private final int height = 22;
    private final int totalSize = 1760;

    // ---- Camera / projection constants (tuned in Proto.java) ----
    // K2: distance of the world origin behind the camera.
    // Kx/Ky: independent screen-space scale factors; kept unequal both to
    //        fill the 80x22 buffer and to correct for terminal characters
    //        being roughly twice as tall as they are wide.
    private static final double K2_FAR = 30.0;  // establishing shot: whole web fits on screen
    private static final double K2_NEAR = 20.0; // pushed in for detail once the spiral gets busy
    private static final double ZOOM_SIGMOID_STEEPNESS = 10.0; // higher = snappier S-curve
    private static final double KX = 85.0;
    private static final double KY = 23.0;
    private static final double CAMERA_TILT = 0.45; // fixed rotation about X
    private static final double SPIN_SPEED = 0.006; // rotation about Y per tick

    // ---- Web geometry constants ----
    private static final int NUM_ANCHORS = 6;
    private static final int NUM_SPOKES = 16;
    private static final double BASE_RADIUS = 14.0;
    private static final double DEPTH_VARIANCE = 8.0;
    private static final int SPIRAL_TURNS = 6;
    private static final double SPIRAL_START_FRACTION = 0.92;
    private static final double SPIRAL_END_FRACTION = 0.08;

    // Total number of thread segments a web is planned to have, known
    // exactly up front from the constants above - used to turn "how much
    // is drawn so far" into a 0..1 construction-progress fraction that
    // drives the camera's zoom.
    private static final int TOTAL_SEGMENTS =
            2 /* bridge + Y-drop */ + NUM_ANCHORS /* frame */
            + NUM_SPOKES /* radii */ + NUM_SPOKES * SPIRAL_TURNS /* spiral */;

    // Thread type IDs (also used for coloring at render time)
    private static final int BRIDGE = 0;
    private static final int FRAME = 1;
    private static final int RADIAL = 2;
    private static final int SPIRAL = 3;

    private static final class Vec3 {
        final double x, y, z;
        Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }

    private static final class PendingSegment {
        final Vec3 start, end;
        final int type;
        PendingSegment(Vec3 start, Vec3 end, int type) {
            this.start = start; this.end = end; this.type = type;
        }
    }

    private static final class Segment {
        final Vec3 a, b;
        final int type;
        Segment(Vec3 a, Vec3 b, int type) { this.a = a; this.b = b; this.type = type; }
    }

    // NOTE: intentionally not initialized here either, same reason as below.
    private Random rand;
    // NOTE: intentionally not initialized here. Loader's constructor calls
    // initialize() before SpiderWebLoader's own field initializers run, so
    // these are created fresh inside initialize() instead - relying on
    // "= new ArrayList<>()" here would still be null on the very first call.
    private List<Segment> completed;
    private Deque<PendingSegment> queue;

    private Vec3[] anchors;
    private Vec3 hub;
    private Vec3[] spokeTips;

    private int phase; // 0 = bridge+frame, 1 = radii, 2 = spiral, 3 = resting
    private PendingSegment active;
    private double activeProgress;
    private Vec3 spiderPos;

    private double cameraSpin;
    private double cameraDistance = K2_FAR; // recomputed every frame from construction progress

    public SpiderWebLoader() {
        super(SIM_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
        if (rand == null) rand = new Random();
        completed = new ArrayList<>();
        queue = new ArrayDeque<>();
        active = null;
        activeProgress = 0.0;
        phase = 0;

        buildAnchors();
        hub = centroid(anchors);
        spiderPos = hub;

        // Step 1 + 2: bridge across two roughly-opposite anchors, then the
        // Y drop from the bridge's midpoint down to the hub.
        Vec3 bridgeA = anchors[0];
        Vec3 bridgeB = anchors[NUM_ANCHORS / 2];
        queue.addLast(new PendingSegment(bridgeA, bridgeB, BRIDGE));
        Vec3 bridgeMid = midpoint(bridgeA, bridgeB);
        queue.addLast(new PendingSegment(bridgeMid, hub, BRIDGE));

        // Step 3: frame perimeter, anchor to anchor, closing the polygon.
        for (int i = 0; i < NUM_ANCHORS; i++) {
            Vec3 a = anchors[i];
            Vec3 b = anchors[(i + 1) % NUM_ANCHORS];
            queue.addLast(new PendingSegment(a, b, FRAME));
        }
    }

    private void buildAnchors() {
        anchors = new Vec3[NUM_ANCHORS];
        for (int i = 0; i < NUM_ANCHORS; i++) {
            double theta = (i * 2.0 * Math.PI / NUM_ANCHORS) + (rand.nextDouble() - 0.5) * 0.4;
            double radius = BASE_RADIUS * (0.75 + rand.nextDouble() * 0.5);
            double x = Math.cos(theta) * radius;
            double y = Math.sin(theta) * radius;
            double z = (rand.nextDouble() - 0.5) * DEPTH_VARIANCE;
            anchors[i] = new Vec3(x, y, z);
        }
    }

    private static Vec3 centroid(Vec3[] pts) {
        double x = 0, y = 0, z = 0;
        for (Vec3 p : pts) { x += p.x; y += p.y; z += p.z; }
        int n = pts.length;
        return new Vec3(x / n, y / n, z / n);
    }

    private static Vec3 midpoint(Vec3 a, Vec3 b) {
        return new Vec3((a.x + b.x) / 2, (a.y + b.y) / 2, (a.z + b.z) / 2);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }

    /**
     * Casts a ray from the hub, in the plane's XY projection, at the given
     * angle, and returns the exact point where it crosses the frame
     * polygon's perimeter (z interpolated along whichever edge it hits).
     * This is what makes each radius a real geometric consequence of the
     * frame shape rather than an assumed fixed length.
     */
    private Vec3 castRayToFrame(double theta) {
        double dx = Math.cos(theta), dy = Math.sin(theta);
        for (int i = 0; i < NUM_ANCHORS; i++) {
            Vec3 a = anchors[i];
            Vec3 b = anchors[(i + 1) % NUM_ANCHORS];
            double v1x = hub.x - a.x, v1y = hub.y - a.y;
            double v2x = b.x - a.x, v2y = b.y - a.y;
            double v3x = -dy, v3y = dx;
            double denom = v2x * v3x + v2y * v3y;
            if (Math.abs(denom) < 1e-9) continue;
            double t = (v2x * v1y - v2y * v1x) / denom;
            double u = (v1x * v3x + v1y * v3y) / denom;
            if (t >= 0 && u >= 0 && u <= 1) {
                double z = a.z + (b.z - a.z) * u;
                return new Vec3(hub.x + dx * t, hub.y + dy * t, z);
            }
        }
        // Should not happen for a star-convex polygon covering all angles;
        // fall back to the average anchor radius so rendering never breaks.
        return new Vec3(hub.x + dx * BASE_RADIUS, hub.y + dy * BASE_RADIUS, hub.z);
    }

    private void advanceConstruction() {
        if (active == null) {
            if (!queue.isEmpty()) {
                active = queue.pollFirst();
                activeProgress = 0.0;
            } else {
                advancePhase();
                return;
            }
        }

        activeProgress += 0.10;
        if (activeProgress >= 1.0) {
            completed.add(new Segment(active.start, active.end, active.type));
            spiderPos = active.end;
            active = null;
        } else {
            spiderPos = lerp(active.start, active.end, activeProgress);
        }
    }

    private void advancePhase() {
        if (phase == 0) {
            // Frame complete: cast the radii.
            spokeTips = new Vec3[NUM_SPOKES];
            for (int s = 0; s < NUM_SPOKES; s++) {
                double theta = s * 2.0 * Math.PI / NUM_SPOKES;
                spokeTips[s] = castRayToFrame(theta);
                queue.addLast(new PendingSegment(hub, spokeTips[s], RADIAL));
            }
            phase = 1;
        } else if (phase == 1) {
            // Radii complete: walk the capture spiral spoke-to-spoke,
            // each vertex placed at a shrinking fraction of that spoke's
            // own length so the spiral naturally follows the frame shape.
            int totalSteps = NUM_SPOKES * SPIRAL_TURNS;
            Vec3 prevPoint = null;
            for (int k = 0; k <= totalSteps; k++) {
                int spokeIdx = k % NUM_SPOKES;
                double t = (double) k / totalSteps;
                double fraction = SPIRAL_START_FRACTION
                        - t * (SPIRAL_START_FRACTION - SPIRAL_END_FRACTION);
                Vec3 tip = spokeTips[spokeIdx];
                Vec3 point = lerp(hub, tip, fraction);
                if (prevPoint != null) {
                    queue.addLast(new PendingSegment(prevPoint, point, SPIRAL));
                }
                prevPoint = point;
            }
            phase = 2;
        } else {
            // Web complete: hold, then occasionally rebuild with a fresh
            // (randomly shaped) anchor layout, like a spider starting over.
            phase = 3;
        }
    }

    /** Fraction of the web's total planned thread segments completed so far, in [0,1]. */
    private double constructionProgress() {
        if (phase == 3) return 1.0; // web finished: hold the close-up
        double done = completed.size() + (active != null ? activeProgress : 0.0);
        return clamp(done / TOTAL_SEGMENTS, 0.0, 1.0);
    }

    /**
     * Logistic sigmoid normalized so f(0)=0 and f(1)=1 exactly (a raw
     * logistic never quite reaches its asymptotes), giving a true S-curve
     * ease instead of the ease-in-out polynomials (smoothstep etc.) that
     * usually stand in for "sigmoid" in graphics code.
     */
    private static double normalizedSigmoid(double x, double steepness) {
        double raw = 1.0 / (1.0 + Math.exp(-steepness * (x - 0.5)));
        double at0 = 1.0 / (1.0 + Math.exp(steepness * 0.5));
        double at1 = 1.0 / (1.0 + Math.exp(-steepness * 0.5));
        return (raw - at0) / (at1 - at0);
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // --- STEP 1: ADVANCE THE CONSTRUCTION ALGORITHM ---
        for (int cycle = 0; cycle < 3; cycle++) {
            if (phase == 3) {
                if (rand.nextDouble() < 0.004) initialize();
            } else {
                advanceConstruction();
            }
        }
        cameraSpin += SPIN_SPEED;

        // Dolly the camera in as the web fills out: distance eases from
        // K2_FAR to K2_NEAR along a sigmoid of construction progress, not
        // wall-clock ticks, so the zoom actually tracks what's on screen.
        double zoomT = normalizedSigmoid(constructionProgress(), ZOOM_SIGMOID_STEEPNESS);
        cameraDistance = K2_FAR + (K2_NEAR - K2_FAR) * zoomT;

        // --- STEP 2: CLEAR BUFFERS ---
        for (int i = 0; i < totalSize; i++) {
            outputBuffer[i] = " ";
            zBuffer[i] = -1.0;
        }

        // --- STEP 3: RE-PROJECT EVERY COMPLETED THREAD THROUGH THE ROTATING CAMERA ---
        for (Segment seg : completed) {
            drawProjectedLine(outputBuffer, zBuffer, seg.a, seg.b, seg.type);
        }
        // The thread currently being spun, drawn only up to the spider's
        // current position so construction is visibly in progress.
        if (active != null) {
            drawProjectedLine(outputBuffer, zBuffer, active.start, spiderPos, active.type);
        }

        // --- STEP 4: PROJECT AND DRAW THE SPIDER ITSELF ---
        int[] sp = project(spiderPos);
        if (sp != null) {
            int idx = sp[0] + sp[1] * width;
            outputBuffer[idx] = "\u001B[38;2;255;50;50m\u2588\u001B[0m";
            zBuffer[idx] = 1e9; // always drawn on top
        }
    }

    /** Rotates a world point by the fixed tilt + current turntable spin, then applies perspective divide. */
    private int[] project(Vec3 p) {
        double y1 = p.y * Math.cos(CAMERA_TILT) - p.z * Math.sin(CAMERA_TILT);
        double z1 = p.y * Math.sin(CAMERA_TILT) + p.z * Math.cos(CAMERA_TILT);
        double x1 = p.x;

        double x2 = x1 * Math.cos(cameraSpin) + z1 * Math.sin(cameraSpin);
        double z2 = -x1 * Math.sin(cameraSpin) + z1 * Math.cos(cameraSpin);
        double y2 = y1;

        double zCam = z2 + cameraDistance;
        if (zCam < 1.0) return null; // behind the camera; shouldn't happen at this scale
        double ooz = 1.0 / zCam;

        int sx = (int) Math.round(width / 2.0 + KX * ooz * x2);
        int sy = (int) Math.round(height / 2.0 - KY * ooz * y2);
        if (sx < 0 || sx >= width || sy < 0 || sy >= height) return null;
        return new int[] { sx, sy, (int) (ooz * 1_000_000) };
    }

    /** Samples a 3D segment densely enough to stay pixel-continuous after projection, z-buffering as it goes. */
    private void drawProjectedLine(String[] outputBuffer, double[] zBuffer, Vec3 a, Vec3 b, int type) {
        double worldLen = Math.sqrt(
                (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y) + (b.z - a.z) * (b.z - a.z));
        int steps = Math.max(1, (int) (worldLen * 3));

        int prevSx = Integer.MIN_VALUE, prevSy = Integer.MIN_VALUE;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = lerp(a, b, t);
            double y1 = p.y * Math.cos(CAMERA_TILT) - p.z * Math.sin(CAMERA_TILT);
            double z1 = p.y * Math.sin(CAMERA_TILT) + p.z * Math.cos(CAMERA_TILT);
            double x1 = p.x;
            double x2 = x1 * Math.cos(cameraSpin) + z1 * Math.sin(cameraSpin);
            double z2 = -x1 * Math.sin(cameraSpin) + z1 * Math.cos(cameraSpin);
            double y2 = y1;
            double zCam = z2 + cameraDistance;
            if (zCam < 1.0) continue;
            double ooz = 1.0 / zCam;
            int sx = (int) Math.round(width / 2.0 + KX * ooz * x2);
            int sy = (int) Math.round(height / 2.0 - KY * ooz * y2);
            if (sx < 0 || sx >= width || sy < 0 || sy >= height) continue;

            // Skip only-repeated pixels so we don't waste z-buffer writes,
            // but always draw the first sample of the line.
            if (sx == prevSx && sy == prevSy) continue;
            prevSx = sx; prevSy = sy;

            int idx = sx + sy * width;
            if (ooz > zBuffer[idx]) {
                zBuffer[idx] = ooz;
                outputBuffer[idx] = colorFor(type, ooz);
            }
        }
    }

    private String colorFor(int type, double ooz) {
        // Brightness falls off with distance from the camera, the same
        // depth cue donut.c gets from its luminance ramp. Recomputed off
        // the current (possibly zoomed-in) camera distance so the shading
        // stays correctly calibrated as the dolly moves.
        double near = 1.0 / (cameraDistance - 20.0);
        double far = 1.0 / (cameraDistance + 20.0);
        double depthT = clamp((ooz - far) / (near - far), 0.15, 1.0);

        int r, g, b;
        char glyph;
        switch (type) {
            case BRIDGE:
                r = 230; g = 200; b = 120; glyph = '=';
                break;
            case FRAME:
                r = 150; g = 155; b = 160; glyph = '#';
                break;
            case RADIAL:
                r = 180; g = 185; b = 190; glyph = '.';
                break;
            default: // SPIRAL
                r = 110; g = 210; b = 235; glyph = '-';
                break;
        }
        r = (int) (r * depthT);
        g = (int) (g * depthT);
        b = (int) (b * depthT);
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m" + glyph + "\u001B[0m";
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}