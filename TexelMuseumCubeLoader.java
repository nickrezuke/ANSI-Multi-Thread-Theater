// TODO: Improve the visual clarity of those artworks

public class TexelMuseumCubeLoader extends TexelCubeLoader {
    private static final StatusStage[] ART_STAGES = new StatusStage[] {
            new StatusStage(15, "Erecting Gallery Walls:"),
            new StatusStage(35, "Stretching Canvas Linens:"),
            new StatusStage(55, "Mixing Raw Oil Pigments:"),
            new StatusStage(75, "Applying Gold Leaf Frames:"),
            new StatusStage(90, "Adjusting Track Lighting:"),
            new StatusStage(100, "Exhibition Open!")
    };

    // Bumped to 128 for detailed structural rendering of fine art
    private static final int TEXTURE_RESOLUTION = 128;

    // Classical gold/wood frame bounds
    private static final int OUTER_FRAME = 12;
    private static final int GOLD_TRIM = 16;

    public TexelMuseumCubeLoader() {
        super(ART_STAGES, 80, 22);
    }

    @Override
    protected int getTextureResolution() {
        return TEXTURE_RESOLUTION;
    }

    @Override
    protected void initialize() {
        // No random colors needed; textures are assigned deterministically per face
    }

    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        int size = TEXTURE_RESOLUTION;
        int edgeDist = Math.min(Math.min(x, size - 1 - x), Math.min(y, size - 1 - y));

        // --- 1. MUSEUM PICTURE FRAME RENDERER ---
        if (edgeDist < OUTER_FRAME) {
            // Ornate Carved Mahogany/Walnut Outer Frame
            int shadow = (edgeDist % 3 == 0) ? -20 : 0;
            return new VoxelTexel(Math.max(0, 70 + shadow), Math.max(0, 45 + shadow), Math.max(0, 25 + shadow), '▓');
        }
        if (edgeDist < GOLD_TRIM) {
            // Antique Gold Leaf Inner Trim
            int shine = ((x + y) % 4 == 0) ? 30 : 0;
            return new VoxelTexel(Math.min(255, 215 + shine), Math.min(255, 175 + shine), Math.max(0, 55 - shine), '█');
        }

        // Normalize inner canvas coordinates to 0.0 -> 1.0
        double cx = (double) (x - GOLD_TRIM) / (size - 2 * GOLD_TRIM);
        double cy = (double) (y - GOLD_TRIM) / (size - 2 * GOLD_TRIM);

        // --- 2. THE 6 MASTERPIECES (One distinct painting mapped per cube face) ---
        switch (face) {
            case 0: // FRONT: Mona Lisa (Leonardo da Vinci)
                return renderMonaLisa(cx, cy);
            case 1: // BACK: The Starry Night (Vincent van Gogh)
                return renderStarryNight(cx, cy);
            case 2: // TOP: The Scream (Edvard Munch)
                return renderTheScream(cx, cy);
            case 3: // BOTTOM: Girl with a Pearl Earring (Johannes Vermeer)
                return renderGirlPearlEarring(cx, cy);
            case 4: // LEFT: The Great Wave off Kanagawa (Hokusai)
                return renderGreatWave(cx, cy);
            case 5: // RIGHT: Composition with Red, Blue and Yellow (Piet Mondrian)
            default:
                return renderMondrian(cx, cy);
        }
    }

    private VoxelTexel renderMonaLisa(double x, double y) {
        // Dark, moody atmospheric sfumato background
        int r = 35, g = 40, b = 30;
        char ch = '░';

        // Sfumato landscape horizon break
        if (y > 0.45 && y < 0.55) {
            r = 45;
            g = 58;
            b = 48;
        }

        // Pyramidal portrait silhouette shape (Head and shoulders body arc)
        double dx = x - 0.5;
        double dy = y - 0.65;
        if ((dy * dy * 1.4) + (dx * dx) < 0.12) {
            r = 30;
            g = 28;
            b = 24;
            ch = '▒'; // Dark Renaissance drapery/dress

            // Glowing skin tones for the centered oval face
            double fx = x - 0.5;
            double fy = y - 0.35;
            if ((fy * fy * 1.8) + (fx * fx) < 0.015) {
                r = 195;
                g = 165;
                b = 125;
                ch = '█'; // Face highlights
            }
            // Veil / hair cascading down sides
            else if (Math.abs(fx) > 0.07 && y > 0.32 && y < 0.6) {
                r = 25;
                g = 22;
                b = 18;
                ch = '▓';
            }
        }
        return new VoxelTexel(r, g, b, ch);
    }

    private VoxelTexel renderStarryNight(double x, double y) {
        // Deep swirling blue and indigo night sky
        int r = 15, g = 25, b = 75;
        char ch = '▒';

        // Swirling light blue clouds wind pattern across center
        double swirl = Math.sin(x * 7.0 + y * 4.0);
        if (swirl > 0.4 && y < 0.65) {
            r = 60;
            g = 110;
            b = 180;
            ch = '░';
        }

        // Bright glowing yellow stars & crescent moon (Top-right)
        if (y < 0.45) {
            // Moon localized cluster
            if (Math.pow(x - 0.82, 2) + Math.pow(y - 0.22, 2) < 0.008) {
                r = 245;
                g = 215;
                b = 100;
                ch = '█';
            }
            // Star clusters
            else if (Math.sin(x * 25.0) * Math.sin(y * 25.0) > 0.75) {
                r = 250;
                g = 230;
                b = 140;
                ch = '█';
            }
        }

        // Rolling blue-green hills across lower canvas line
        if (y >= 0.68) {
            r = 20;
            g = 45;
            b = 55;
            ch = '▓';
        }

        // Giant dark looming Cypress silhouette cutting up on the left side
        if (x > 0.12 && x < 0.32 && y > (0.95 - (x * 1.8))) {
            r = 12;
            g = 22;
            b = 18;
            ch = '█';
        }
        return new VoxelTexel(r, g, b, ch);
    }

    private VoxelTexel renderTheScream(double x, double y) {
        int r, g, b;
        char ch = '▒';

        if (y < 0.38) {
            // Fiery linear blood-red and orange sky
            r = (int) (220 - (y * 80));
            g = 85;
            b = 40;
            ch = '█';
        } else if (y < 0.62) {
            // Dark blue-black turbid shoreline landscape water bend
            double waveY = 0.5 + 0.08 * Math.sin(x * 4.0);
            if (y > waveY) {
                r = 25;
                g = 40;
                b = 80;
            } else {
                r = 160;
                g = 130;
                b = 70;
            }
        } else {
            // Deep brown/purple wooden bridge structural perspective plank
            r = (int) (110 + (x * 40));
            g = 75;
            b = 60;
        }

        // Distorted central screaming figure (Elongated pale skeleton/ghoul)
        double fx = x - 0.52;
        double fy = y - 0.68;
        if ((fy * fy * 2.2) + (fx * fx * 0.7) < 0.025) {
            r = 215;
            g = 210;
            b = 175;
            ch = '█'; // Pale face/hands combo
            // Hollowed dark socket eyes/mouth voids
            if ((Math.abs(fx) < 0.03 && fy > -0.04 && fy < -0.01) || (fx * fx + Math.pow(fy - 0.04, 2) < 0.003)) {
                r = 30;
                g = 30;
                b = 35;
                ch = '░';
            }
        }
        return new VoxelTexel(r, g, b, ch);
    }

    private VoxelTexel renderGirlPearlEarring(double x, double y) {
        // Deep pure dark black negative space studio background
        int r = 15, g = 15, b = 18;
        char ch = ' ';

        double dx = x - 0.52;
        double dy = y - 0.52;

        // Structural Portrait Composition
        if ((dy * dy * 1.2) + (dx * dx) < 0.14) {
            ch = '▒';
            if (y < 0.38) {
                // Vibrant ultramarine blue turban headdress top wrap
                r = 20;
                g = 65;
                b = 155;
                ch = '█';
                if (x > 0.58) {
                    r = 210;
                    g = 160;
                    b = 45;
                } // Lemon yellow cascading fabric tail
            } else if (y >= 0.38 && y < 0.58) {
                // Luminous pearlescent pale flesh skin profile
                r = 225;
                g = 185;
                b = 155;
                ch = '█';
                // Contrast shadow zone for profile depth
                if (dx < -0.05) {
                    r = 120;
                    g = 90;
                    b = 75;
                    ch = '▓';
                }

                // The brilliant shining single point white Pearl Earring highlight
                if (Math.abs(x - 0.46) < 0.02 && Math.abs(y - 0.54) < 0.02) {
                    r = 245;
                    g = 245;
                    b = 250;
                    ch = '█';
                }
            } else {
                // Earthy ochre / brown canvas jacket coat line
                r = 145;
                g = 110;
                b = 55;
                ch = '▓';
                if (y < 0.64 && dx > -0.08 && dx < 0.08) {
                    r = 230;
                    g = 225;
                    b = 220;
                } // Crisp white collar trim
            }
        }
        return new VoxelTexel(r, g, b, ch);
    }

    private VoxelTexel renderGreatWave(double x, double y) {
        // Traditional flat beige/tan print paper background
        int r = 215, g = 200, b = 170;
        char ch = '░';

        // Deep blue ocean floor base tracking up from bottom frame edge
        if (y > 0.55) {
            r = 25;
            g = 55;
            b = 110;
            ch = '▒';
        }

        // The massive rolling sweeping crest curve line of the Great Wave
        double waveCurve = 0.85 - Math.pow(x - 0.1, 2) * 1.1 - 0.4 * x;
        if (y > waveCurve) {
            r = 15;
            g = 45;
            b = 95;
            ch = '▓'; // Prussian Blue deep water column

            // Frothing white spray claws breaking over top lip
            if (Math.abs(y - waveCurve) < 0.05 && x < 0.65) {
                r = 245;
                g = 245;
                b = 240;
                ch = '█';
            }
        }

        // Tiny centered silhouette representation of Mount Fuji in background valley
        if (x > 0.44 && x < 0.58 && y > 0.58 && y <= 0.68) {
            double fujiSlope = 0.68 - (0.1 - Math.abs(x - 0.51) * 1.3);
            if (y > fujiSlope) {
                if (y < fujiSlope + 0.025) {
                    r = 250;
                    g = 250;
                    b = 250;
                    ch = '█';
                } // Snow-capped cone peak
                else {
                    r = 40;
                    g = 50;
                    b = 70;
                    ch = '▒';
                } // Dark volcano body rock
            }
        }
        return new VoxelTexel(r, g, b, ch);
    }

    private VoxelTexel renderMondrian(double x, double y) {
        // Modernist De Stijl Primary Grid blocks
        int r = 240, g = 235, b = 225; // Default off-white panels
        char ch = '█';

        // Define asymmetric thick solid black structural divider frame gridlines
        boolean isGridX = Math.abs(x - 0.28) < 0.025 || Math.abs(x - 0.88) < 0.02;
        boolean isGridY = Math.abs(y - 0.32) < 0.025 || Math.abs(y - 0.76) < 0.02;

        if (isGridX || isGridY) {
            return new VoxelTexel(15, 15, 20, '█');
        }

        // Fill explicit distinct geometric grid intersections with pure solid primary
        // colors
        if (x > 0.28 && x < 0.88 && y < 0.32) {
            r = 210;
            g = 35;
            b = 35; // Iconic Large Red Block
        } else if (x < 0.28 && y > 0.76) {
            r = 30;
            g = 65;
            b = 145;
            // Bottom-Left Blue Block
        } else if (x > 0.88 && y > 0.76) {
            r = 235;
            g = 195;
            b = 20;
            // Bottom-Right Yellow Block
        } else if (x < 0.28 && y < 0.32) {
            r = 25;
            g = 25;
            b = 30;
            // Small Accent Black Block
        }
        return new VoxelTexel(r, g, b, ch);
    }
}