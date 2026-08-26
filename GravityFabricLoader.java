import java.util.Arrays;

public class GravityFabricLoader extends Loader {
    private static final StatusStage[] FABRIC_STAGES = {
        new StatusStage(25, "Weaving space-time fabric matrix..."),
        new StatusStage(50, "Calibrating central solar mass..."),
        new StatusStage(75, "Simulating planetary orbital tracking..."),
        new StatusStage(100, "Spacetime Gravity Well Active!")
    };

    private static final int WIDTH = 120;
    private static final int HEIGHT = 32;

    private double timeClock = 0.0;

    // Fixed pitch, but yaw will now rotate dynamically over time
    private final double pitchAngle = 0.36;   

    public GravityFabricLoader() {
        super(FABRIC_STAGES, WIDTH, HEIGHT);
    }

    @Override
    protected void initialize() {
        timeClock = 0.0;
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // 1. Clear canvas and reset tracking Z-buffer
        Arrays.fill(outputBuffer, " ");
        Arrays.fill(zBuffer, Double.MAX_VALUE);

        // Dynamic camera rotation around the Y-axis using timeClock
        double dynamicYaw = 0.45 - timeClock * 0.09;
        double cosP = Math.cos(pitchAngle), sinP = Math.sin(pitchAngle);
        double cosY = Math.cos(dynamicYaw), sinY = Math.sin(dynamicYaw);

        // Physics Masses
        double sunX = 0.0;
        double sunZ = 0.0;
        double sunMass = 12.0;

        double orbitRadius = 7.0;
        double planetX = orbitRadius * Math.cos(timeClock * 1.1);
        double planetZ = orbitRadius * Math.sin(timeClock * 1.1);
        double planetMass = 2.0;

        double zoomFactor = 2.5;

        // ====================================================================
        // PASS 1: THE SPACETIME FABRIC GRID (Rotating Wireframe)
        // ====================================================================
        int gridExtent = 75; 
        double stepSize = 0.4; 

        for (int xi = -gridExtent; xi <= gridExtent; xi++) {
            for (int zi = -gridExtent; zi <= gridExtent; zi++) {
                double gx = xi * stepSize;
                double gz = zi * stepSize;

                double distToSun = Math.sqrt((gx - sunX) * (gx - sunX) + (gz - sunZ) * (gz - sunZ));
                double distToPlanet = Math.sqrt((gx - planetX) * (gx - planetX) + (gz - planetZ) * (gz - planetZ));

                double sunWell = -sunMass / (distToSun * 0.35 + 0.9);
                double planetWell = -planetMass / (distToPlanet * 0.7 + 0.5);
                double gy = sunWell + planetWell + 3; // Flat +3 raise for visual clarity

                double rotX = gx * cosY - gz * sinY;
                double rotZ = gx * sinY + gz * cosY;
                double rotY = gy;

                double finalX = rotX;
                double finalY = (-rotY) * cosP - rotZ * sinP; 
                double finalZ = (-rotY) * sinP + rotZ * cosP; 

                int screenX = (int) (WIDTH / 2.0 + (zoomFactor * 2.2) * finalX);
                int screenY = (int) (HEIGHT / 2.0 + zoomFactor * finalY);

                if (screenX < 0 || screenX >= WIDTH || screenY < 0 || screenY >= HEIGHT) continue;

                int bufferIndex = screenX + WIDTH * screenY;
                if (finalZ < zBuffer[bufferIndex]) {
                    zBuffer[bufferIndex] = finalZ;

                    String ansiColor = "\u001B[38;5;33m";  // Electric Indigo (Wells)

                    boolean isLineX = (xi % 5 == 0);
                    boolean isLineZ = (zi % 5 == 0);
                    
                    char elementChar = (isLineX || isLineZ) ? '+' : '.';
                    outputBuffer[bufferIndex] = ansiColor + elementChar + RESET;
                }
            }
        }

        // ====================================================================
        // PASS 2: THE 3D SOLAR SPHERE CORE (Self-Illuminating Light Source)
        // ====================================================================
        double sunRadius = 1.8; 
        double sunCenterY = -0.1; 
        double sunStep = 0.08;

        for (double sx = -sunRadius; sx <= sunRadius; sx += sunStep) {
            for (double sy = -sunRadius; sy <= sunRadius; sy += sunStep) {
                for (double sz = -sunRadius; sz <= sunRadius; sz += sunStep) {
                    if (sx * sx + sy * sy + sz * sz <= sunRadius * sunRadius) {
                        double worldX = sunX + sx;
                        double worldY = sunCenterY + sy;
                        double worldZ = sunZ + sz;

                        double rotX = worldX * cosY - worldZ * sinY;
                        double rotZ = worldX * sinY + worldZ * cosY;
                        double rotY = worldY;

                        double finalX = rotX;
                        double finalY = (-rotY) * cosP - rotZ * sinP;
                        double finalZ = (-rotY) * sinP + rotZ * cosP;

                        int screenX = (int) (WIDTH / 2.0 + (zoomFactor * 2.2) * finalX);
                        int screenY = (int) (HEIGHT / 2.0 + zoomFactor * finalY);

                        if (screenX < 0 || screenX >= WIDTH || screenY < 0 || screenY >= HEIGHT) continue;

                        int bufferIndex = screenX + WIDTH * screenY;
                        
                        // Z-Buffer check to only render the front-facing hemisphere
                        if (finalZ < zBuffer[bufferIndex]) {
                            zBuffer[bufferIndex] = finalZ;

                            // Calculate normal vector of the sun's surface
                            double nx = sx / sunRadius;
                            double ny = sy / sunRadius;
                            double nz = sz / sunRadius;

                            // Rotate normal identically to geometry to find normal relative to camera
                            double rotNz = nx * sinY + nz * cosY;
                            double finalNz = (-ny) * sinP + rotNz * cosP;

                            // Exposure based purely on camera-facing angle (limb darkening) to create a glowing volume
                            double exposure = -finalNz; 
                            char glyph = (exposure > 0.8) ? '#' : (exposure > 0.5) ? 'O' : (exposure > 0.2) ? '*' : ':';
                            
                            outputBuffer[bufferIndex] = "\u001B[38;5;220m" + glyph + RESET; // Radiant Gold
                        }
                    }
                }
            }
        }

        // ====================================================================
        // PASS 3: THE 3D PLANET SPHERE CORE (Lit by the Sun)
        // ====================================================================
        double planetRadius = 0.5; 
        double planetCenterY = 0.1; 
        double planetStep = 0.05;

        for (double px = -planetRadius; px <= planetRadius; px += planetStep) {
            for (double py = -planetRadius; py <= planetRadius; py += planetStep) {
                for (double pz = -planetRadius; pz <= planetRadius; pz += planetStep) {
                    if (px * px + py * py + pz * pz <= planetRadius * planetRadius) {
                        double worldX = planetX + px;
                        double worldY = planetCenterY + py;
                        double worldZ = planetZ + pz;

                        double rotX = worldX * cosY - worldZ * sinY;
                        double rotZ = worldX * sinY + worldZ * cosY;
                        double rotY = worldY;

                        double finalX = rotX;
                        double finalY = (-rotY) * cosP - rotZ * sinP;
                        double finalZ = (-rotY) * sinP + rotZ * cosP;

                        int screenX = (int) (WIDTH / 2.0 + (zoomFactor * 2.2) * finalX);
                        int screenY = (int) (HEIGHT / 2.0 + zoomFactor * finalY);

                        if (screenX < 0 || screenX >= WIDTH || screenY < 0 || screenY >= HEIGHT) continue;

                        int bufferIndex = screenX + WIDTH * screenY;
                        
                        // Z-Buffer check prevents dark side of planet from overwriting the lit side
                        if (finalZ < zBuffer[bufferIndex]) {
                            zBuffer[bufferIndex] = finalZ;

                            // 1. Calculate direction vector FROM the planet TO the sun
                            double dirX = sunX - worldX;
                            double dirY = sunCenterY - worldY;
                            double dirZ = sunZ - worldZ;
                            double mag = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                            dirX /= mag;
                            dirY /= mag;
                            dirZ /= mag;

                            // 2. Normal vector of the planet's surface at this local point
                            double nx = px / planetRadius;
                            double ny = py / planetRadius;
                            double nz = pz / planetRadius;

                            // 3. Lambertian reflectance (dot product of normal and light direction)
                            double exposure = (nx * dirX) + (ny * dirY) + (nz * dirZ);
                            
                            // High exposure creates the daytime face, low exposure maps out the nighttime face
                            char glyph = (exposure > 0.3) ? '@' : (exposure > -0.2) ? '8' : '=';

                            outputBuffer[bufferIndex] = "\u001B[38;5;81m" + glyph + RESET; // Sky Blue
                        }
                    }
                }
            }
        }

        timeClock += 0.025;
    }
}