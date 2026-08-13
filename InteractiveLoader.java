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
                        // Check if bytes are following for the arrow key sequence
                        if (System.in.available() >= 2) {
                            int secondByte = System.in.read();
                            int thirdByte = System.in.read();
                            if (secondByte == '[' || secondByte == 'O') {
                                handleKeyInput(thirdByte);
                            }
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
