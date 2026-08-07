import java.util.Random;

public class LunarLanderLoader extends Loader {

    private static final StatusStage[] LANDER_STAGES = {
            new StatusStage(25, "Calculating orbital trajectory..."),
            new StatusStage(50, "Calibrating thrust nozzles..."),
            new StatusStage(75, "Scanning lunar surface topography..."),
            new StatusStage(100, "Eagle Lander Detached!")
    };

    private static final int TERMINAL_W = 80;
    private static final int TERMINAL_H = 22;

    // Physics Simulation Constants (Balanced floaty profile)
    private static final double GRAVITY = 0.03;
    private static final double THRUST_ACCEL = 0.1;
    private static final double LATERAL_THRUST = 0.04;
    private static final double MAX_SAFE_SPEED = 0.45;

    // Simulation Physics State
    private double posX = 40.0;
    private double posY = 2.0;
    private double velX = 0.0;
    private double velY = 0.0;
    private double fuel = 600.0;

    // Simulation Management Parameters
    private final int[] surfaceHeights = new int[TERMINAL_W];
    private int padStartX = 0;
    private int padEndX = 0;
    private double padCenterXTgt = 0.0;
    private final Random rand = new Random();

    private boolean simulationOver = false;
    private String outcomeMessage = "";
    private long lastTickTime = 0;
    private long stateResetTime = 0;
    private static final long PHYSICS_TICK_MS = 65;
    private static final long REBOOT_PAUSE_MS = 4000;

    // High-Compatibility ANSI 256-Color Profile
    private static final String C_HUD_TXT = "\u001B[38;5;253m";
    private static final String C_HUD_GREEN = "\u001B[38;5;46m";
    private static final String C_HUD_RED = "\u001B[38;5;196m";
    private static final String C_LANDER = "\u001B[38;5;15m";
    private static final String C_FIRE = "\u001B[38;5;208m";
    private static final String C_EXPLODE = "\u001B[38;5;160m";
    private static final String C_PAD = "\u001B[38;5;81m";
    private static final String C_CRUST = "\u001B[38;5;244m";
    private static final String C_BEDROCK = "\u001B[38;5;237m";

    public LunarLanderLoader() {
        super(LANDER_STAGES);
    }

    @Override
    protected void initialize() {
        generateLunarTopography();
        resetLanderSimulation();
        lastTickTime = System.currentTimeMillis();
    }

    private void resetLanderSimulation() {
        simulationOver = false;
        outcomeMessage = "";

        posX = 5.0 + rand.nextInt(20);
        posY = 2.0;
        velX = 0.3 + (rand.nextDouble() * 0.4);
        velY = 0.0;
        fuel = 650.0 + rand.nextInt(100);
    }

    private void generateLunarTopography() {
        int currentHeight = 16 + rand.nextInt(4);
        for (int x = 0; x < TERMINAL_W; x++) {
            if (rand.nextInt(8) == 0) {
                currentHeight += (rand.nextBoolean() ? 1 : -1);
                if (currentHeight > TERMINAL_H - 1)
                    currentHeight = TERMINAL_H - 1;
                if (currentHeight < 13)
                    currentHeight = 13;
            }
            surfaceHeights[x] = currentHeight;
        }

        padStartX = 45 + rand.nextInt(18);
        padEndX = padStartX + 12;
        int padElevation = 18;

        for (int x = padStartX; x <= padEndX; x++) {
            surfaceHeights[x] = padElevation;
        }
        padCenterXTgt = padStartX + (double) (padEndX - padStartX) / 2.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();

        if (simulationOver) {
            if (currentTime - stateResetTime >= REBOOT_PAUSE_MS) {
                generateLunarTopography();
                resetLanderSimulation();
                lastTickTime = currentTime;
            }
            drawLunarCanvas(outputBuffer, 0);
            return;
        }

        if (currentTime - lastTickTime >= PHYSICS_TICK_MS) {
            lastTickTime = currentTime;
            processLanderPhysics();
        }

        int thrustState = 0;
        if (!simulationOver && fuel > 0) {
            if (shouldEngageVerticalThruster())
                thrustState = 1;
            else if (getRequiredLateralThrust() < 0)
                thrustState = 2;
            else if (getRequiredLateralThrust() > 0)
                thrustState = 3;
        }

        drawLunarCanvas(outputBuffer, thrustState);
    }

    private double getRequiredLateralThrust() {
        if (posY > 13.0)
            return 0.0;

        double dx = padCenterXTgt - posX;
        double targetVelX = dx * 0.05;

        if (targetVelX > 0.6)
            targetVelX = 0.6;
        if (targetVelX < -0.6)
            targetVelX = -0.6;

        double diffX = targetVelX - velX;
        if (diffX > 0.02)
            return LATERAL_THRUST;
        if (diffX < -0.02)
            return -LATERAL_THRUST;

        return 0.0;
    }

    private boolean shouldEngageVerticalThruster() {
        int currentGridX = Math.min(TERMINAL_W - 1, Math.max(0, (int) posX));
        double distanceToGround = surfaceHeights[currentGridX] - posY;

        // CUTOFF OVERRIDE: If aligned, close, and dropping safely, let it touch down!
        if (distanceToGround < 1.5 && currentGridX >= padStartX && currentGridX <= padEndX) {
            if (velY <= MAX_SAFE_SPEED * 0.8) {
                return false;
            }
        }

        double stopDistance = (velY * velY) / (2.0 * (THRUST_ACCEL - GRAVITY));

        if (distanceToGround <= stopDistance + 0.3)
            return true;
        if (velY > 0.40)
            return true;

        return false;
    }

    private void processLanderPhysics() {
        boolean verticalEngaged = shouldEngageVerticalThruster();
        double lateralCorrection = getRequiredLateralThrust();

        if (fuel > 0) {
            if (verticalEngaged) {
                velY -= (THRUST_ACCEL - GRAVITY);
                fuel -= 5.0;
            } else {
                velY += GRAVITY;
            }

            if (lateralCorrection != 0.0 && !verticalEngaged) {
                velX += lateralCorrection;
                fuel -= 2.0;
            }
        } else {
            velY += GRAVITY;
        }

        posX += velX;
        posY += velY;

        if (posX < 0) {
            posX = 0;
            velX = -velX;
        }
        if (posX >= TERMINAL_W) {
            posX = TERMINAL_W - 1;
            velX = -velX;
        }

        int currentGridX = (int) posX;
        int currentGridY = (int) posY;

        if (currentGridY >= surfaceHeights[currentGridX]) {
            simulationOver = true;
            stateResetTime = System.currentTimeMillis();
            posY = surfaceHeights[currentGridX];
            velX = 0.0;

            if (currentGridX >= padStartX && currentGridX <= padEndX) {
                if (Math.abs(velY) <= MAX_SAFE_SPEED) {
                    outcomeMessage = "  [TOUCHDOWN SUCCESSFUL - EAGLE HAS LANDED]";
                } else {
                    outcomeMessage = "  [CRITICAL IMPACT - LANDER DESTROYED]";
                }
            } else {
                outcomeMessage = "  [CRASHED - MISSED THE LANDING PAD]";
            }
        }
    }

    private void drawLunarCanvas(String[] outputBuffer, int thrustState) {
        // Erase old data frames completely by resetting the row arrays
        for (int i = 0; i < TERMINAL_W; i++) {
            outputBuffer[i] = "";
        }

        String speedColor = (Math.abs(velY) <= MAX_SAFE_SPEED) ? C_HUD_GREEN : C_HUD_RED;
        StringBuilder hud = new StringBuilder(" ");
        hud.append(C_HUD_TXT).append(String.format("ALT: %2.0f0m  ", (18.0 - posY) * 10))
                .append("VX: ").append(String.format("%+4.2f  ", velX))
                .append("VY: ").append(speedColor).append(String.format("%+4.2f  ", velY))
                .append(C_HUD_TXT).append(String.format("FUEL: %3.0f ", Math.max(0, fuel)));

        if (simulationOver) {
            hud.append(outcomeMessage);
        } else if (fuel <= 0) {
            hud.append(C_HUD_RED).append(" [FUEL DEPLETED]");
        } else {
            // Overwrites any leftover historical string buffers with blank space padding
            hud.append("                                            ");
        }
        hud.append(RESET);
        outputBuffer[0] = hud.toString();

        // 2. CONSTRUCT MOON SURFACE TEXTURES
        for (int x = 0; x < TERMINAL_W; x++) {
            int surfaceY = surfaceHeights[x];
            int surfaceIdx = x + (surfaceY * TERMINAL_W);

            if (surfaceIdx >= TERMINAL_W && surfaceIdx < outputBuffer.length) {
                if (x >= padStartX && x <= padEndX) {
                    outputBuffer[surfaceIdx] = C_PAD + "▔" + RESET;
                } else {
                    outputBuffer[surfaceIdx] = C_CRUST + "▲" + RESET;
                }
            }

            for (int y = surfaceY + 1; y < TERMINAL_H; y++) {
                int rockIdx = x + (y * TERMINAL_W);
                if (rockIdx >= TERMINAL_W && rockIdx < outputBuffer.length) {
                    if (y == surfaceY + 1) {
                        outputBuffer[rockIdx] = C_BEDROCK + "▓" + RESET;
                    } else {
                        outputBuffer[rockIdx] = C_BEDROCK + "█" + RESET;
                    }
                }
            }
        }

        // 3. OVERLAY LANDER
        int landerX = (int) posX;
        int landerY = (int) posY;
        int landerIdx = landerX + (landerY * TERMINAL_W);

        if (landerIdx >= TERMINAL_W && landerIdx < outputBuffer.length) {
            if (simulationOver && outcomeMessage.contains("DESTROYED")) {
                outputBuffer[landerIdx] = C_EXPLODE + "💥" + RESET;
            } else {
                outputBuffer[landerIdx] = C_LANDER + "🛸" + RESET;

                if (landerY + 1 < surfaceHeights[landerX]) {
                    int plumeIdx = landerX + ((landerY + 1) * TERMINAL_W);
                    if (plumeIdx < outputBuffer.length) {
                        if (thrustState == 1) {
                            outputBuffer[plumeIdx] = C_FIRE + "▼" + RESET;
                        } else if (thrustState == 2 && landerX + 1 < TERMINAL_W) {
                            outputBuffer[plumeIdx] = C_FIRE + "▶" + RESET;
                        } else if (thrustState == 3 && landerX - 1 >= 0) {
                            outputBuffer[plumeIdx] = C_FIRE + "◀" + RESET;
                        }
                    }
                }
            }
        }
    }
}