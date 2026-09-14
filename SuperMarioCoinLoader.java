public class SuperMarioCoinLoader extends Loader { 
    private static final StatusStage[] COIN_STAGES = { 
        new StatusStage(15, "Minting 8-bit gold polygons:"), 
        new StatusStage(40, "Carving rectangular central slot:"), 
        new StatusStage(65, "Enhancing specular coin shine:"), 
        new StatusStage(85, "Loading Mushroom Kingdom textures:"), 
        new StatusStage(100, "Super Mario Coin Active!") 
    }; 

    private static final int WIDTH = 80;
    private static final int HEIGHT = 32;
    
    private double yaw = 0; 
    
    public SuperMarioCoinLoader() { 
        super(COIN_STAGES, WIDTH, HEIGHT); 
    } 
    
    @Override 
    protected void initialize() { 
        this.yaw = 0; 
    } 
    
    @Override 
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) { 
        // Increased radius for a wider face, increased thickness for a chunkier coin
        double radius = 3.5; 
        double thickness = 0.9; 
        double halfThick = thickness / 2.0; 
        double pitch = Math.toRadians(15.0); 
        
        // Scaled up the slot proportionally to match the wider coin
        double slotWidth = 1.4; 
        double slotHeight = 4.2; 
        double slotDepth = 0.35; 
        double rimRecess = 0.12; 
        double innerRimRadius = radius * 0.82;
        
        // 1. Front and Back Faces
        for (int rIndex = 0; rIndex <= 80; rIndex++) { 
            double r = rIndex * (radius / 80.0); 
            
            for (int tIndex = 0; tIndex < 240; tIndex++) { 
                double theta = tIndex * (2.0 * Math.PI / 240.0); 
                double cosT = Math.cos(theta); 
                double sinT = Math.sin(theta); 
                double lx = r * cosT;
                double ly = r * sinT;
                
                boolean inSlot = Math.abs(lx) <= (slotWidth / 2.0) && Math.abs(ly) <= (slotHeight / 2.0);
                
                double faceZ; 
                double occlusion = 1.0; 
                
                if (inSlot) { 
                    faceZ = halfThick - rimRecess - slotDepth; 
                    occlusion = 0.35; 
                } else if (r < innerRimRadius) { 
                    faceZ = halfThick - rimRecess; 
                } else {
                    faceZ = halfThick;
                }
                
                renderPoint(lx, ly, faceZ, 0, 0, 1, yaw, pitch, occlusion, outputBuffer, zBuffer); 
                renderPoint(lx, ly, -faceZ, 0, 0, -1, yaw, pitch, occlusion, outputBuffer, zBuffer); 
                            
                if (rIndex == 65) { 
                    for(int wall = 0; wall <= 4; wall++) {
                        double wz = (halfThick - rimRecess) + (rimRecess * wall / 4.0);
                        renderPoint(lx, ly, wz, -cosT, -sinT, 0, yaw, pitch, 1.0, outputBuffer, zBuffer);
                        renderPoint(lx, ly, -wz, -cosT, -sinT, 0, yaw, pitch, 1.0, outputBuffer, zBuffer);
                    }
                }
            } 
        } 
        
        // 2. Rectangular Slot Walls
        for (int yStep = 0; yStep <= 60; yStep++) {
            double sy = -(slotHeight / 2.0) + slotHeight * (yStep / 60.0);
            for (int zStep = 0; zStep <= 6; zStep++) {
                double sz = (halfThick - rimRecess) - slotDepth * (zStep / 6.0);
                double hw = slotWidth / 2.0;
                
                renderPoint(hw, sy, sz, -1, 0, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
                renderPoint(-hw, sy, sz, 1, 0, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
                renderPoint(hw, sy, -sz, -1, 0, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
                renderPoint(-hw, sy, -sz, 1, 0, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
            }
        }
        
        for (int xStep = 0; xStep <= 25; xStep++) {
            double sx = -(slotWidth / 2.0) + slotWidth * (xStep / 25.0);
            for (int zStep = 0; zStep <= 6; zStep++) {
                double sz = (halfThick - rimRecess) - slotDepth * (zStep / 6.0);
                double hh = slotHeight / 2.0;
                
                renderPoint(sx, hh, sz, 0, -1, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
                renderPoint(sx, -hh, sz, 0, 1, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
                renderPoint(sx, hh, -sz, 0, -1, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
                renderPoint(sx, -hh, -sz, 0, 1, 0, yaw, pitch, 0.5, outputBuffer, zBuffer); 
            }
        }

        // 3. Outer Edge (Rim)
        for (int zIndex = 0; zIndex <= 30; zIndex++) { 
            double z = -halfThick + (thickness * zIndex / 30.0); 
            for (int tIndex = 0; tIndex < 240; tIndex++) { 
                double theta = tIndex * (2.0 * Math.PI / 240.0); 
                double cosT = Math.cos(theta); 
                double sinT = Math.sin(theta); 
                
                renderPoint(radius * cosT, radius * sinT, z, cosT, sinT, 0, yaw, pitch, 1.0, outputBuffer, zBuffer); 
            } 
        } 
        
        yaw += 0.08; 
    } 
    
    private void renderPoint(double lx, double ly, double lz, 
                             double nx, double ny, double nz, 
                             double currentYaw, double pitch, 
                             double occlusion, 
                             String[] outputBuffer, double[] zBuffer) { 
                                 
        double sinY = Math.sin(currentYaw), cosY = Math.cos(currentYaw); 
        double sinP = Math.sin(pitch), cosP = Math.cos(pitch); 
        
        // Point Rotations
        double rx1 = lx * cosY - lz * sinY; 
        double ry1 = ly; 
        double rz1 = lx * sinY + lz * cosY; 
        
        double rx = rx1; 
        double ry = ry1 * cosP - rz1 * sinP; 
        double rz = ry1 * sinP + rz1 * cosP; 
        
        // 3D Projection 
        double ooz = 1.0 / (rz + 12.0); 
        int x = (int) (WIDTH / 2 + 1.5 * WIDTH * ooz * rx); 
        int y = (int) (HEIGHT / 2 + HEIGHT * ooz * ry * 1.6); 
        int o = x + WIDTH * y; 
        
        if (y >= 0 && y < HEIGHT && x >= 0 && x < WIDTH && ooz > (zBuffer[o] + 0.0001)) { 
            zBuffer[o] = ooz; 
            
            // Normal Rotations
            double nrx1 = nx * cosY - nz * sinY; 
            double nry1 = ny; 
            double nrz1 = nx * sinY + nz * cosY; 
            
            double nrx = nrx1; 
            double nry = nry1 * cosP - nrz1 * sinP; 
            double nrz = nry1 * sinP + nrz1 * cosP; 
            
            double lx_light = 0.577, ly_light = -0.577, lz_light = 0.577; 
            double dotNL = (nrx * lx_light + nry * ly_light + nrz * lz_light); 
            double diffuseLight = Math.max(0.0, dotNL); 
            
            double hx = 0.325, hy = -0.325, hz = 0.888; 
            double dotNH = (nrx * hx + nry * hy + nrz * hz);
            double specularHighlight = 0;
            if (diffuseLight > 0) { 
                specularHighlight = Math.pow(Math.max(0.0, dotNH), 24.0); 
            }
            
            double ambient = 0.15;
            double lightIntensity = (ambient + (diffuseLight * 0.85)) * occlusion;
            lightIntensity = Math.max(0.0, Math.min(1.0, lightIntensity));
            
            // Suppress shiny highlights inside the occluded slot to maintain realism
            specularHighlight *= occlusion;
            
            String lString = " .,-~:;=!*#$@"; 
            int charIndex = (int) (lightIntensity * (lString.length() - 1)); 
            char asciiChar = lString.charAt(charIndex); 
            
            int rBase = 120, gBase = 60,  bBase = 10;   
            int rDiff = 255, gDiff = 190, bDiff = 0;    
            int rSpec = 255, gSpec = 255, bSpec = 255;  
            
            // Calculate base surface color (shadows to midtones)
            double rSurface = (1.0 - lightIntensity) * rBase + lightIntensity * rDiff;
            double gSurface = (1.0 - lightIntensity) * gBase + lightIntensity * gDiff;
            double bSurface = (1.0 - lightIntensity) * bBase + lightIntensity * bDiff;

            // Interpolate toward pure white for the specular highlight to prevent RGB capping
            int r = (int) ((1.0 - specularHighlight) * rSurface + specularHighlight * rSpec);
            int g = (int) ((1.0 - specularHighlight) * gSurface + specularHighlight * gSpec);
            int b = (int) ((1.0 - specularHighlight) * bSurface + specularHighlight * bSpec);
            
            r = Math.max(0, Math.min(255, r)); 
            g = Math.max(0, Math.min(255, g)); 
            b = Math.max(0, Math.min(255, b)); 
            
            outputBuffer[o] = String.format("\u001B[38;2;%d;%d;%dm%c\u001B[0m", r, g, b, asciiChar); 
        } 
    }
}