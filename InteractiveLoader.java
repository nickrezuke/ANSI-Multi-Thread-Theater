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
                    // Check if bytes are physically waiting in the stream buffer
                    if (System.in.available() > 0) {
                        int firstByte = System.in.read();
                        if (firstByte == -1) break;

                        // Standard escape sequences for arrow keys start with 27 (\u001B)
                        if (firstByte == 27) {
                            // Briefly wait to ensure multi-byte sequence frames have arrived
                            long start = System.currentTimeMillis();
                            while (System.in.available() < 2 && (System.currentTimeMillis() - start) < 50) {
                                Thread.onSpinWait();
                            }
                            
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
                    } else {
                        // High-performance backoff pause to prevent 100% CPU thread thrashing
                        // while allowing near-instant response to both keys and thread stop flags
                        Thread.sleep(20); 
                    }
                }
            } catch (IOException e) {
                // Thread closing down cleanly due to stream disruption
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        });

        inputThread.setDaemon(true);
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
    // USE THIS FOR YOUR OWN INTERACTIVE LOADERS INSTEAD OF INITIALIZE IF YOU ADD TO MY PROJECT

    @Override
    public void stopLoading() {
        super.stopLoading();
        // Crucial: Restore original terminal characteristics immediately upon exit
        TerminalConfig.restoreMode();
    }
}
