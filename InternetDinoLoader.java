// TODO: Add visual background elements?  Maybe day/night cycle?

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InternetDinoLoader extends InteractiveLoader {
    private static final StatusStage[] DINO_STAGES = {
        new StatusStage(100, "[Jump to avoid obstacles!]")
    };

    private static final int WIDTH = 110;
    private static final int HEIGHT = 30;
    private static final int GROUND_Y = HEIGHT - 4;

    private volatile int gameState = 0;
    private int inputCooldown = 0;
    private int restartTimer = 0;

    private volatile double dinoY = GROUND_Y;
    private volatile double dinoVelocity = 0.0;
    private static final double GRAVITY = 0.022;    
    private static final double JUMP_IMPULSE = -0.65; 
    private static final double SCROLL_SPEED = 0.45;  

    private int score = 0;
    private int highScore = 0;

    private static class Cactus {
        double posX;      
        int widthSize;   
        int height;       
        boolean scoreClaimed = false;
    }

    private final List<Cactus> cacti = new ArrayList<>();
    private int obstacleSpawnTimer = 0;

    private static final String COLOR_BG = "\u001B[38;2;247;247;247m";     
    private static final String COLOR_DINO = "\u001B[38;2;83;83;83m";       
    private static final String COLOR_DINO_HIT = "\u001B[38;2;240;80;80m";  
    private static final String COLOR_CACTUS = "\u001B[38;2;115;115;115m";  
    private static final String COLOR_GROUND = "\u001B[38;2;210;210;210m";  
    private static final String COLOR_TEXT = "\u001B[38;2;83;83;83m";      

    private final Random random = new Random();

    public InternetDinoLoader() {
        super(DINO_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void onInitialize() {
        resetGame();
        this.highScore = 0;
    }

    private void resetGame() {
        this.dinoY = GROUND_Y;
        this.dinoVelocity = 0.0;
        this.score = 0;
        this.gameState = 0;
        this.inputCooldown = 0;
        this.restartTimer = 0;
        this.cacti.clear();
        this.obstacleSpawnTimer = 30; 
    }

    @Override
    protected void handleKeyInput(int keyCode) {
        if (inputCooldown > 0) return;
        if (gameState == 1) return;

        if (dinoY >= GROUND_Y) {
            dinoVelocity = JUMP_IMPULSE;
            inputCooldown = 4; 
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        Arrays.fill(zBuffer, 0.0);
        for (int i = 0; i < outputBuffer.length; i++) {
            outputBuffer[i] = COLOR_BG + " " + RESET;
        }

        if (inputCooldown > 0) inputCooldown--;

        if (gameState == 0) {
            dinoVelocity += GRAVITY;
            dinoY += dinoVelocity;

            if (dinoY >= GROUND_Y) {
                dinoY = GROUND_Y;
                dinoVelocity = 0.0;
            }

            for (int i = cacti.size() - 1; i >= 0; i--) {
                Cactus c = cacti.get(i);
                c.posX -= SCROLL_SPEED;

                if (!c.scoreClaimed && c.posX < 22) {
                    score++;
                    if (score > highScore) highScore = score;
                    c.scoreClaimed = true;
                }

                if (c.posX < -5) {
                    cacti.remove(i);
                }
            }

            obstacleSpawnTimer--;
            if (obstacleSpawnTimer <= 0) {
                Cactus newCactus = new Cactus();
                newCactus.posX = WIDTH;
                newCactus.widthSize = 1 + random.nextInt(2); 
                newCactus.height = 3 + random.nextInt(2);    
                cacti.add(newCactus);
                obstacleSpawnTimer = 75 + random.nextInt(50);
            }

            int dLeft = 22;  
            int dRight = 25;
            int dBottomY = (int) Math.round(dinoY) + 1;

            for (Cactus c : cacti) {
                int cLeft = (int) Math.round(c.posX);
                int cRight = cLeft + (c.widthSize * 2) - 1;

                if (dRight >= cLeft && dLeft <= cRight) {
                    int cactusTopY = GROUND_Y - c.height;
                    
                    if (dBottomY >= cactusTopY) {
                        triggerCrash();
                    }
                }
            }
        } else {
            restartTimer--;
            if (restartTimer <= 0) {
                resetGame();
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            int bufferIdx = x + WIDTH * GROUND_Y;
            outputBuffer[bufferIdx] = COLOR_GROUND + "▄" + RESET;
            if (x % 14 == 0) {
                outputBuffer[x + WIDTH * (GROUND_Y + 1)] = COLOR_GROUND + "." + RESET;
            }
        }

        for (Cactus c : cacti) {
            int startX = (int) Math.round(c.posX);
            for (int w = 0; w < c.widthSize; w++) {
                int baseIdx = startX + (w * 2); 
                for (int h = 0; h < c.height; h++) {
                    int drawX = baseIdx;
                    int drawY = (GROUND_Y - 1) - h;

                    if (drawX < 0 || drawX >= WIDTH || drawY < 0 || drawY >= HEIGHT) continue;

                    char segmentSymbol = (h == c.height - 1) ? '╪' : '║';
                    outputBuffer[drawX + WIDTH * drawY] = COLOR_CACTUS + segmentSymbol + RESET;
                }
            }
        }

        int dinoScreenX = 22;
        int dinoScreenY = (int) Math.round(dinoY);

        boolean blinkHideFrame = (gameState == 1) && (restartTimer % 6 < 3);
        String currentDinoColor = (gameState == 1) ? COLOR_DINO_HIT : COLOR_DINO;

        if (!blinkHideFrame && dinoScreenY >= 3) {
            outputBuffer[dinoScreenX + 2 + WIDTH * (dinoScreenY - 2)] = currentDinoColor + "█" + RESET;
            outputBuffer[dinoScreenX + 3 + WIDTH * (dinoScreenY - 2)] = currentDinoColor + "▄" + RESET;

            outputBuffer[dinoScreenX + 1 + WIDTH * (dinoScreenY - 1)] = currentDinoColor + "█" + RESET;
            outputBuffer[dinoScreenX + 2 + WIDTH * (dinoScreenY - 1)] = currentDinoColor + "╪" + RESET;

            outputBuffer[dinoScreenX + WIDTH * dinoScreenY] = currentDinoColor + "◥" + RESET; 
            outputBuffer[dinoScreenX + 1 + WIDTH * dinoScreenY] = currentDinoColor + "█" + RESET; 

            if (gameState == 0 && dinoScreenY == GROUND_Y && (System.currentTimeMillis() / 120 % 2 == 0)) {
                outputBuffer[dinoScreenX + 1 + WIDTH * (dinoScreenY + 1)] = currentDinoColor + "┘" + RESET;
            } else {
                outputBuffer[dinoScreenX + 1 + WIDTH * (dinoScreenY + 1)] = currentDinoColor + "┴" + RESET;
            }
        }

        String scoreMessage = "HI " + String.format("%05d", highScore) + "  " + String.format("%05d", score);
        if (gameState == 1) {
            scoreMessage = " G A M E   O V E R ";
        }

        int targetX = (WIDTH - scoreMessage.length()) / 2;
        int targetY = 2; 

        for (int i = 0; i < scoreMessage.length(); i++) {
            int bufferIdx = (targetX + i) + WIDTH * targetY;
            if (bufferIdx >= 0 && bufferIdx < outputBuffer.length) {
                outputBuffer[bufferIdx] = COLOR_TEXT + scoreMessage.charAt(i) + RESET;
            }
        }
    }

    private void triggerCrash() {
        this.gameState = 1; 
        this.restartTimer = 50; 
        this.dinoVelocity = 0.0;
    }
}
