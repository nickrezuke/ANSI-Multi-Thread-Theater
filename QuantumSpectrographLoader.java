public class QuantumSpectrographLoader extends Loader {

    private static final StatusStage[] STAGES = {
        new StatusStage(25, "Mapping discrete spatial eigenvalues:"),
        new StatusStage(55, "Integrating Time-Dependent Schrödinger phase:"),
        new StatusStage(80, "Verifying probability conservation laws:"),
        new StatusStage(100, "Quantum Telemetry Streams Normalized!")
    };

    // Strict layout boundaries
    private static final int RENDER_W = 54;
    private static final int TOTAL_W = 80;
    private static final int HEIGHT = 22;

    // Simulation parameters
    private double time = 0.0;
    private static final double DT = 0.08;
    private static final int MODES = 4;
    
    private final double[] amplitudes = {0.30, 0.75, 0.55, 0.20};
    private final double[] energies = new double[MODES];

    // Real-time telemetry data variables
    private double expectationX = 0.0;
    private double uncertaintyX = 0.0;
    private double normalizationIntegral = 1.0000;
    private double totalEnergyEv = 0.0;

    // Corrected constructor passing exact dimension footprints down to parent framework
    public QuantumSpectrographLoader() {
        super(STAGES, TOTAL_W, HEIGHT);
    }

    @Override
    protected void initialize() {
        time = 0.0;
        for (int n = 0; n < MODES; n++) {
            int principalNumber = n + 1;
            energies[n] = 0.10 * (principalNumber * principalNumber);
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        int maxIndex = TOTAL_W * HEIGHT;

        // Clear terminal output matrix slices safely within absolute limit allocations
        for (int i = 0; i < maxIndex; i++) {
            outputBuffer[i] = " ";
            zBuffer[i] = 0.0;
        }

        time += DT;

        double[] probDensityProfile = new double[RENDER_W];
        double[] realComponentProfile = new double[RENDER_W];
        
        double normSum = 0.0;
        double xSum = 0.0;
        double xSquaredSum = 0.0;

        for (int x = 0; x < RENDER_W; x++) {
            double u = (double) x / (RENDER_W - 1); 

            double psiRe = 0.0;
            double psiIm = 0.0;

            for (int n = 0; n < MODES; n++) {
                int principalNumber = n + 1;
                double spatialBasis = Math.sin(principalNumber * Math.PI * u);
                double phaseAngle = energies[n] * time;

                psiRe += amplitudes[n] * spatialBasis * Math.cos(phaseAngle);
                psiIm -= amplitudes[n] * spatialBasis * Math.sin(phaseAngle);
            }

            double probDensity = (psiRe * psiRe) + (psiIm * psiIm);
            probDensityProfile[x] = probDensity;
            realComponentProfile[x] = psiRe;

            normSum += probDensity;
            xSum += u * probDensity;
            xSquaredSum += u * u * probDensity;
        }

        normalizationIntegral = normSum / RENDER_W * 1.65; 
        expectationX = (xSum / normSum) * RENDER_W;       
        double expectedX2 = xSum / normSum;
        double expectedXSquared = xSquaredSum / normSum;
        uncertaintyX = Math.sqrt(Math.abs(expectedXSquared - (expectedX2 * expectedX2))) * 10.0;
        totalEnergyEv = 2.18; 

        // 3. SAFE BOUNDS VECTOR PLOTTING
        for (int x = 0; x < RENDER_W; x++) {
            // Plot Upper Graph: Probability Density (Bounded strictly between row 1 and row 8)
            double rawValueTop = probDensityProfile[x] * 8.5;
            int graphYTop = 8 - (int) Math.min(7, Math.max(0, rawValueTop));
            int idxTop = x + TOTAL_W * graphYTop;
            if (idxTop >= 0 && idxTop < maxIndex) {
                outputBuffer[idxTop] = "\u001B[38;5;81m█" + RESET; 
            }

            // Plot Lower Graph: Real Wave Component (Bounded strictly between row 12 and row 19)
            double rawValueBottom = realComponentProfile[x] * 3.5;
            int graphYBottom = 16 - (int) Math.round(rawValueBottom);
            if (graphYBottom < 12) graphYBottom = 12;
            if (graphYBottom > 19) graphYBottom = 19;
            
            int idxBottom = x + TOTAL_W * graphYBottom;
            if (idxBottom >= 0 && idxBottom < maxIndex) {
                outputBuffer[idxBottom] = "\u001B[38;5;201m" + ((rawValueBottom >= 0) ? "⌃" : "⌄") + RESET; 
            }
        }

        // Draw structural baseline horizontal axes
        for (int x = 0; x < RENDER_W; x++) {
            outputBuffer[TOTAL_W * 9 + x] = "\u001B[38;5;242m─" + RESET;
            outputBuffer[TOTAL_W * 20 + x] = "\u001B[38;5;242m─" + RESET;
        }
        
        // Add Position Expectation Value marker securely onto the graph baseline axis split
        int expXIdx = (int) Math.min(RENDER_W - 1, Math.max(0, expectationX));
        outputBuffer[TOTAL_W * 9 + expXIdx] = "\u001B[38;5;46m▲" + RESET;

        // Overlay static text labeling blocks safely
        writeInlineLabel(outputBuffer, 0, 1, "PROBABILITY DENSITY PROFILE |Ψ(x,t)|²", "\u001B[38;5;246m");
        writeInlineLabel(outputBuffer, 11, 1, "COMPLEX WAVE REAL COMPONENT Re(Ψ)", "\u001B[38;5;246m");

        // 4. INSTRUMENT PANEL DATA OVERLAYS
        String white = "\u001B[38;5;255m";
        String gray = "\u001B[38;5;244m";
        String green = "\u001B[38;5;112m";

        drawDashboardLine(outputBuffer, 2,  "┌──────────────────────┐", white, white);
        drawDashboardLine(outputBuffer, 3,  "│  QUANTUM INSTRUMENT  │", white, white);
        drawDashboardLine(outputBuffer, 4,  "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 5,  String.format("│ %sBASIS MODES:  %s%4d   │", gray, green, MODES), gray, white);
        drawDashboardLine(outputBuffer, 6,  String.format("│ %sCONSERVED E: %s%5.2feV │", gray, green, totalEnergyEv), gray, white);
        drawDashboardLine(outputBuffer, 7,  String.format("│  %sWAVE TIME: %s%6.2f s │", gray, green, time), gray, white);
        drawDashboardLine(outputBuffer, 8,  "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 9,  "│ ANALYTICAL METRICS   │", white, white);
        drawDashboardLine(outputBuffer, 10, String.format("│ %s∫|Ψ|² dx:   %s%6.4f   │", gray, green, normalizationIntegral), gray, white);
        drawDashboardLine(outputBuffer, 11, String.format("│ %sEXPECT <x>: %s%6.2f Å │", gray, green, expectationX * 0.1), gray, white);
        drawDashboardLine(outputBuffer, 12, String.format("│ %sUNCTY Δx:   %s%6.2f Å │", gray, green, uncertaintyX * 0.1), gray, white);
        drawDashboardLine(outputBuffer, 13, "├──────────────────────┤", white, white);
        drawDashboardLine(outputBuffer, 14, "│ BOUNDARY PARAMETERS  │", white, white);
        drawDashboardLine(outputBuffer, 15, String.format("│ %sWELL POT:  %s 0.00 eV  │", gray, green), gray, white);
        drawDashboardLine(outputBuffer, 16, String.format("│ %sWALL POT:  %s   ∞  eV  │", gray, green), gray, white);
        drawDashboardLine(outputBuffer, 17, String.format("│ %sSPACE DIMS: %s 1D BOX  │", gray, green), gray, white);
        drawDashboardLine(outputBuffer, 18, "└──────────────────────┘", white, white);
    }

    private void writeInlineLabel(String[] outputBuffer, int row, int col, String label, String color) {
        int startIdx = row * TOTAL_W + col;
        for (int i = 0; i < label.length(); i++) {
            if (col + i >= RENDER_W) break;
            if (startIdx + i < outputBuffer.length) {
                outputBuffer[startIdx + i] = color + label.charAt(i) + RESET;
            }
        }
    }

    private void drawDashboardLine(String[] outputBuffer, int row, String content, String textColor, String wallColor) {
        int targetColumn = 56;
        int rowStartIdx = row * TOTAL_W;
        String cleanContent = content.replaceAll("\u001B\\[[;\\d]*m", "");
        int visualIndex = 0;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\u001B') {
                int endM = content.indexOf('m', i);
                if (endM != -1) { i = endM; continue; }
            }
            int currentCellX = targetColumn + visualIndex;
            if (currentCellX >= TOTAL_W) break;
            int targetBufferIndex = rowStartIdx + currentCellX;

            if (targetBufferIndex < 0 || targetBufferIndex >= outputBuffer.length) continue;

            String activeColor = (ch == '│' || ch == '┌' || ch == '┐' || ch == '├' || ch == '┤' || ch == '└' || ch == '┘' || ch == '─') ? wallColor : textColor;

            if (Character.isDigit(ch) || ch == '.' || ch == '-' || ch == '+' || ch == '∞') {
                if (!cleanContent.contains("INSTRUMENT") && visualIndex > 2 && visualIndex < 20) {
                    activeColor = "\u001B[38;5;112m"; 
                }
            }

            outputBuffer[targetBufferIndex] = activeColor + ch + RESET;
            visualIndex++;
        }
    }
}
