public class Labyrinth3DLoader extends InteractiveLoader {
    private static final StatusStage[] MAZE_STAGES = {
            new StatusStage(100, "[Arrow Keys to Control!]")
    };

    private static final int MAP_WIDTH = 9;
    private static final int MAP_HEIGHT = 9;
    private static final int[][] MAZE_MAP = {
            { 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 1, 1, 0, 1, 1, 0, 1 },
            { 1, 0, 1, 0, 0, 0, 1, 0, 1 },
            { 1, 0, 0, 0, 1, 0, 0, 0, 1 },
            { 1, 0, 1, 0, 0, 0, 1, 0, 1 },
            { 1, 0, 1, 1, 0, 1, 1, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 1 }
    };

    // Start at the absolute center of cell (1, 1)
    private volatile double posX = 1.5;
    private volatile double posY = 1.5;
    
    // Track explicit discrete target grid destinations
    private volatile int targetTileX = 1;
    private volatile int targetTileY = 1;

    private volatile double dirX = 1.0;
    private volatile double dirY = 0.0;
    private volatile double planeX = 0.0;
    private volatile double planeY = 0.66;
    private volatile double velTurn = 0.0;
    private volatile double targetAngle = 0.0;

    private static final double FOG_MAX_DIST = 5.5;

    private static final String COLOR_CEILING = "\u001B[38;2;25;30;45m";
    private static final String COLOR_FLOOR = "\u001B[38;2;50;55;60m";
    private static final String COLOR_WALL_Y = "\u001B[38;2;180;130;40m";
    private static final String COLOR_WALL_X = "\u001B[38;2;130;95;30m";

    public Labyrinth3DLoader() {
        super(MAZE_STAGES);
    }

    @Override
    protected void onInitialize() {
        this.posX = 1.5;
        this.posY = 1.5;
        this.targetTileX = 1;
        this.targetTileY = 1;
        this.targetAngle = 0.0;
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        int lookStepX = (int) Math.round(dirX);
        int lookStepY = (int) Math.round(dirY);

        switch (keyCode) {
            case 'A': // Arrow UP
                // Advance target coordinates by 1
                int nextX = targetTileX + lookStepX;
                int nextY = targetTileY + lookStepY;
                if (nextX >= 0 && nextX < MAP_WIDTH && nextY >= 0 && nextY < MAP_HEIGHT) {
                    if (MAZE_MAP[nextY][nextX] == 0) {
                        targetTileX = nextX;
                        targetTileY = nextY;
                    }
                }
                break;
                
            case 'B': // Arrow DOWN
                // Retreat target coordinates backward by 1
                int backX = targetTileX - lookStepX;
                int backY = targetTileY - lookStepY;
                if (backX >= 0 && backX < MAP_WIDTH && backY >= 0 && backY < MAP_HEIGHT) {
                    if (MAZE_MAP[backY][backX] == 0) {
                        targetTileX = backX;
                        targetTileY = backY;
                    }
                }
                break;
                
            case 'C': // Arrow RIGHT
                targetAngle += Math.PI / 2.0;
                break;
                
            case 'D': // Arrow LEFT
                targetAngle -= Math.PI / 2.0;
                break;
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Calculate rotation angle and smooth glide...
        double currentAngle = Math.atan2(dirY, dirX);
        double angleDelta = targetAngle - currentAngle;

        while (angleDelta < -Math.PI) angleDelta += 2.0 * Math.PI;
        while (angleDelta > Math.PI) angleDelta -= 2.0 * Math.PI;

        velTurn += angleDelta * 0.15;
        velTurn *= 0.65;

        if (Math.abs(velTurn) > 0.0001) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(velTurn) - dirY * Math.sin(velTurn);
            dirY = oldDirX * Math.sin(velTurn) + dirY * Math.cos(velTurn);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(velTurn) - planeY * Math.sin(velTurn);
            planeY = oldPlaneX * Math.sin(velTurn) + planeY * Math.cos(velTurn);
        }

        // Find the absolute centers of our desired target tile coordinates
        double centerTargetX = targetTileX + 0.5;
        double centerTargetY = targetTileY + 0.5;

        // Measure how far our floating position is from the true square center
        double deltaX = centerTargetX - posX;
        double deltaY = centerTargetY - posY;

        // Spring-Damping interpolation: Pull coordinates toward target center points.
        // This mirrors the rotational ease, creating a smooth gliding stride that stops perfectly centered.
        posX += deltaX * 0.18;
        posY += deltaY * 0.18;

        // On to the ray casting...
        for (int x = 0; x < 80; x++) {
            double cameraX = 0.025 * x - 1.0;
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX;
            int mapY = (int) posY;

            double sideDistX, sideDistY;

            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1.0 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1.0 / rayDirY);
            double perpendicularWallDist = 0.0;

            int stepX, stepY;
            int hit = 0;
            int side = 0;

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (posX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - posX) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (posY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - posY) * deltaDistY;
            }

            // DDA step sequence
            while (hit == 0) {
                if (sideDistX < sideDistY) {
                    perpendicularWallDist = sideDistX;
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    perpendicularWallDist = sideDistY;
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }

                // Fog Optimization. Stop calculations immediately
                // if the ray extends past our visual fog range limit.
                if (perpendicularWallDist >= FOG_MAX_DIST) {
                    perpendicularWallDist = FOG_MAX_DIST;
                    hit = 1;
                    side = -1; // Special flag value representing fog void
                    break;
                }

                if (MAZE_MAP[mapY][mapX] > 0) {
                    hit = 1;
                }
            }

            if (perpendicularWallDist < 0.01)
                perpendicularWallDist = 0.01;

            int lineHeight = (int) (22 / perpendicularWallDist);
            int drawStart = -lineHeight / 2 + 11;
            int drawEnd = lineHeight / 2 + 11;
            double inverseDepth = 1.0 / perpendicularWallDist;

            String wallColor = (side == 1) ? COLOR_WALL_Y : COLOR_WALL_X;
            char wallChar = (perpendicularWallDist < 1.8) ? '█' : (perpendicularWallDist < 3.2) ? '▓' : '▒';
            String finishedWallBlock = wallColor + wallChar + RESET;

            for (int y = 0; y < 22; y++) {
                int index = x + 80 * y;
                zBuffer[index] = inverseDepth;

                if (y < drawStart) {
                    char ceilChar = (y < 4) ? '█' : (y < 8) ? '▓' : '▒';
                    outputBuffer[index] = COLOR_CEILING + ceilChar + RESET;
                } else if (y >= drawStart && y <= drawEnd) {
                    // If the ray hit the fog threshold (-1), paint void
                    if (side == -1) {
                        outputBuffer[index] = " ";
                    } else {
                        outputBuffer[index] = finishedWallBlock;
                    }
                } else {
                    char floorChar = (x % 6 == 0 || y % 4 == 0) ? '·' : ' ';
                    outputBuffer[index] = COLOR_FLOOR + floorChar + RESET;
                }
            }
        }
    }
}
