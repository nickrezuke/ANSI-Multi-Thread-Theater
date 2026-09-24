import java.io.IOException;

public abstract class InteractiveLoader extends Loader {
    // Distinct key codes for the arrow keys. Only delivered to a subclass that opts in
    // by overriding useDistinctArrowCodes() to return true (see below). They sit above
    // the byte range, so they can never collide with a typed character.
    public static final int KEY_UP = 0x101;
    public static final int KEY_DOWN = 0x102;
    public static final int KEY_RIGHT = 0x103;
    public static final int KEY_LEFT = 0x104;

    // By default (false) arrows keep their legacy behaviour: the third byte of the ANSI
    // sequence is passed straight to handleKeyInput(), so Up/Down/Right/Left arrive as
    // 'A'/'B'/'C'/'D' - indistinguishable from a typed capital letter. A subclass that
    // needs real letter keys (like Chip8Loader) overrides this to return true and gets
    // KEY_UP/KEY_DOWN/KEY_RIGHT/KEY_LEFT instead. Other escape sequences (Delete, Home,
    // F-keys, Ctrl+Arrow, ...) are then swallowed whole instead of leaking stray
    // characters into handleKeyInput().
    protected boolean useDistinctArrowCodes() {
        return false;
    }

    public InteractiveLoader(StatusStage[] stages) {
        super(stages);
        this.isRawCanvas = true;
    }

    public InteractiveLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.isRawCanvas = true;
    }

    @Override
    protected final void initialize() {
        // 1. Flip the active terminal window into unbuffered raw execution mode
        TerminalConfig.setRawMode();

        // 2. Spawn an independent background daemon thread to harvest instant keyboard strokes
        Thread inputThread = new Thread(() -> {
            try {
                // Read directly from System.in without available() checks.
                // In raw mode, System.in.read() blocks until a key is hit, 
                // which uses 0% CPU and eliminates timing race conditions.
                while (isRunning) {
                    int firstByte = System.in.read();
                    if (firstByte == -1 || !isRunning) break; 

                    // Standard escape sequences for arrow keys start with 27 (\u001B)
                    if (firstByte == 27) {
                        // Poll briefly for the rest of the sequence rather than checking
                        // available() exactly once: on a laggy terminal (SSH, high load)
                        // the follow-up bytes can arrive a beat late, which previously
                        // meant they'd get silently dropped on the next loop iteration.
                        int secondByte = waitForNextByte(50);
                        if (secondByte == '[' || secondByte == 'O') {
                            if (useDistinctArrowCodes()) {
                                handleEscapeSequence();
                            } else {
                                int thirdByte = waitForNextByte(50);
                                if (thirdByte != -1) {
                                    handleKeyInput(thirdByte);
                                }
                            }
                        } else if (secondByte == -1) {
                            // Nothing followed within the timeout: this was a genuine,
                            // standalone Escape keypress, not the start of a sequence.
                            // (Previously this case was silently swallowed entirely.)
                            handleKeyInput(27);
                        } else {
                            // Some other byte followed 27 that isn't a recognized
                            // CSI/SS3 lead-in ('[' or 'O'); deliver the Escape and let
                            // the byte that followed it be handled as its own keypress.
                            handleKeyInput(27);
                            handleKeyInput(secondByte);
                        }
                    } else {
                        // Pass standard character keys (like letters, space, or numbers) directly
                        handleKeyInput(firstByte);
                    }
                }
            } catch (IOException e) {
                // Thread closing down cleanly due to stream disruption or terminal reset
            }
        });
        
        inputThread.setDaemon(true);
        inputThread.setName("Loader-Input-Thread");
        inputThread.start();

        // Allow child classes to run their own custom initialization if needed
        onInitialize();
    }

    // Consumes the remainder of a CSI ("ESC [") / SS3 ("ESC O") sequence after its
    // introducer. Only a bare arrow (no parameters, final byte A-D) is delivered, as
    // KEY_UP/KEY_DOWN/KEY_RIGHT/KEY_LEFT; everything else is read to its final byte and
    // dropped so its trailing bytes can't be mistaken for keypresses.
    private void handleEscapeSequence() throws IOException {
        int b = waitForNextByte(50);
        boolean hasParams = false;
        // Parameter bytes (0x30-0x3F) and intermediate bytes (0x20-0x2F)
        while (b >= 0x20 && b <= 0x3F) {
            hasParams = true;
            b = waitForNextByte(50);
        }
        if (b == -1 || hasParams) {
            return;
        }
        switch (b) {
            case 'A': handleKeyInput(KEY_UP); break;
            case 'B': handleKeyInput(KEY_DOWN); break;
            case 'C': handleKeyInput(KEY_RIGHT); break;
            case 'D': handleKeyInput(KEY_LEFT); break;
            default: break; // Home/End/F1-F4/etc: swallow
        }
    }

    // Waits up to timeoutMillis for another byte to become available, polling in
    // short slices instead of either checking available() a single time or doing
    // a naive blocking read. Returns the byte read, or -1 if the timeout elapses
    // with nothing arriving (e.g. a standalone Escape keypress with no sequence
    // behind it).
    private static int waitForNextByte(int timeoutMillis) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (System.in.available() > 0) {
                return System.in.read();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        return -1;
    }

    // Subclasses override this to respond to raw keystrokes in real-time.
    // For Arrow Keys, the third byte of the ANSI sequence is mapped:
    // 'A' = Up, 'B' = Down, 'C' = Right, 'D' = Left
    protected abstract void handleKeyInput(int keyCode);

    // Optional hooks for child classes since initialize() is now finalized
    protected void onInitialize() {}

    @Override
    public void stopLoading() {
        TerminalConfig.restoreMode();
        super.stopLoading(); 
    }
}
