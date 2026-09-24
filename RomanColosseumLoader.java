// TODO: Make this more realistic to the real life building

public class RomanColosseumLoader extends Loader {
    private static final StatusStage[] COLOSSEUM_STAGES = {
            new StatusStage(25, "Pouring Roman concrete foundations:"),
            new StatusStage(50, "Constructing the vaulted travertine arcades:"),
            new StatusStage(75, "Excavating the hypogeum and arena floor:"),
            new StatusStage(100, "Colosseum of Rome Operational!")
    };

    // Typography for Roman masonry and structure
    private static final char CH_WALL = '\u2588';   // █ Solid travertine blocks
    private static final char CH_SHADOW = '\u2593'; // ▓ Deep recessed shadows inside arches
    private static final char CH_SEAT = '\u2592';   // ▒ Tiered marble seating (cavea)
    private static final char CH_ARENA = '\u2591';  // ░ Sandy arena floor

    // Travertine & Roman Brick Palette (Warm tans and weathered grays)
    private static final int[] STONE_BASE = { 210, 190, 160 }; 
    private static final int[] STONE_SHD = { 110, 90, 75 };    

    // Imperial Sunset Gradient (Deep purple fading down to golden orange)
    private static final int[] SKY_TOP = { 35, 15, 55 };
    private static final int[] SKY_BOTTOM = { 220, 90, 40 };

    private double rotationY = 0.0;
    private static final double CAMERA_DISTANCE = 7.5; 

    public RomanColosseumLoader() {
        super(COLOSSEUM_STAGES, 80, 22);
    }

    @Override
    protected void initialize() {
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Smooth, constant majestic orbit. No zoom required as the 
        // asymmetrical shape provides natural dynamic visual interest.
        rotationY += 0.015;

        // Fixed downward tilt to look into the hollow bowl of the amphitheater
        double tiltX = 0.45; 

        double cosX = Math.cos(tiltX);
        double sinX = Math.sin(tiltX);
        double cosY = Math.cos(rotationY);
        double sinY = Math.sin(rotationY);

        // 1. RENDER FULL CANVAS SKY GRADIENT (Sunset)
        for (int yp = 0; yp < 22; yp++) {
            double skyGrad = (double) yp / 21.0;
            int sr = (int) (SKY_TOP[0] * (1.0 - skyGrad) + SKY_BOTTOM[0] * skyGrad);
            int sg = (int) (SKY_TOP[1] * (1.0 - skyGrad) + SKY_BOTTOM[1] * skyGrad);
            int sb = (int) (SKY_TOP[2] * (1.0 - skyGrad) + SKY_BOTTOM[2] * skyGrad);

            String skyColor = String.format("\u001B[38;2;%d;%d;%dm", sr, sg, sb);
            for (int xp = 0; xp < 80; xp++) {
                outputBuffer[xp + 80 * yp] = skyColor + " " + RESET;
            }
        }

        // 2. COLOSSEUM ELLIPTICAL GEOMETRY
        double rxOuter = 2.5;  // Ellipse X radius
        double rzOuter = 1.9;  // Ellipse Z radius
        double rxInner = 1.3;  
        double rzInner = 0.9;

        // Sweep polar coordinates around the ellipse
        for (double theta = 0; theta < 2 * Math.PI; theta += 0.015) {
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);

            // --- THE RUIN PROFILE ---
            // The Colosseum is famously destroyed on its southern side.
            // We use a cosine wave offset to calculate which side is high vs low.
            double ruinFactor = Math.cos(theta - 1.2);
            double maxY;
            if (ruinFactor > 0.1) {
                maxY = 1.4; // Intact 4 stories
            } else if (ruinFactor < -0.2) {
                maxY = 0.25; // Heavily ruined down to the 2nd tier
            } else {
                // Steep jagged transition connecting the high and low walls
                maxY = 0.25 + ((ruinFactor + 0.2) / 0.3) * 1.15;
            }

            // --- A. OUTER FAÇADE (With Arches) ---
            for (double y = -0.8; y <= maxY; y += 0.025) {
                double x = rxOuter * cosT;
                double z = rzOuter * sinT;

                // Boolean Architecture: Calculate if current point is hollow air (an archway)
                int tier = (int) Math.floor((y + 0.8) / 0.55);
                double tierY = (y + 0.8) % 0.55;
                
                // 40 arches wrapping around the perimeter
                double archFrequency = Math.cos(theta * 40); 
                
                // Arches only exist on the first 3 tiers, don't cut through the floors, and have a defined width
                boolean isArch = (tier < 3) && (tierY < 0.40) && (archFrequency > 0.25);

                if (!isArch) {
                    // Solid outer wall or pillars
                    renderProjectedPoint(x, y, z, cosT, 0, sinT, CH_WALL, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                } else {
                    // Inner structural wall visible through the archway (recessed inward)
                    double recess = 0.2;
                    renderProjectedPoint(x - recess * cosT, y, z - recess * sinT, cosT, 0, sinT, CH_SHADOW, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            }

            // --- B. INNER SEATING (Cavea) ---
            // Slopes downward from the outer wall to the inner arena edge
            double seatMaxY = Math.min(maxY, 0.5); // Seating doesn't go all the way to the 4th tier roof
            for (double r = 0.0; r <= 1.0; r += 0.03) {
                double x = (rxInner + r * (rxOuter - rxInner)) * cosT;
                double z = (rzInner + r * (rzOuter - rzInner)) * sinT;
                
                // Linear slope calculating the height of the seats at this radius
                double y = -0.8 + r * 1.3; 
                
                if (y <= seatMaxY) {
                    // Alternate characters slightly to give the texture of stepped rows
                    char seatChar = ((int)(r * 40) % 2 == 0) ? CH_SEAT : CH_WALL;
                    
                    // Normal vector pointing UP and INWARD for the seating
                    renderProjectedPoint(x, y, z, -cosT, 1.0, -sinT, seatChar, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
                }
            }

            // --- C. ARENA FLOOR ---
            for (double r = 0.0; r <= 1.0; r += 0.08) {
                double x = (rxInner * r) * cosT;
                double z = (rzInner * r) * sinT;
                renderProjectedPoint(x, -0.82, z, 0, 1, 0, CH_ARENA, cosX, sinX, cosY, sinY, outputBuffer, zBuffer);
            }
        }
    }

    private void renderProjectedPoint(double x, double y, double z, double nx, double ny, double nz, char renderChar, 
            double cosX, double sinX, double cosY, double sinY, String[] outputBuffer, double[] zBuffer) {
        
        // Rotate point
        double xSpun = x * cosY + z * sinY;
        double ySpun = y;
        double zSpun = -x * sinY + z * cosY;
        
        double rx = xSpun;
        double ry = ySpun * cosX - zSpun * sinX;
        double rz = ySpun * sinX + zSpun * cosX;
        
        // Rotate normals (for lighting)
        double nxSpun = nx * cosY + nz * sinY;
        double nzSpun = -nx * sinY + nz * cosY;
        double nry = ny * cosX - nzSpun * sinX;
        
        double ooz = 1.0 / (rz + CAMERA_DISTANCE);
        int xp = (int) (40 + 54 * ooz * rx * 1.95);
        int yp = (int) (11 + 25 * ooz * ry);
        
        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.00005) {
                zBuffer[index] = ooz;
                
                // Directional Lighting based on surface normal (Light coming from top-left sunset)
                double lightIntensity = Math.max(0.0, nxSpun * -0.6 + nry * 0.5 + nzSpun * 0.4);
                double ambient = 0.35;
                double intensity = ambient + (1.0 - ambient) * lightIntensity;
                
                int r = (int) (STONE_SHD[0] * (1.0 - intensity) + STONE_BASE[0] * intensity);
                int g = (int) (STONE_SHD[1] * (1.0 - intensity) + STONE_BASE[1] * intensity);
                int b = (int) (STONE_SHD[2] * (1.0 - intensity) + STONE_BASE[2] * intensity);
                
                // Hard-coded shadows inside the archways
                if (renderChar == CH_SHADOW) {
                    r = (int)(r * 0.4);
                    g = (int)(g * 0.4);
                    b = (int)(b * 0.45); // slight blueish shadow cast
                } else if (renderChar == CH_ARENA) {
                    r = (int)(r * 1.1);
                    g = (int)(g * 1.0);
                    b = (int)(b * 0.8);
                }
                
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                
                outputBuffer[index] = String.format("\u001B[38;2;%d;%d;%dm", r, g, b) + renderChar + RESET;
            }
        }
    }
}