// TODO: The ! are not lined up... rotate the faces properly

public class TexelMarioCapBlockLoader extends TexelCubeLoader { 
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] { 
        new StatusStage(15, "Entering Castle Grounds:"), 
        new StatusStage(35, "Draining the Moat:"), 
        new StatusStage(55, "Chasing Mips:"), 
        new StatusStage(75, "Activating Cap Switches:"), 
        new StatusStage(90, "Collecting Red Coins:"), 
        new StatusStage(100, "Here We Go!:") 
    }; 

    private int baseR, baseG, baseB;
    private int highR, highG, highB;
    private int darkR, darkG, darkB;

    public TexelMarioCapBlockLoader() { 
        super(TEXEL_CUBE_STAGES, 80, 22); 
    } 

    @Override
    protected void initialize() {
        // Correct initialization hook to safely seed the block color variables
        initializePalette();
    }

    private void initializePalette() {
        int blockType = (int) (Math.random() * 4);

        switch(blockType) {
            case 0: // Red Block (Wing Cap)
                baseR = 210; baseG = 25;  baseB = 25;
                highR = 255; highG = 110; highB = 110; 
                darkR = 120; darkG = 10;  darkB = 10;
                break;
            case 1: // Green Block (Metal Cap)
                baseR = 25;  baseG = 165; baseB = 40;
                highR = 120; highG = 240; highB = 135; 
                darkR = 5;   darkG = 85;  darkB = 10;
                break;
            case 2: // Blue Block (Vanish Cap)
                baseR = 25;  baseG = 65;  baseB = 220;
                highR = 115; highG = 150; highB = 255; 
                darkR = 5;   darkG = 15;  darkB = 115;
                break;
            case 3: // Yellow Block (Standard Item Box)
            default:
                baseR = 235; baseG = 165; baseB = 10;
                highR = 255; highG = 230; highB = 130; 
                darkR = 140; darkG = 80;  darkB = 0;
                break;
        }
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) { 
        // --- 1. COORDINATE RE-ALIGNMENT FOR SIDE FACES ---
        // Converts raw engine mapping inputs into a standard canvas where:
        // (x=0, y=0) is Top-Left and (x=15, y=15) is Bottom-Right relative to gravity.
        int targetX = x;
        int targetY = y;
        
        switch (face) {
            case 0: // Engine Back Face
                targetX = 15 - x;
                targetY = 15 - y;
                break;
            case 1: // Engine Front Face
                // Naturally matches standard top-to-bottom layout orientation
                break;
            case 4: // Engine Left Face
                targetX = y;
                targetY = 15 - x;
                break;
            case 5: // Engine Right Face
                targetX = 15 - y;
                targetY = x;
                break;
            case 2: // Engine Bottom Face
            case 3: // Engine Top Face
            default:
                break;
        }

        int noise = (int) (Math.abs((targetX * 34211L + targetY * 12473L + face * 4567L) ^ 0x5DEECE66DL) % 3); 

        // --- 2. CONTINUOUS CHECKERBOARD SEAM ALIGNMENT ---
        // Determines if a coordinate is on a border row/column
        boolean isBorder = (targetX <= 1 || targetX >= 14 || targetY <= 1 || targetY >= 14);

        if (isBorder) {
            // Uses global spatial symmetries instead of simple division to ensure that when 
            // faces fold together at their joints, the 2x2 checks meet continuously.
            int checkX = (targetX >= 14) ? (15 - targetX) : targetX;
            int checkY = (targetY >= 14) ? (15 - targetY) : targetY;
            
            boolean isHighCheck = ((checkX / 2 + checkY / 2) % 2 == 0);
            if (isHighCheck) {
                return new VoxelTexel(highR, highG, highB, '\u2588'); // Highlight Tint Check (█)
            } else {
                return new VoxelTexel(baseR + noise * 4, baseG + noise * 4, baseB, '\u2593'); // Base Color Check (▓)
            }
        }

        // --- 3. EXCLAMATION POINT (!) ORIENTATION MATH ---
        boolean isExclamationStem = false;
        boolean isExclamationDot = false;

        // Render exclusively on the 4 vertical sides (0 = Back, 1 = Front, 4 = Left, 5 = Right)
        if (face == 0 || face == 1 || face == 4 || face == 5) {
            // Realigned math points all exclamation marks downward toward targetY = 15
            if (targetX >= 7 && targetX <= 8) {
                if (targetY >= 4 && targetY <= 8) {
                    isExclamationStem = true; 
                }
                if (targetY == 10 || targetY == 11) {
                    isExclamationDot = true;  
                }
            }
        }

        // --- 4. TEXTURE LAYER RASTERIZATION --- 
        if (isExclamationStem || isExclamationDot) {
            return new VoxelTexel(255, 255, 255, '\u2588'); // Pure White Symbol (█)
        }

        // Apply a subtle directional bottom shadow to inside panels for a depth effect
        if (targetX == 13 || targetY == 13) {
            return new VoxelTexel(darkR + noise * 3, darkG + noise * 3, darkB, '\u2592'); // Bevel Recess (▒)
        }

        return new VoxelTexel(baseR + noise * 6, baseG + noise * 4, baseB, '\u2593'); // Main Casing Field (▓)
    } 
}
