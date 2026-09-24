// TODO: Fix this its a little jankey and make sure the half blocks are rendering correctly

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.stream.Stream;

/**
 * A CHIP-8 interpreter that plays ROM files on the terminal "view screen".
 *
 * ROMs are NOT included: drop your own .ch8 files into a "Chip8ROMs" folder next to the
 * program (or straight into the working directory). With no ROM available, a tiny
 * built-in demo runs instead so the loader still has something to show.
 * Visit: https://johnearnest.github.io/chip8Archive/ 
 *
 * Everything here is written from the public CHIP-8 specification; no third-party code.
 *
 * Display : the 64x32 CHIP-8 screen is drawn with half-block glyphs (two "pixels" per
 *           terminal cell), integer-scaled to fit the canvas and centered, with a small
 *           phosphor-style afterglow to tame CHIP-8's notorious XOR sprite flicker.
 * Keypad  : 1234 / QWER / ASDF / ZXCV  ->  the hex keypad (1 2 3 C / 4 5 6 D / 7 8 9 E / A 0 B F).
 *           Arrows also press 2/8/4/6 and Space presses 5, for games that use them.
 * Extras  : [ ] previous/next ROM   - = slower/faster   P pause   Tab quirk profile   Enter reset
 *
 * Terminals only report key PRESSES (plus OS auto-repeat), never releases, so releases are
 * emulated with timeouts (see the HOLD_* constants). Terminals also only auto-repeat the
 * most recently pressed key, so holding two keys at once can't be sustained.
 *
 * Plain CHIP-8 only (64x32): SUPER-CHIP / XO-CHIP ROMs that need 128x64 won't display properly.
 */
public class Chip8Loader extends InteractiveLoader {

    private static final StatusStage[] STAGES = { new StatusStage(100, "[Keys: 1234 QWER ASDF ZXCV]") };

    private static final String DEFAULT_ROM_DIR = "Chip8ROMs";
    private static final String[] ROM_EXTENSIONS = { ".ch8", ".c8", ".chip8", ".rom" };

    private static final int DEFAULT_IPS = 700; // CHIP-8 instructions per second
    private static final int MIN_IPS = 100;
    private static final int MAX_IPS = 5000;
    private static final int IPS_STEP = 100;

    private static final long TICK_NANOS = 1_000_000_000L / 60;    // CHIP-8 timers run at 60 Hz (regardless of our own fps)
    private static final long MAX_CATCHUP_NANOS = 100_000_000L;    // never try to catch up more than 100 ms
    private static final int MAX_TICKS_PER_FRAME = 6;

    // Key release emulation. A single keypress event is held down for HOLD_TAP; once the OS
    // auto-repeat kicks in (a second event for the same key within REPEAT_WINDOW) each event
    // only needs to bridge the short gap to the next repeat, so HOLD_REPEAT is shorter.
    // Raise HOLD_TAP if taps feel too short, lower it if they feel too sticky.
    private static final long HOLD_TAP_NANOS = 140_000_000L;
    private static final long HOLD_REPEAT_NANOS = 90_000_000L;
    private static final long REPEAT_WINDOW_NANOS = 1_000_000_000L;

    private static final boolean PHOSPHOR_FADE = true;  // afterglow on/off
    private static final boolean TRUECOLOR = !"false".equalsIgnoreCase(System.getProperty("chip8.truecolor"));

    // ------------------------------------------------------------------ palette / glyph caches
    private static final int LEVELS = 6; // 0 = fully off ... 5 = fully lit
    private static final int[] COLOR_OFF = { 8, 16, 12 };
    private static final int[] COLOR_ON = { 110, 255, 140 };
    private static final int[] COLOR_INFO = { 120, 140, 125 };

    private static final String GLYPH = "\u2580"; // upper half block: fg = top pixel, bg = bottom pixel
    private static final String GLYPH_RESET = GLYPH + RESET;
    private static final String RESET_SPACE = RESET + " ";
    private static final String INFO_SGR;
    private static final String[] COMBO = new String[LEVELS * LEVELS];       // colour escape + glyph
    private static final String[] COMBO_RESET = new String[LEVELS * LEVELS]; // ... + reset (last cell in a row)

    static {
        for (int top = 0; top < LEVELS; top++) {
            for (int bottom = 0; bottom < LEVELS; bottom++) {
                String s = sgr(38, level(top)) + sgr(48, level(bottom)) + GLYPH;
                COMBO[top * LEVELS + bottom] = s;
                COMBO_RESET[top * LEVELS + bottom] = s + RESET;
            }
        }
        INFO_SGR = sgr(38, COLOR_INFO);
    }

    private static int[] level(int l) {
        int[] rgb = new int[3];
        for (int c = 0; c < 3; c++) {
            rgb[c] = COLOR_OFF[c] + (COLOR_ON[c] - COLOR_OFF[c]) * l / (LEVELS - 1);
        }
        return rgb;
    }

    private static String sgr(int layer, int[] rgb) {
        if (TRUECOLOR) {
            return "\u001B[" + layer + ";2;" + rgb[0] + ";" + rgb[1] + ";" + rgb[2] + "m";
        }
        int idx = 16 + 36 * cube(rgb[0]) + 6 * cube(rgb[1]) + cube(rgb[2]);
        return "\u001B[" + layer + ";5;" + idx + "m";
    }

    private static int cube(int c) {
        return Math.round(c * 5f / 255f);
    }

    // ------------------------------------------------------------------ built-in demo ROM (original)
    // A bouncing ball. Used when no ROM files are found.
    private static final int[] DEMO_ROM = {
            0x60, 0x20,        // 200: V0 = 32  (x)
            0x61, 0x0A,        // 202: V1 = 10  (y)
            0x62, 0x02,        // 204: V2 = 2   (dx)
            0x63, 0x01,        // 206: V3 = 1   (dy)
            0xA2, 0x38,        // 208: I = 0x238 (sprite)
            0xD0, 0x14,        // 20A: draw
            0x64, 0x01,        // 20C: loop: V4 = 1
            0xF4, 0x15,        // 20E: delay = V4
            0xF4, 0x07,        // 210: V4 = delay
            0x34, 0x00,        // 212: skip if V4 == 0
            0x12, 0x10,        // 214: jump 210
            0xD0, 0x14,        // 216: erase
            0x80, 0x24,        // 218: V0 += V2
            0x81, 0x34,        // 21A: V1 += V3
            0x30, 0x3C,        // 21C: skip if V0 == 60
            0x12, 0x22,        // 21E: jump 222
            0x62, 0xFE,        // 220: dx = -2
            0x30, 0x00,        // 222: skip if V0 == 0
            0x12, 0x28,        // 224: jump 228
            0x62, 0x02,        // 226: dx = 2
            0x31, 0x1C,        // 228: skip if V1 == 28
            0x12, 0x2E,        // 22A: jump 22E
            0x63, 0xFF,        // 22C: dy = -1
            0x31, 0x00,        // 22E: skip if V1 == 0
            0x12, 0x34,        // 230: jump 234
            0x63, 0x01,        // 232: dy = 1
            0xD0, 0x14,        // 234: draw
            0x12, 0x0C,        // 236: jump 20C
            0x60, 0xF0, 0xF0, 0x60 // 238: sprite (4x4 ball)
    };

    // ------------------------------------------------------------------ loader state
    private static final int CMD_PAUSE = 1, CMD_RESET = 2, CMD_NEXT = 3, CMD_PREV = 4,
            CMD_FASTER = 5, CMD_SLOWER = 6, CMD_QUIRKS = 7;

    private final String romSource; // file or directory given by the caller (null = default lookup)
    private final Keypad keypad;
    private final Chip8Core core;
    private final List<Path> roms = new ArrayList<>();
    private int romIndex = -1;
    private String romName = "";
    private String loadError = null;

    private int ips = DEFAULT_IPS;
    private boolean vipQuirks = true;
    private boolean paused = false;
    private boolean soundOn = false;

    private long lastNanos = 0;
    private long tickAccum = 0;
    private double cycleDebt = 0;

    private final byte[] glow = new byte[Chip8Core.WIDTH * Chip8Core.HEIGHT];

    // Input side (written by the input thread, read by the render thread)
    private final AtomicLongArray keyExpiry = new AtomicLongArray(16);
    private final AtomicLongArray keyLastEvent = new AtomicLongArray(16);
    private volatile int pressSerial = 0;
    private volatile int lastPressedKey = -1;
    private final ConcurrentLinkedQueue<Integer> commands = new ConcurrentLinkedQueue<>();

    // Layout (computed once from the canvas size)
    private int usableRows;
    private int[] colToSrc;
    private int[] halfRowToSrc;

    private String[] infoCells;
    private boolean infoDirty = true;

    public Chip8Loader() {
        this(null);
    }

    /** @param romPathOrDir a single ROM file, or a folder of ROMs; null uses ./Chip8ROMs (or the working directory) */
    public Chip8Loader(String romPathOrDir) {
        super(STAGES);
        this.romSource = romPathOrDir;
        this.keypad = new Keypad() {
            @Override
            public boolean isDown(int key) {
                return System.nanoTime() < keyExpiry.get(key & 0xF);
            }

            @Override
            public int pressSerial() {
                return pressSerial;
            }

            @Override
            public int lastPressed() {
                return lastPressedKey;
            }
        };
        this.core = new Chip8Core(keypad);
        setTargetFps(60);
        buildLayout();
    }

    // ------------------------------------------------------------------ Loader / InteractiveLoader hooks
    @Override
    protected boolean useDistinctArrowCodes() {
        return true; // so typed letters and arrow keys can't be confused
    }

    @Override
    protected void onInitialize() {
        discoverRoms();
        int start = 0;
        if (!roms.isEmpty()) {
            start = pickStartIndex();
        }
        loadRom(start);
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        switch (keyCode) {
            case KEY_UP: pressKey(0x2); return;
            case KEY_DOWN: pressKey(0x8); return;
            case KEY_LEFT: pressKey(0x4); return;
            case KEY_RIGHT: pressKey(0x6); return;
            case ' ': pressKey(0x5); return;
            case '\r':
            case '\n': commands.add(CMD_RESET); return;
            case '\t': commands.add(CMD_QUIRKS); return;
            case '[': commands.add(CMD_PREV); return;
            case ']': commands.add(CMD_NEXT); return;
            case '-':
            case '_': commands.add(CMD_SLOWER); return;
            case '=':
            case '+': commands.add(CMD_FASTER); return;
            case 'p':
            case 'P': commands.add(CMD_PAUSE); return;
            default: break;
        }
        int hex = hexForKey(keyCode);
        if (hex >= 0) {
            pressKey(hex);
        }
    }

    // Standard mapping of the COSMAC VIP hex keypad onto the left of a QWERTY keyboard:
    //   1 2 3 C        1 2 3 4
    //   4 5 6 D   ->   Q W E R
    //   7 8 9 E        A S D F
    //   A 0 B F        Z X C V
    private static int hexForKey(int c) {
        if (c < 0 || c > 0xFF) {
            return -1;
        }
        switch (Character.toLowerCase((char) c)) {
            case '1': return 0x1;
            case '2': return 0x2;
            case '3': return 0x3;
            case '4': return 0xC;
            case 'q': return 0x4;
            case 'w': return 0x5;
            case 'e': return 0x6;
            case 'r': return 0xD;
            case 'a': return 0x7;
            case 's': return 0x8;
            case 'd': return 0x9;
            case 'f': return 0xE;
            case 'z': return 0xA;
            case 'x': return 0x0;
            case 'c': return 0xB;
            case 'v': return 0xF;
            default: return -1;
        }
    }

    // Runs on the input thread. Emulates "key held" with an expiry deadline.
    private void pressKey(int hex) {
        long now = System.nanoTime();
        long previousEvent = keyLastEvent.get(hex);
        boolean repeating = previousEvent != 0 && (now - previousEvent) < REPEAT_WINDOW_NANOS;
        keyLastEvent.set(hex, now);
        long deadline = now + (repeating ? HOLD_REPEAT_NANOS : HOLD_TAP_NANOS);
        keyExpiry.set(hex, Math.max(keyExpiry.get(hex), deadline));
        lastPressedKey = hex;
        pressSerial++; // only the input thread writes this
    }

    // ------------------------------------------------------------------ ROM discovery / loading
    private void discoverRoms() {
        roms.clear();
        try {
            Path source = Paths.get(romSource != null ? romSource : DEFAULT_ROM_DIR);
            if (Files.isRegularFile(source)) {
                roms.add(source);
            } else if (Files.isDirectory(source)) {
                scan(source);
            } else if (romSource == null) {
                scan(Paths.get(".")); // ROMs dropped right next to the program
            }
        } catch (IOException | RuntimeException ignored) {
            // No readable ROM location: fall through to the built-in demo
        }
        roms.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
    }

    private void scan(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).filter(Chip8Loader::hasRomExtension).forEach(roms::add);
        }
    }

    private static boolean hasRomExtension(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String ext : ROM_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    // -Dchip8.rom=<path or part of a file name> picks the starting ROM; otherwise a random one.
    private int pickStartIndex() {
        String want = System.getProperty("chip8.rom");
        if (want != null && !want.isEmpty()) {
            try {
                Path direct = Paths.get(want);
                if (Files.isRegularFile(direct)) {
                    roms.add(0, direct);
                    return 0;
                }
            } catch (RuntimeException ignored) {
                // not a usable path; try it as a name fragment below
            }
            String needle = want.toLowerCase(Locale.ROOT);
            for (int i = 0; i < roms.size(); i++) {
                if (roms.get(i).getFileName().toString().toLowerCase(Locale.ROOT).contains(needle)) {
                    return i;
                }
            }
        }
        return new Random().nextInt(roms.size());
    }

    private void loadRom(int index) {
        loadError = null;
        core.applyProfile(vipQuirks);

        if (roms.isEmpty()) {
            startDemo();
            return;
        }

        romIndex = Math.floorMod(index, roms.size());
        Path path = roms.get(romIndex);
        String fileName = path.getFileName().toString();
        try {
            byte[] data = Files.readAllBytes(path);
            if (data.length == 0) {
                loadError = "Empty ROM";
            } else if (!core.load(data)) {
                loadError = "ROM too large";
            }
        } catch (IOException | RuntimeException e) {
            loadError = "Can't read ROM";
        }

        if (loadError != null) {
            loadError = fileName + ": " + loadError;
            startDemo();
            return;
        }
        int dot = fileName.lastIndexOf('.');
        romName = dot > 0 ? fileName.substring(0, dot) : fileName;
        resetRunState();
    }

    private void startDemo() {
        byte[] demo = new byte[DEMO_ROM.length];
        for (int i = 0; i < demo.length; i++) {
            demo[i] = (byte) DEMO_ROM[i];
        }
        core.load(demo);
        romName = roms.isEmpty() ? "demo (no ROMs: drop .ch8 files in ./" + DEFAULT_ROM_DIR + ")" : "demo";
        resetRunState();
    }

    private void resetRunState() {
        Arrays.fill(glow, (byte) 0);
        lastNanos = 0;
        tickAccum = 0;
        cycleDebt = 0;
        infoDirty = true;
    }

    private void processCommands() {
        Integer cmd;
        while ((cmd = commands.poll()) != null) {
            switch (cmd) {
                case CMD_PAUSE:
                    paused = !paused;
                    infoDirty = true;
                    break;
                case CMD_RESET:
                    loadRom(romIndex);
                    break;
                case CMD_NEXT:
                    loadRom(romIndex + 1);
                    break;
                case CMD_PREV:
                    loadRom(romIndex - 1);
                    break;
                case CMD_FASTER:
                    ips = Math.min(MAX_IPS, ips + IPS_STEP);
                    infoDirty = true;
                    break;
                case CMD_SLOWER:
                    ips = Math.max(MIN_IPS, ips - IPS_STEP);
                    infoDirty = true;
                    break;
                case CMD_QUIRKS:
                    vipQuirks = !vipQuirks;
                    loadRom(romIndex);
                    break;
                default:
                    break;
            }
        }
    }

    // ------------------------------------------------------------------ layout
    private void buildLayout() {
        usableRows = Math.max(1, window_height - (window_height >= 2 ? 1 : 0)); // last row = status line
        double scaleX = window_width / (double) Chip8Core.WIDTH;
        double scaleY = (usableRows * 2) / (double) Chip8Core.HEIGHT; // two half-rows per cell row
        double scale = Math.min(scaleX, scaleY);
        if (scale >= 1.0) {
            scale = Math.floor(scale); // whole-number scaling keeps pixels even
        }
        int displayCols = Math.max(1, (int) Math.floor(Chip8Core.WIDTH * scale + 1e-9));
        int displayHalfRows = Math.max(1, (int) Math.floor(Chip8Core.HEIGHT * scale + 1e-9));
        int offsetX = Math.max(0, (window_width - displayCols) / 2);
        int offsetY = Math.max(0, (usableRows * 2 - displayHalfRows) / 2);

        colToSrc = new int[window_width];
        Arrays.fill(colToSrc, -1);
        for (int c = 0; c < displayCols && offsetX + c < window_width; c++) {
            colToSrc[offsetX + c] = Math.min(Chip8Core.WIDTH - 1, (int) (c / scale));
        }

        halfRowToSrc = new int[usableRows * 2];
        Arrays.fill(halfRowToSrc, -1);
        for (int r = 0; r < displayHalfRows && offsetY + r < halfRowToSrc.length; r++) {
            halfRowToSrc[offsetY + r] = Math.min(Chip8Core.HEIGHT - 1, (int) (r / scale));
        }
    }

    private void rebuildInfo() {
        StringBuilder sb = new StringBuilder(" ");
        if (loadError != null) {
            sb.append(loadError).append(" | ");
        }
        sb.append(romName).append("  ").append(ips).append(" ips  ").append(vipQuirks ? "VIP" : "MODERN");
        if (paused) {
            sb.append("  PAUSED");
        }
        if (soundOn) {
            sb.append("  \u266A");
        }
        sb.append("  |  [ ] rom  - = speed  P pause  Tab quirks  Enter reset");

        infoCells = new String[window_width];
        for (int i = 0; i < window_width; i++) {
            infoCells[i] = String.valueOf(i < sb.length() ? sb.charAt(i) : ' ');
        }
        if (window_width > 0) {
            infoCells[0] = INFO_SGR + infoCells[0];
            infoCells[window_width - 1] = infoCells[window_width - 1] + RESET;
        }
        infoDirty = false;
    }

    // ------------------------------------------------------------------ frame
    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        processCommands();

        long now = System.nanoTime();
        if (paused || lastNanos == 0) {
            lastNanos = now;
        } else {
            tickAccum += Math.min(now - lastNanos, MAX_CATCHUP_NANOS);
            lastNanos = now;
            int ticks = 0;
            while (tickAccum >= TICK_NANOS && ticks < MAX_TICKS_PER_FRAME) {
                tickAccum -= TICK_NANOS;
                cycleDebt += ips / 60.0;
                int cycles = (int) cycleDebt;
                cycleDebt -= cycles;
                core.runTick(cycles);
                ticks++;
            }
        }

        boolean soundNow = core.soundActive();
        if (soundNow != soundOn) {
            soundOn = soundNow;
            infoDirty = true;
        }

        updateGlow();
        drawScreen(outputBuffer);

        if (window_height >= 2) {
            if (infoDirty || infoCells == null) {
                rebuildInfo();
            }
            System.arraycopy(infoCells, 0, outputBuffer, (window_height - 1) * window_width, window_width);
        }
    }

    private void updateGlow() {
        boolean[] pixels = core.display;
        final byte max = (byte) (LEVELS - 1);
        for (int i = 0; i < glow.length; i++) {
            if (pixels[i]) {
                glow[i] = max;
            } else if (!PHOSPHOR_FADE) {
                glow[i] = 0;
            } else if (glow[i] > 0) {
                glow[i]--;
            }
        }
    }

    // Two CHIP-8 pixels per terminal cell: foreground colour = top pixel, background = bottom
    // pixel, glyph = upper half block. Colour escapes are only emitted when a cell's colour pair
    // differs from the one before it, and every row ends with a reset so nothing bleeds into the
    // newline or the status footer.
    private void drawScreen(String[] out) {
        final int width = window_width;
        for (int row = 0; row < usableRows; row++) {
            int topSrcY = halfRowToSrc[row * 2];
            int bottomSrcY = halfRowToSrc[row * 2 + 1];
            int base = row * width;
            int previous = -1; // -1 = terminal is in its default colours

            for (int col = 0; col < width; col++) {
                int srcX = colToSrc[col];
                boolean last = col == width - 1;
                String cell;

                if (srcX < 0 || (topSrcY < 0 && bottomSrcY < 0)) {
                    cell = previous != -1 ? RESET_SPACE : " ";
                    previous = -1;
                } else {
                    int top = topSrcY < 0 ? 0 : glow[topSrcY * Chip8Core.WIDTH + srcX];
                    int bottom = bottomSrcY < 0 ? 0 : glow[bottomSrcY * Chip8Core.WIDTH + srcX];
                    int combo = top * LEVELS + bottom;
                    if (combo == previous) {
                        cell = last ? GLYPH_RESET : GLYPH;
                    } else {
                        cell = last ? COMBO_RESET[combo] : COMBO[combo];
                    }
                    previous = combo;
                }
                out[base + col] = cell;
            }
        }
    }

    // ==================================================================
    //  CHIP-8 machine
    // ==================================================================

    /** What the machine needs to know about the keyboard. */
    interface Keypad {
        boolean isDown(int key);

        int pressSerial();

        int lastPressed();
    }

    static final class Chip8Core {
        static final int WIDTH = 64;
        static final int HEIGHT = 32;
        static final int MEMORY_SIZE = 4096;
        static final int PROGRAM_START = 0x200;
        static final int FONT_START = 0x50;

        private static final int[] FONT = {
                0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                0x20, 0x60, 0x20, 0x20, 0x70, // 1
                0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        };

        final byte[] memory = new byte[MEMORY_SIZE];
        final int[] v = new int[16];
        final int[] stack = new int[16];
        final boolean[] display = new boolean[WIDTH * HEIGHT];
        int i;
        int pc;
        int sp;
        int delayTimer;
        int soundTimer;

        // Behaviour differences between CHIP-8 implementations ("quirks")
        boolean shiftUsesVy;         // 8XY6/8XYE shift VY into VX (VIP) instead of shifting VX in place
        boolean loadStoreIncrementsI; // FX55/FX65 leave I advanced (VIP) instead of unchanged
        boolean vfReset;             // 8XY1/8XY2/8XY3 clear VF (VIP)
        boolean clipSprites;         // sprites clip at the screen edge (VIP) instead of wrapping
        boolean displayWait;         // at most one DXYN per 60 Hz tick (VIP)

        private boolean vblankHit;
        private int waitState;       // 0 = running, 1 = FX0A waiting for a press, 2 = waiting for its release
        private int waitReg;
        private int waitKey;
        private int waitSerial;

        private final Keypad keypad;
        private final Random rng = new Random();

        Chip8Core(Keypad keypad) {
            this.keypad = keypad;
        }

        /** VIP = original COSMAC VIP behaviour; otherwise the "modern" (SUPER-CHIP / Octo-style) behaviour. */
        void applyProfile(boolean vip) {
            shiftUsesVy = vip;
            loadStoreIncrementsI = vip;
            vfReset = vip;
            clipSprites = vip;
            displayWait = vip;
        }

        /** Resets the machine and loads a ROM at 0x200. Returns false if it doesn't fit. */
        boolean load(byte[] rom) {
            if (rom.length > MEMORY_SIZE - PROGRAM_START) {
                return false;
            }
            Arrays.fill(memory, (byte) 0);
            Arrays.fill(v, 0);
            Arrays.fill(stack, 0);
            Arrays.fill(display, false);
            for (int k = 0; k < FONT.length; k++) {
                memory[FONT_START + k] = (byte) FONT[k];
            }
            System.arraycopy(rom, 0, memory, PROGRAM_START, rom.length);
            i = 0;
            pc = PROGRAM_START;
            sp = 0;
            delayTimer = 0;
            soundTimer = 0;
            vblankHit = false;
            waitState = 0;
            return true;
        }

        boolean soundActive() {
            return soundTimer > 0;
        }

        /** One 60 Hz frame: up to `cycles` instructions, then both timers count down. */
        void runTick(int cycles) {
            vblankHit = false;
            for (int c = 0; c < cycles && !vblankHit; c++) {
                step();
            }
            if (delayTimer > 0) {
                delayTimer--;
            }
            if (soundTimer > 0) {
                soundTimer--;
            }
        }

        void step() {
            if (waitState != 0) {
                stepWaitForKey();
                return;
            }

            int hi = memory[pc] & 0xFF;
            int lo = memory[(pc + 1) & 0xFFF] & 0xFF;
            int op = (hi << 8) | lo;
            pc = (pc + 2) & 0xFFF;

            int x = hi & 0xF;
            int y = lo >> 4;
            int n = lo & 0xF;
            int nn = lo;
            int nnn = op & 0xFFF;

            switch (hi >> 4) {
                case 0x0:
                    if (op == 0x00E0) {
                        Arrays.fill(display, false);
                    } else if (op == 0x00EE) {
                        if (sp > 0) {
                            pc = stack[--sp];
                        }
                    }
                    break; // 0NNN (machine code call) is ignored
                case 0x1:
                    pc = nnn;
                    break;
                case 0x2:
                    if (sp < stack.length) {
                        stack[sp++] = pc;
                    }
                    pc = nnn;
                    break;
                case 0x3:
                    if (v[x] == nn) skip();
                    break;
                case 0x4:
                    if (v[x] != nn) skip();
                    break;
                case 0x5:
                    if (n == 0 && v[x] == v[y]) skip();
                    break;
                case 0x6:
                    v[x] = nn;
                    break;
                case 0x7:
                    v[x] = (v[x] + nn) & 0xFF;
                    break;
                case 0x8:
                    alu(x, y, n);
                    break;
                case 0x9:
                    if (n == 0 && v[x] != v[y]) skip();
                    break;
                case 0xA:
                    i = nnn;
                    break;
                case 0xB:
                    pc = (nnn + v[0]) & 0xFFF;
                    break;
                case 0xC:
                    v[x] = rng.nextInt(256) & nn;
                    break;
                case 0xD:
                    draw(x, y, n);
                    break;
                case 0xE:
                    if (nn == 0x9E) {
                        if (keypad.isDown(v[x] & 0xF)) skip();
                    } else if (nn == 0xA1) {
                        if (!keypad.isDown(v[x] & 0xF)) skip();
                    }
                    break;
                case 0xF:
                    misc(x, nn);
                    break;
                default:
                    break;
            }
        }

        private void skip() {
            pc = (pc + 2) & 0xFFF;
        }

        private void alu(int x, int y, int n) {
            int vx = v[x];
            int vy = v[y];
            switch (n) {
                case 0x0:
                    v[x] = vy;
                    break;
                case 0x1:
                    v[x] = vx | vy;
                    if (vfReset) v[0xF] = 0;
                    break;
                case 0x2:
                    v[x] = vx & vy;
                    if (vfReset) v[0xF] = 0;
                    break;
                case 0x3:
                    v[x] = vx ^ vy;
                    if (vfReset) v[0xF] = 0;
                    break;
                case 0x4: {
                    int sum = vx + vy;
                    v[x] = sum & 0xFF;
                    v[0xF] = sum > 0xFF ? 1 : 0;
                    break;
                }
                case 0x5:
                    v[x] = (vx - vy) & 0xFF;
                    v[0xF] = vx >= vy ? 1 : 0;
                    break;
                case 0x6: {
                    int src = shiftUsesVy ? vy : vx;
                    v[x] = src >> 1;
                    v[0xF] = src & 1;
                    break;
                }
                case 0x7:
                    v[x] = (vy - vx) & 0xFF;
                    v[0xF] = vy >= vx ? 1 : 0;
                    break;
                case 0xE: {
                    int src = shiftUsesVy ? vy : vx;
                    v[x] = (src << 1) & 0xFF;
                    v[0xF] = (src >> 7) & 1;
                    break;
                }
                default:
                    break;
            }
        }

        private void draw(int x, int y, int n) {
            int px = v[x] % WIDTH;
            int py = v[y] % HEIGHT;
            boolean collision = false;
            for (int row = 0; row < n; row++) {
                int yy = py + row;
                if (yy >= HEIGHT) {
                    if (clipSprites) break;
                    yy %= HEIGHT;
                }
                int bits = memory[(i + row) & 0xFFF] & 0xFF;
                for (int col = 0; col < 8; col++) {
                    if ((bits & (0x80 >> col)) == 0) continue;
                    int xx = px + col;
                    if (xx >= WIDTH) {
                        if (clipSprites) break;
                        xx %= WIDTH;
                    }
                    int idx = yy * WIDTH + xx;
                    if (display[idx]) collision = true;
                    display[idx] = !display[idx];
                }
            }
            v[0xF] = collision ? 1 : 0;
            if (displayWait) {
                vblankHit = true;
            }
        }

        private void misc(int x, int nn) {
            switch (nn) {
                case 0x07:
                    v[x] = delayTimer;
                    break;
                case 0x0A:
                    waitReg = x;
                    waitSerial = keypad.pressSerial();
                    waitState = 1;
                    break;
                case 0x15:
                    delayTimer = v[x];
                    break;
                case 0x18:
                    soundTimer = v[x];
                    break;
                case 0x1E:
                    i = (i + v[x]) & 0xFFF;
                    break;
                case 0x29:
                    i = FONT_START + (v[x] & 0xF) * 5;
                    break;
                case 0x33: {
                    int value = v[x];
                    memory[i & 0xFFF] = (byte) (value / 100);
                    memory[(i + 1) & 0xFFF] = (byte) ((value / 10) % 10);
                    memory[(i + 2) & 0xFFF] = (byte) (value % 10);
                    break;
                }
                case 0x55:
                    for (int k = 0; k <= x; k++) {
                        memory[(i + k) & 0xFFF] = (byte) v[k];
                    }
                    if (loadStoreIncrementsI) i = (i + x + 1) & 0xFFF;
                    break;
                case 0x65:
                    for (int k = 0; k <= x; k++) {
                        v[k] = memory[(i + k) & 0xFFF] & 0xFF;
                    }
                    if (loadStoreIncrementsI) i = (i + x + 1) & 0xFFF;
                    break;
                default:
                    break;
            }
        }

        // FX0A: wait for a NEW keypress, then (like the original) for that key's release.
        private void stepWaitForKey() {
            if (waitState == 1) {
                if (keypad.pressSerial() == waitSerial) {
                    return;
                }
                waitKey = keypad.lastPressed() & 0xF;
                waitState = 2;
            }
            if (keypad.isDown(waitKey)) {
                return;
            }
            v[waitReg] = waitKey;
            waitState = 0;
        }
    }
}