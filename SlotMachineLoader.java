import java.util.Arrays;
import java.util.Random;

public class SlotMachineLoader extends InteractiveLoader {
    private static final StatusStage[] SLOT_STAGES = {
            new StatusStage(100, "[Press Any Key to Pull Lever / Stop Reels Sequentially!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;

    // Game State Engine
    private volatile int gameState = 0; // 0=Waiting, 1=Stop R1, 2=Stop R2, 3=Stop R3, 4=Results
    private int inputCooldown = 0;

    // Rolling Wheel Physics Tracking (Sized correctly to 3 columns)
    private final double[] reelPositions = new double[3];
    private final double[] reelSpeeds = new double[3];
    private final int[] lockedSymbols = new int[3];

    // Mechanical Lever State (0=Up, 1=Mid, 2=Down, 3=Returning)
    private int leverState = 0;
    private int leverTimer = 0;

    // CORRECTED: Fixed dimension formatting to properly map rows directly as String
    // steps
    private static final String[][] AS_ICONS = {
            { // 0: Lucky Seven
                    "  /█████ ", "  └───██ ", "     ██  ", "    ██   ", "    ██   "
            },
            { // 1: Diamond
                    "   /█\\   ", "  /███\\  ", " /█████\\ ", " \\█████/ ", "  \\███/  "
            },
            { // 2: BAR Block
                    " ┌─────┐ ", " │ ███ │ ", " │ █▄█ │ ", " │ ███ │ ", " └─────┘ "
            },
            { // 3: Wild Star
                    "    █    ", " █  █  █ ", " ▀████▀  ", "  ████   ", " █ ▀▀ █  "
            },
            { // 4: Golden Bell
                    "   ▄█▄   ", "  █████  ", " ███████ ", " ▀▀███▀▀ ", "   ▄█▄   "
            }
    };

    // Palette Mapping
    private static final String[] ICON_COLORS = {
            "\u001B[38;2;255;50;50m", // 7: Crimson Red
            "\u001B[38;2;0;225;255m", // Diamond: Electric Cyan
            "\u001B[38;2;220;220;220m", // BAR: Metallic Silver
            "\u001B[38;2;255;230;0m", // Star: Golden Yellow
            "\u001B[38;2;255;140;0m" // Bell: Amber Orange
    };

    private static final String COLOR_CHASSIS = "\u001B[38;2;85;90;105m"; // Cabinet Plate Steel
    private static final String COLOR_FRAME = "\u001B[38;2;230;175;30m"; // Golden Accent Molding
    private static final String COLOR_LEVER = "\u001B[38;2;180;185;200m"; // Mechanical Arm Silver
    private static final String COLOR_TEXT = "\u001B[38;2;0;255;150m"; // Matrix Green Indicator

    private final Random random = new Random();

    public SlotMachineLoader() {
        super(SLOT_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void onInitialize() {
        this.gameState = 0;
        this.inputCooldown = 0;
        this.leverState = 0;
        this.leverTimer = 0;
        Arrays.fill(reelPositions, 0.0);
        Arrays.fill(reelSpeeds, 0.0);
        Arrays.fill(lockedSymbols, 0);
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (inputCooldown > 0)
            return;

        switch (gameState) {
            case 0:
            case 4: // Initialize spin physics sequence
                for (int i = 0; i < 3; i++) {
                    reelSpeeds[i] = 0.35 + random.nextDouble() * 0.25;
                }
                gameState = 1;
                leverState = 1; // Trigger mechanical crank down phase
                leverTimer = 3;
                inputCooldown = 15;
                break;

            case 1:
                lockReel(0);
                gameState = 2;
                inputCooldown = 10;
                break;

            case 2:
                lockReel(1);
                gameState = 3;
                inputCooldown = 10;
                break;

            case 3:
                lockReel(2);
                gameState = 4;
                inputCooldown = 25;
                break;
        }
    }

    private void lockReel(int idx) {
        reelSpeeds[idx] = 0.0;
        reelPositions[idx] = Math.round(reelPositions[idx]);
        lockedSymbols[idx] = ((int) reelPositions[idx]) % AS_ICONS.length;
        if (lockedSymbols[idx] < 0)
            lockedSymbols[idx] += AS_ICONS.length;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Clear viewport buffer bounds
        Arrays.fill(zBuffer, 0.0);
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = " ";
        }

        if (inputCooldown > 0)
            inputCooldown--;

        // Update mechanical animation vectors
        if (leverTimer > 0) {
            leverTimer--;
            if (leverTimer == 0) {
                if (leverState == 1) {
                    leverState = 2;
                    leverTimer = 4;
                } // Stay down temporarily
                else if (leverState == 2) {
                    leverState = 3;
                    leverTimer = 3;
                } // Recoil bounce upward
                else if (leverState == 3) {
                    leverState = 0;
                } // Return to stable up position
            }
        }

        // Advance rotating wheels speed calculations
        for (int i = 0; i < 3; i++) {
            if (reelSpeeds[i] > 0) {
                reelPositions[i] += reelSpeeds[i];
                if (reelPositions[i] >= AS_ICONS.length) {
                    reelPositions[i] -= AS_ICONS.length;
                }
            }
        }

        // 1. Draw Widescreen Structural Cabinet Framing
        for (int y = 3; y <= 25; y++) {
            for (int x = 10; x <= 90; x++) {
                int index = x + WIDTH * y;
                boolean isOuterEdge = (y == 3 || y == 25 || x == 10 || x == 90);
                boolean isInteriorRail = (x == 36 || x == 64);
                boolean insideWindowRows = (y >= 6 && y <= 22);

                if (isOuterEdge) {
                    outputBuffer[index] = COLOR_FRAME + "█" + RESET;
                } else if (isInteriorRail && insideWindowRows) {
                    outputBuffer[index] = COLOR_FRAME + "║" + RESET;
                } else if (insideWindowRows && x > 10 && x < 90) {
                    outputBuffer[index] = "\u001B[48;2;15;15;20m \u001B[0m";
                } else {
                    outputBuffer[index] = COLOR_CHASSIS + "░" + RESET;
                }
            }
        }

        // 2. Render Multi-Line ASCII Symbols with Continuous Vertical Offsets
        int[] reelCenterScreenX = { 23, 50, 77 };

        for (int r = 0; r < 3; r++) {
            int cx = reelCenterScreenX[r];
            double currentPos = reelPositions[r];
            int baseIdx = (int) Math.floor(currentPos);
            double verticalScrollFraction = currentPos - baseIdx;

            for (int offset = -1; offset <= 1; offset++) {
                int targetSymIdx = (baseIdx + offset + AS_ICONS.length) % AS_ICONS.length;
                String[] currentAsciiData = AS_ICONS[targetSymIdx];
                String activeColor = ICON_COLORS[targetSymIdx];

                int centerSlotPixelY = 14 - (int) Math.round(offset * 7.5 - verticalScrollFraction * 7.5);

                for (int rowLine = 0; rowLine < 5; rowLine++) {
                    int finalDrawY = centerSlotPixelY - 2 + rowLine;

                    if (finalDrawY >= 6 && finalDrawY <= 22) {
                        String lineString = currentAsciiData[rowLine];

                        for (int colChar = 0; colChar < lineString.length(); colChar++) {
                            int finalDrawX = cx - 4 + colChar;
                            int targetBufferIdx = finalDrawX + WIDTH * finalDrawY;

                            if (targetBufferIdx >= 0 && targetBufferIdx < outputBuffer.length) {
                                outputBuffer[targetBufferIdx] = activeColor + lineString.charAt(colChar) + RESET;
                            }
                        }
                    }
                }
            }
        }

        // 3. Render 4-Stage Mechanical Lever Display on Right Margin
        int lx = 93;
        int ly = 12;

        for (int h = -5; h <= 8; h++) {
            for (int w = 0; w <= 9; w++) {
                int clearIndex = (lx + w) + WIDTH * (ly + h);
                if (clearIndex >= 0 && clearIndex < outputBuffer.length)
                    outputBuffer[clearIndex] = " ";
            }
        }

        outputBuffer[lx + WIDTH * ly] = COLOR_CHASSIS + "▄" + RESET;
        outputBuffer[lx + 1 + WIDTH * ly] = COLOR_CHASSIS + "█" + RESET;
        outputBuffer[lx + 1 + WIDTH * (ly + 1)] = COLOR_CHASSIS + "▀" + RESET;

        if (leverState == 0) {
            outputBuffer[lx + 2 + WIDTH * (ly - 1)] = COLOR_LEVER + "/" + RESET;
            outputBuffer[lx + 3 + WIDTH * (ly - 2)] = COLOR_LEVER + "/" + RESET;
            outputBuffer[lx + 4 + WIDTH * (ly - 3)] = COLOR_LEVER + "/" + RESET;
            outputBuffer[lx + 5 + WIDTH * (ly - 4)] = "\u001B[38;2;255;40;40m" + "█" + RESET;
        } else if (leverState == 1 || leverState == 3) {
            outputBuffer[lx + 2 + WIDTH * ly] = COLOR_LEVER + "▬" + RESET;
            outputBuffer[lx + 3 + WIDTH * ly] = COLOR_LEVER + "─" + RESET;
            outputBuffer[lx + 4 + WIDTH * ly] = COLOR_LEVER + "─" + RESET;
            outputBuffer[lx + 5 + WIDTH * ly] = "\u001B[38;2;255;40;40m" + "█" + RESET;
        } else if (leverState == 2) {
            outputBuffer[lx + 2 + WIDTH * (ly + 1)] = COLOR_LEVER + "\\" + RESET;
            outputBuffer[lx + 3 + WIDTH * (ly + 2)] = COLOR_LEVER + "\\" + RESET;
            outputBuffer[lx + 4 + WIDTH * (ly + 3)] = COLOR_LEVER + "\\" + RESET;
            outputBuffer[lx + 5 + WIDTH * (ly + 4)] = "\u001B[38;2;255;40;40m" + "█" + RESET;
        }

        // 4. Overlap HUD Status Text Matrix Displays
        String uiMsg = "";
        if (gameState == 0)
            uiMsg = "  === PRESS ANY KEY TO CRANK LEVER AND SPIN ===  ";
        else if (gameState == 1)
            uiMsg = "  ◌ ROTATING ◌  === PRESS KEY TO STOP REEL 1 ===  ";
        else if (gameState == 2)
            uiMsg = "  ◌ ROTATING ◌  === PRESS KEY TO STOP REEL 2 ===  ";
        else if (gameState == 3)
            uiMsg = "  ◌ ROTATING ◌  === PRESS KEY TO STOP REEL 3 ===  ";
        else if (gameState == 4) {
            boolean win = (lockedSymbols[0] == lockedSymbols[1] && lockedSymbols[1] == lockedSymbols[2]);
            uiMsg = win ? "  JACKPOT!!! WINNER!!! PAYOUT ALL CREDITS!!!  " : " \u2717  OUT OF LUCK - TRY ANOTHER PULL  \u2717 ";
        }
        int targetX = 10 + (80 - uiMsg.length()) / 2;
        for (int i = 0; i < uiMsg.length(); i++) {
            int idx = (targetX + i) + WIDTH * 24;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = COLOR_TEXT + uiMsg.charAt(i) + RESET;
            }
        }
        String headerTitle = "   TRIPLE CHROME BIG LOAD SLOT REELS   ";
        int headX = 10 + (80 - headerTitle.length()) / 2;
        for (int i = 0; i < headerTitle.length(); i++) {
            int idx = (headX + i) + WIDTH * 4;
            if (idx >= 0 && idx < outputBuffer.length) {
                outputBuffer[idx] = COLOR_FRAME + headerTitle.charAt(i) + RESET;
            }
        }
    }
}