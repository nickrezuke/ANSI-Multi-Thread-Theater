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
                while (isRunning) {
                    int firstByte = System.in.read();
                    if (firstByte == -1) break;

                    // Standard escape sequences for arrow keys start with 27 (\u001B)
                    if (firstByte == 27) {
                        int secondByte = System.in.read();
                        int thirdByte = System.in.read();

                        if (secondByte == '[' || secondByte == 'O') {
                            handleKeyInput(thirdByte);
                        }
                    } else {
                        // Pass standard character keys (like letters or space) directly
                        handleKeyInput(firstByte);
                    }
                }
            } catch (IOException e) {
                // Thread closing down cleanly
            }
        });

        inputThread.setDaemon(true);
        inputThread.start();

        // Allow child classes to run their own custom initialization if needed
        onInitialize();
    }

    /**
     * Subclasses override this to respond to raw keystrokes in real-time.
     * For Arrow Keys, the third byte of the ANSI sequence is mapped:
     * 'A' = Up, 'B' = Down, 'C' = Right, 'D' = Left
     */
    protected abstract void handleKeyInput(int keyCode);

    /** Optional hooks for child classes since initialize() is now finalized */
    protected void onInitialize() {}

    @Override
    public void stopLoading() {
        super.stopLoading();
        // Crucial: Restore original terminal characteristics immediately upon exit
        TerminalConfig.restoreMode();
    }
}
