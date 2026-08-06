import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GaltonBoardLoader extends Loader {
    private static final StatusStage[] GALTON_STAGES = {
        new StatusStage(25, "Constructing high-density peg matrix arrays:"),
        new StatusStage(50, "Calibrating macro binomial distribution paths:"),
        new StatusStage(75, "Instantiating 2x4 Braille sub-pixel grids:"),
        new StatusStage(100, "Hyper-Resolution Gaussian Curve Stable!")
    };

    // Framework dimension boundaries [1]
    private final int width;
    private final int height;

    // Dynamic Board State Parameters (No longer static constants)
    private final int pegRows;
    private final int numBins; 
    private final int[] binPixelHeights;
    private final int startChannelColumn;

    private static class Bead {
        int row;       
        int pegIndex;  
        double subFrame; 

        Bead() {
            this.row = 0;
            this.pegIndex = 0;
            this.subFrame = 0.0;
        }
    }

    private final List<Bead> activeBeads = new ArrayList<>();
    private final Random rand = new Random();
    private long lastSpawnTime = 0;

    // TrueColor ANSI color palette configuration
    private static final String COLOR_PEG      = "\u001B[38;2;120;130;140m"; 
    private static final String COLOR_BEAD     = "\u001B[38;2;255;110;20m";  
    private static final String COLOR_BIN      = "\u001B[38;2;50;80;110m";   
    private static final String COLOR_PILE     = "\u001B[38;2;240;160;30m";  

    // Primary Default Config Constructor [1]
    public GaltonBoardLoader() {
        this(GALTON_STAGES, 150, 38);
    }

    // Dynamic Layout Boundary Evaluation Constructor [1]
    public GaltonBoardLoader(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;

        // 1. CALCULATE DYNAMIC PEG FIELD CAPACITY (Top half of screen)
        // Reserve rows 1-2 for the hopper funnel and give the peg board ~60% of vertical depth
        this.pegRows = (int) Math.floor((height - 3) * 0.60);

        // 2. CALCULATE DYNAMIC COLLECTION BINS BOUNDARIES (Bottom half of screen)
        // Ensure the bin quantity is an odd integer to preserve a central alignment column anchor [1]
        int rawBinCapacity = (int) Math.floor(width * 0.20); // Scale horizontal span to 20% of width
        if (rawBinCapacity % 2 == 0) {
            rawBinCapacity--; // Subtract 1 to force odd parity [1]
        }
        this.numBins = Math.max(3, rawBinCapacity); // Clamp to a minimum threshold of 3 bins [1]
        
        // 3. ALLOCATE THE GRAPH ARRAYS NATIVELY
        this.binPixelHeights = new int[this.numBins];
        
        // Center the bin group horizontally on the screen matrix [1]
        int centerH = width / 2;
        this.startChannelColumn = centerH - (this.numBins / 2);
    }

    @Override
    protected void initialize() {
        activeBeads.clear();
        for (int i = 0; i < numBins; i++) {
            binPixelHeights[i] = 0;
        }
        lastSpawnTime = System.currentTimeMillis();

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        long currentTime = System.currentTimeMillis();
        int centerH = width / 2;

        // Dynamic floor receptacle row boundary markers based on total canvas height limits [1]
        int binTopY = 3 + pegRows + 2; // Leave a 2-row gap below the pegs
        int binBottomY = height - 1;   // Anchor directly to the bottom frame ceiling row
        int maxCellHeightsCount = binBottomY - binTopY + 1;
        int maxPixelHeightCap = maxCellHeightsCount * 4; // 4 sub-pixels per Braille row cell

        // 1. DYNAMIC PARTICLE SPAWNER
        if (currentTime - lastSpawnTime >= 60 && activeBeads.size() < 120) {
            activeBeads.add(new Bead()); 
            lastSpawnTime = currentTime;
        }

        // 2. PROCEDURAL COIN-FLIP TIMELINE PROGRESSION
        for (int i = activeBeads.size() - 1; i >= 0; i--) {
            Bead b = activeBeads.get(i);
            b.subFrame += 0.22; 

            if (b.subFrame >= 1.0) {
                b.subFrame = 0.0;
                
                if (b.row < pegRows - 1) {
                    if (rand.nextBoolean()) {
                        b.pegIndex++; 
                    }
                    b.row++; 
                } else {
                    // Map the horizontal index out across our back-to-back bin slots
                    double ratio = (double) b.pegIndex / (pegRows - 1);
                    int targetBin = (int) Math.round(ratio * (numBins - 1));
                    
                    if (targetBin >= 0 && targetBin < numBins) {
                        binPixelHeights[targetBin]++;

                        // --- THE INFINITE RUNTIME AUTO-SCALING TRIGGER ---
                        // Check if the newly added pixel pushes this bin over the maximum ceiling limit
                        if (binPixelHeights[targetBin] >= maxPixelHeightCap) {
                            
                            // Proportionally truncate EVERY bin by 25% down to preserve relative curves
                            for (int k = 0; k < numBins; k++) {
                                // Math.floor prevents fractional trailing bits from corrupting the integers
                                binPixelHeights[k] = (int) Math.floor(binPixelHeights[k] * 0.75);
                            }
                        }
                    }
                    activeBeads.remove(i);
                }

            }
        }

        // 3. DRAW STRUCTURAL CEILING HOPPER FUNNEL (Rows 1-2) [1]
        for (int x = centerH - 12; x <= centerH + 12; x++) {
            if (Math.abs(x - centerH) >= 5) {
                outputBuffer[x + width * 1] = COLOR_BIN + "▬" + RESET;
            }
            if (Math.abs(x - centerH) == 1) {
                outputBuffer[x + width * 2] = COLOR_BIN + "█" + RESET;
            }
        }

        // 4. DRAW EQUILATERAL PEG FIELD MATRIX GRID (Rows 3 to 3+pegRows) [1]
        double pegSpacingX = 2.0;
        for (int r = 0; r < pegRows; r++) {
            int y = 3 + r;
            double firstPegX = centerH - (r * pegSpacingX * 0.5);

            for (int p = 0; p <= r; p++) {
                int x = (int) Math.round(firstPegX + p * pegSpacingX);
                if (x >= 0 && x < width) {
                    outputBuffer[x + width * y] = COLOR_PEG + "·" + RESET;
                }
            }
        }

        // 5. DRAW STORAGE RECEPTACLES & HYPER-RESOLUTION BRAILLE PILES [1]
        outputBuffer[(startChannelColumn - 1) + width * binTopY] = COLOR_BIN + "│" + RESET;
        outputBuffer[(startChannelColumn + numBins) + width * binTopY] = COLOR_BIN + "│" + RESET;

        for (int bIdx = 0; bIdx < numBins; bIdx++) {
            int centerCol = startChannelColumn + bIdx;
            
            if (centerCol >= 2 && centerCol < width - 2) {
                for (int y = binTopY; y <= binBottomY; y++) {
                    if (bIdx == 0) outputBuffer[(centerCol - 1) + width * y] = COLOR_BIN + "│" + RESET;
                    if (bIdx == numBins - 1) outputBuffer[(centerCol + 1) + width * y] = COLOR_BIN + "│" + RESET;
                }

                // INVERTED 2X4 BOTTOM-TO-TOP BRAILLE CONVERTER
                int totalPixels = binPixelHeights[bIdx];

                for (int cellY = 0; cellY < maxCellHeightsCount; cellY++) {
                    int drawY = binBottomY - cellY; 
                    
                    int cellPixelStart = cellY * 4;
                    int pixelsInThisCell = totalPixels - cellPixelStart;

                    if (pixelsInThisCell > 0) {
                        if (pixelsInThisCell >= 4) {
                            outputBuffer[centerCol + width * drawY] = COLOR_PILE + "⣿" + RESET; 
                        } else {
                            int brailleHexOffset = 0;
                            if (pixelsInThisCell >= 1) brailleHexOffset |= (64 | 128); 
                            if (pixelsInThisCell >= 2) brailleHexOffset |= (64 | 128 | 4 | 32);  
                            if (pixelsInThisCell >= 3) brailleHexOffset |= (64 | 128 | 4 | 32 | 2 | 16); 
                            
                            char brailleChar = (char) (0x2800 + brailleHexOffset);
                            outputBuffer[centerCol + width * drawY] = COLOR_PILE + brailleChar + RESET;
                        }
                    }
                }
            }
        }

        // 6. OVERLAY PROCEDURAL INTERPOLATED MOVING BEADS [1]
        for (Bead b : activeBeads) {
            double currentFirstX = centerH - (b.row * pegSpacingX * 0.5);
            double currentX = currentFirstX + b.pegIndex * pegSpacingX;
            double currentY = 3.0 + b.row;

            if (b.row < pegRows - 1) {
                double nextFirstX = centerH - ((b.row + 1) * pegSpacingX * 0.5);
                double nextXLeft = nextFirstX + b.pegIndex * pegSpacingX;
                double nextXRight = nextFirstX + (b.pegIndex + 1) * pegSpacingX;
                double nextX = (nextXLeft + nextXRight) * 0.5; 
                
                currentX = currentX * (1.0 - b.subFrame) + nextX * b.subFrame;
                currentY = currentY * (1.0 - b.subFrame) + (currentY + 1.0) * b.subFrame;
            } else {
                currentY = currentY * (1.0 - b.subFrame) + binTopY * b.subFrame;
            }

            int sx = (int) Math.round(currentX);
            int sy = (int) Math.round(currentY);

            if (sy < height && sy >= 0 && sx >= 0 && sx < width) {
                outputBuffer[sx + width * sy] = COLOR_BEAD + "o" + RESET;
            }
        }
    }
}
