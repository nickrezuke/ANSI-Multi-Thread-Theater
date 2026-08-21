import java.io.IOException;

public abstract class InteractiveLoader extends Loader {

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
                            int thirdByte = waitForNextByte(50);
                            if (thirdByte != -1) {
                                handleKeyInput(thirdByte);
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
