import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MotorcycleRacerLoader extends InteractiveLoader {
    private static final StatusStage[] RACER_STAGES = {
            new StatusStage(100, "[ Left / Right : Steer ]")
    };

    private int WIDTH;
    private int HEIGHT;

    // Player State
    private volatile double playerX = 0.0; // -1.0 (Left edge) to 1.0 (Right edge)
    private volatile double speed = 10.0; // Velocity tracker
    private static final double MAX_SPEED = 10.0; // Cruise speed cap
    private volatile double trackPosition = 0.0; // Distance traveled down route
    private int invincibilityFrames = 0; // Hit-state flashing tick tracker

    // Track Geometry
    private volatile double trackCurve = 0.0; // Curve intensity
    private volatile double targetCurve = 0.0; // Heading goal force
    private int curveTimer = 0;
    private final Random random = new Random();

    // Color Palette Theme Engine Structures
    private static class PaletteTheme {
        String sky, horizon, road, shoulder, bike, hit, coneBase, coneStripe;

        PaletteTheme(String sk, String hz, String rd, String sh, String bk, String ht, String cb, String cs) {
            this.sky = sk;
            this.horizon = hz;
            this.road = rd;
            this.shoulder = sh;
            this.bike = bk;
            this.hit = ht;
            this.coneBase = cb;
            this.coneStripe = cs;
        }
    }

    private final PaletteTheme[] themes = {
            // Theme 0: Cyber Synthwave (Original Neon Layout)
            new PaletteTheme("\u001B[38;2;10;15;30m", "\u001B[38;2;255;45;140m", "\u001B[38;2;40;40;50m",
                    "\u001B[38;2;0;240;180m", "\u001B[38;2;255;210;0m", "\u001B[38;2;240;240;240m",
                    "\u001B[38;2;255;90;0m", "\u001B[38;2;240;240;240m"),
            // Theme 1: Virtual Boy (Retro Matrix Wireframe Red Monolith)
            new PaletteTheme("\u001B[38;2;0;0;0m", "\u001B[38;2;255;0;0m", "\u001B[38;2;15;0;0m",
                    "\u001B[38;2;120;0;0m", "\u001B[38;2;255;50;50m", "\u001B[38;2;255;255;255m",
                    "\u001B[38;2;180;0;0m", "\u001B[38;2;255;100;100m"),
            // Theme 2: Cyberpunk Toxic (Acid Green Wasteland Grid)
            new PaletteTheme("\u001B[38;2;20;5;25m", "\u001B[38;2;0;255;100m", "\u001B[38;2;30;30;35m",
                    "\u001B[38;2;200;255;0m", "\u001B[38;2;0;200;255m", "\u001B[38;2;255;255;255m",
                    "\u001B[38;2;220;255;0m", "\u001B[38;2;40;40;45m"),
            // Theme 3: OutRun Dusk (Golden Hour California Horizon)
            new PaletteTheme("\u001B[38;2;255;120;0m", "\u001B[38;2;255;230;0m", "\u001B[38;2;55;40;50m",
                    "\u001B[38;2;140;20;100m", "\u001B[38;2;255;255;255m", "\u001B[38;2;255;45;140m",
                    "\u001B[38;2;255;80;0m", "\u001B[38;2;255;230;0m")
    };

    private volatile int currentThemeIndex = 0;
    private int themeCooldownFrames = 0; // Solid frame-based input throttle

    // Obstacle Tracking
    private static class RoadObstacle {
        double trackZ;
        double roadX;
    }

    private final List<RoadObstacle> obstacles = new ArrayList<>();
    private int spawnTimer = 0;

    // System Clock Warnings
    private static final String STROBE_COLOR_A = "\u001B[38;2;255;0;50m";
    private static final String STROBE_COLOR_B = "\u001B[38;2;255;255;0m";

    public MotorcycleRacerLoader() {
        super(RACER_STAGES);
        this.HEIGHT = this.window_height;
        this.WIDTH = this.window_width;
    }

    @Override
    protected void onInitialize() {
        this.playerX = 0.0;
        this.speed = 0.23;
        this.trackPosition = 0.0;
        this.trackCurve = 0.0;
        this.targetCurve = 0.0;
        this.invincibilityFrames = 0;
        this.currentThemeIndex = random.nextInt(themes.length);
        this.themeCooldownFrames = 0;
        this.obstacles.clear();
        this.spawnTimer = 0;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        switch (keyCode) {
            case 'A': // Arrow UP -> Cycle theme forward if cooldown allows
                if (themeCooldownFrames == 0) {
                    currentThemeIndex = (currentThemeIndex + 1) % themes.length;
                    themeCooldownFrames = 8; // Block theme inputs for the next 8 frames (~200ms)
                }
                break;
            case 'B': // Arrow DOWN -> Cycle theme backward if cooldown allows
                if (themeCooldownFrames == 0) {
                    currentThemeIndex = (currentThemeIndex - 1 + themes.length) % themes.length;
                    themeCooldownFrames = 8;
                }
                break;
            case 'C': // Arrow RIGHT -> Move Right
                playerX += 0.14;
                if (playerX > 1.8)
                    playerX = 1.8;
                break;
            case 'D': // Arrow LEFT -> Move Left
                playerX -= 0.14;
                if (playerX < -1.8)
                    playerX = -1.8;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        PaletteTheme active = themes[currentThemeIndex];

        // 1. Tick input cooldown timer down to zero safely
        if (themeCooldownFrames > 0) {
            themeCooldownFrames--;
        }

        // Core Physics Advancement Block
        trackPosition += speed * 0.05;

        for (int i = obstacles.size() - 1; i >= 0; i--) {
            RoadObstacle obs = obstacles.get(i);
            obs.trackZ -= speed * 0.22;
            if (obs.trackZ <= 2.0) {
                if (obs.trackZ > -2.0 && invincibilityFrames == 0 && Math.abs(obs.roadX - playerX) < 0.15) {
                    speed = 0.2;
                    invincibilityFrames = 50;
                }
                if (obs.trackZ < 0) {
                    obstacles.remove(i);
                }
            }
        }

        if (invincibilityFrames > 0) {
            invincibilityFrames--;
        }

        if (speed < MAX_SPEED) {
            speed = speed * 1.020;
            if (speed > MAX_SPEED)
                speed = MAX_SPEED;
        }

        if (speed > 0.001) {
            curveTimer--;
            if (curveTimer <= 0) {
                targetCurve = (random.nextDouble() * 2.0 - 1.0) * 1.5;
                curveTimer = 45 + random.nextInt(55);
            }
            trackCurve += (targetCurve - trackCurve) * 0.05;
            playerX -= trackCurve * (speed / 35.0) * 0.018;
        }

        if (playerX != 0.0) {
            playerX += 0.001 * (Math.abs(playerX) / playerX);
        }

        // Spawn Cones into the depth layer
        spawnTimer--;
        if (spawnTimer <= 0 && speed > 4.0) {
            RoadObstacle obs = new RoadObstacle();
            obs.trackZ = 100.0;
            obs.roadX = (random.nextDouble() * 1.2) - 0.6;
            obstacles.add(obs);
            spawnTimer = 35 + random.nextInt(35);
        }

        // 2. Render Frame (Row by Row Pseudo-3D Rasterization)
        int horizonY = 9;
        double currentCurveOffset = 0.0;

        for (int y = 0; y < HEIGHT; y++) {
            if (y >= horizonY) {
                double perspective = (double) (y - horizonY) / (HEIGHT - horizonY);
                currentCurveOffset += trackCurve * perspective * 0.8;
            }

            for (int x = 0; x < WIDTH; x++) {
                int index = x + WIDTH * y;
                zBuffer[index] = 0.0;

                if (y < horizonY) {
                    if (y == horizonY - 1) {
                        outputBuffer[index] = active.horizon + "▬" + RESET;
                    } else {
                        outputBuffer[index] = active.sky + " " + RESET;
                    }
                } else {
                    double perspective = (double) (y - horizonY) / (HEIGHT - horizonY);
                    if (perspective < 0.01)
                        perspective = 0.01;

                    double roadWidth = 15.0 + (perspective * 50.0);
                    double roadCenter = (WIDTH / 2.0) + currentCurveOffset - (playerX * perspective * 30.0);

                    double leftEdge = roadCenter - roadWidth / 2.0;
                    double rightEdge = roadCenter + roadWidth / 2.0;
                    double shoulderWidth = 2.0 + (perspective * 4.0);

                    if (x >= leftEdge && x <= rightEdge) {
                        boolean stripe = ((int) (trackPosition + (1.0 / perspective) * 1.5) % 4) == 0;

                        if (x < leftEdge + shoulderWidth || x > rightEdge - shoulderWidth) {
                            outputBuffer[index] = active.shoulder + (stripe ? "█" : "░") + RESET;
                        } else {
                            double centerDist = Math.abs(x - roadCenter);
                            if (centerDist < 1.0 && stripe) {
                                outputBuffer[index] = active.horizon + "█" + RESET;
                            } else {
                                outputBuffer[index] = active.road + "·" + RESET;
                            }
                        }
                    } else {
                        outputBuffer[index] = active.sky + " " + RESET;
                    }
                }
            }
        }

        // 3. Overlap Projected Traffic Cones
        for (RoadObstacle obs : obstacles) {
            if (obs.trackZ <= 0 || obs.trackZ > 95.0)
                continue;

            double perspectiveFactor = (100.0 - obs.trackZ) / 100.0;
            if (perspectiveFactor < 0.01)
                perspectiveFactor = 0.01;

            int targetRowY = horizonY + (int) (perspectiveFactor * (HEIGHT - horizonY));
            if (targetRowY >= HEIGHT)
                continue;

            double geometryCurveOffset = 0.0;
            for (int k = horizonY; k <= targetRowY; k++) {
                double p = (double) (k - horizonY) / (HEIGHT - horizonY);
                geometryCurveOffset += trackCurve * p * 0.8;
            }

            double totalRoadWidthAtRow = 15.0 + (perspectiveFactor * 50.0);
            double absoluteRoadCenterScreenX = (WIDTH / 2.0) + geometryCurveOffset
                    - (playerX * perspectiveFactor * 30.0);
            int projectedScreenX = (int) (absoluteRoadCenterScreenX + (obs.roadX * totalRoadWidthAtRow * 0.8));
            int renderSizeWidth = (int) (perspectiveFactor * 4.0);
            int renderSizeHeight = (int) (perspectiveFactor * 5.0);
            if (renderSizeWidth < 1)
                renderSizeWidth = 1;
            if (renderSizeHeight < 1)
                renderSizeHeight = 1;
            for (int h = 0; h < renderSizeHeight; h++) {
                int drawY = targetRowY - h;
                if (drawY < horizonY || drawY >= HEIGHT)
                    continue;
                double heightRatio = (double) h / renderSizeHeight;
                int currentWidth = (int) (renderSizeWidth * (1.0 - (heightRatio * 0.65)));
                if (currentWidth < 1)
                    currentWidth = 1;
                boolean stripeRow = (h == renderSizeHeight / 2 || h == (renderSizeHeight / 2) + 1);
                String partColor = stripeRow ? active.coneStripe : active.coneBase;
                for (int w = -currentWidth / 2; w <= currentWidth / 2; w++) {
                    int drawX = projectedScreenX + w;
                    if (drawX >= 0 && drawX < WIDTH) {
                        int bufferIdx = drawX + WIDTH * drawY;
                        double inverseDepthValue = 100.0 - obs.trackZ;
                        if (inverseDepthValue > zBuffer[bufferIdx]) {
                            zBuffer[bufferIdx] = inverseDepthValue;
                            outputBuffer[bufferIdx] = partColor + "█" + RESET;
                        }
                    }
                }
            }
        }
        // 4. Central Field of View Steering Directional Warnings
        if (Math.abs(playerX) > 1.05) {
            String warningText = (playerX > 1.05) ? "◄◄◄ TURN LEFT ◄◄◄" : "►►► TURN RIGHT ►►►";
            int textLength = warningText.length();
            int uiRow = horizonY - 2;
            int startX = (WIDTH - textLength) / 2;
            long currentTime = System.currentTimeMillis();
            boolean toggleFrame = (currentTime / 150) % 2 == 0;
            String flashingActiveColor = toggleFrame ? STROBE_COLOR_A : STROBE_COLOR_B;
            for (int i = 0; i < textLength; i++) {
                int targetIndex = (startX + i) + (WIDTH * uiRow);
                if (targetIndex >= 0 && targetIndex < outputBuffer.length) {
                    outputBuffer[targetIndex] = flashingActiveColor + warningText.charAt(i) + RESET;
                }
            }
        }
        // 5. Draw Cockpit Player Avatar (with Flashing States)
        boolean hideAvatarFrame = (invincibilityFrames > 0) && (invincibilityFrames % 4 < 2);
        String bikeColor = (invincibilityFrames > 0) ? active.hit : active.bike;
        if (!hideAvatarFrame) {
            int playerScreenX = WIDTH / 2;
            int playerScreenY = HEIGHT - 2;
            int pIndex = playerScreenX + WIDTH * playerScreenY;
            outputBuffer[pIndex] = bikeColor + "█" + RESET;
            outputBuffer[pIndex - 1] = bikeColor + "◢" + RESET;
            outputBuffer[pIndex + 1] = bikeColor + "◣" + RESET;
            outputBuffer[playerScreenX + WIDTH * (playerScreenY - 1)] = bikeColor + "▲" + RESET;
        }
    }
}