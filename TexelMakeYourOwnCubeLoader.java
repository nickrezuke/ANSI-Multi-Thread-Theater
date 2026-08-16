/**
 * ============================================================================
 * TEXEL CUBE CUSTOMIZATION TEMPLATE
 * ============================================================================
 * This class is a template you can use to design your own 3D textured cube
 * You do not need to worry about the 3D math, matrices, or rendering.
 * Your only job is to decide what colors and characters go on each cube face!
 * 
 * HOW TO MAKE THIS CLASS YOUR OWN:
 * 1. Change the text in 'TEXEL_CUBE_STAGES' to show your own custom loading
 * messages and percents you'd like to have them appear under.
 * 2. Change the number in 'getTextureResolution()' to make your art as detailed 
 * or as retro as you want but do keep in mind the clarity of the terminal window
 * any resolutions above, say, 256x256 may be absolutely unnecessary.
 * 3. Edit 'getCubeTexel()' to paint colors and map characters onto the faces.
 * 4. Decide a good name for the Loader, and rename / save this file.
 * 5. Add your new loader in the LoaderFactory.java file, so that the loader can
 * be registered for instantiation along with its name.  Use others as reference.
 * All you need to do is add a string to represent the name, and then include the 
 * constructor for your new class, so that the program calls your constructor when
 * that specific name gets passed in as the preferred loader.
 * ============================================================================
 */
public class TexelMakeYourOwnCubeLoader extends TexelCubeLoader {

    // --- FACE IDENTIFIER CONSTANTS ---
    // Do not change these numbers. They tell the rendering engine which face is
    // which.
    private static final int FRONT_FACE = 0;
    private static final int BACK_FACE = 1;
    private static final int BOTTOM_FACE = 2;
    private static final int TOP_FACE = 3;
    private static final int LEFT_FACE = 4;
    private static final int RIGHT_FACE = 5;

    // --- LOADING STATUS STAGES ---
    // Customize these text strings! They will display sequentially at the bottom
    // of the screen as your loader initializes and progresses from 0% to 100%.
    // Add more or remove some of these as needed.
    private static final StatusStage[] TEXEL_CUBE_STAGES = new StatusStage[] {
            new StatusStage(12, "You can make your own!:"),
            new StatusStage(25, "Contributing is cool!:"),
            new StatusStage(40, "Open up the project:"),
            new StatusStage(55, "Take a look at this file:"),
            new StatusStage(70, "TexelMakeYourOwnCubeLoader.java:"),
            new StatusStage(85, "Read the documentation:"),
            new StatusStage(96, "Play around with the code:"),
            new StatusStage(100, "Seriously, Make Your Own!")
    };

    // Default Constructor
    public TexelMakeYourOwnCubeLoader() {
        super(TEXEL_CUBE_STAGES);
    }

    // Custom Dimensions Constructor
    // Use this if you need to stretch or constrain the terminal display region.
    public TexelMakeYourOwnCubeLoader(int viewWindowWidth, int viewWindowHeight) {
        super(TEXEL_CUBE_STAGES, viewWindowWidth, viewWindowHeight);
    }

    /**
     * STEP 1: DEFINE YOUR CANVAS DETAIL (RESOLUTION)
     * 
     * Think of each face of the cube as a square grid of pixels (texels).
     * This method returns the grid size (Width and Height) for a single face.
     * 
     * Examples:
     * - return 8; -> Ultra retro, pixelated look (8x8 grid per face).
     * - return 16; -> Standard balanced terminal resolution (16x16 grid).
     * - return 32; -> High detail, but requires a larger terminal window to look
     * clear.
     */
    @Override
    protected int getTextureResolution() {
        return 16;
    }

    /**
     * STEP 2: PAINT THE FACES
     * 
     * The engine calls this method automatically for every single coordinate (x, y)
     * on every face of the cube to figure out what it should look like.
     * 
     * COORDINATE SYSTEM:
     * - 'x' loops from 0 up to (Resolution - 1) -> Left to Right
     * - 'y' loops from 0 up to (Resolution - 1) -> Top to Bottom
     * 
     * HOW TO RETURN A VOXELTEXEL:
     * - Syntax: return new VoxelTexel(Red, Green, Blue, 'Character');
     * - Red, Green, Blue: Integer numbers from 0 (completely dark) to 255 (maximum
     * brightness).
     * - 'Character': The literal text character printed at that spot (e.g., '█',
     * '#', '@', or ' ').
     * You can also use unicode, such as \u2588, to define specific characters.
     * 
     * PRO-TIP FOR MATH ART:
     * Use 'x' and 'y' inside your color calculations to build smooth gradients!
     * Use System.currentTimeMillis() % N to create a time based value
     * Use Math.sin() to create waves and circles!
     * Use Math.random() for a totally random and uncorelated value, good for Static
     * or Speckles
     * Determine the printed character dynamically for texture effects
     * int[][] Bitmaps can also be used for defining faces
     */
    @Override
    protected VoxelTexel getCubeTexel(int face, int x, int y) {
        switch (face) {

            case FRONT_FACE:
                // Example of gradient by putting x or y as values for R G or B
                // Just make sure your R G B values never go above 255!!
                return new VoxelTexel(255, y * 16, x * 16, '&');

            case LEFT_FACE:
                // Example of using Time
                int timeVal = (int) (System.currentTimeMillis() % 255);
                return new VoxelTexel(timeVal, timeVal, timeVal, '@');

            case RIGHT_FACE:
                // Example of using sin wave functions
                double circleFactor = 4.0 * Math.PI / getTextureResolution();
                double waveX = 0.5 * (Math.sin(x * circleFactor) + 1);
                double waveY = 0.5 * (Math.cos(y * circleFactor) + 1);
                int r = (int) (255.0 * waveX);
                int g = (int) (255.0 * Math.sqrt(waveX) * Math.sqrt(waveY));
                int b = (int) (255.0 * waveY);
                return new VoxelTexel(r, g, b, '\u2586');

            case BACK_FACE:
                // Example of using Random Numbers
                int randNum = (int) (Math.random() * 255);
                // Here I am using the same value for all R G B.
                // Using 3 separate random() calls makes rainbow static
                return new VoxelTexel(randNum, randNum, randNum, '?');

            case BOTTOM_FACE:
                // Example of using different face chars
                char faceChar = x > y ? '\u229B' : '\u25A3';
                return new VoxelTexel(185, 215, 125, faceChar);

            case TOP_FACE:
                // Example of defining a Bitmap Array (in this case, a 16x16  grid)
                // If you change the resolution, your bitmap dimensions must match.
                // Here I only used two different values, but you can include more
                // values to get a much more colorful and detailed texture map.           
                int[][] bitmap = { // 0 = Background, 1 = Features
                        { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 },
                        { 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0 },
                        { 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 },
                        { 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 },
                        { 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0 },
                        { 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1 },
                        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                        { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                        { 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1 },
                        { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 },
                        { 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0 },
                        { 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 },
                        { 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 },
                        { 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0 },
                        { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 },
                        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
                };
                // Expert Move: You should actually define this bitmap as a 'private static final int[][]'
                // Defining it here, inside the method, means the computer recreates that entire 2D matrix 
                // thousands of times per second (for every single coordinate, every single frame). 
                // Moving it to the top of the file as a static final makes much more sense.


                // To use the map, just define the colors / chars used for each value
                if (bitmap[y][x] == 1) {
                    return new VoxelTexel(255, 235, 40, '\u2588');
                } else if (bitmap[y][x] == 0) {
                    return new VoxelTexel(25, 20, 55, '\u25A2');
                } else {
                    // Always fallback in case theres an error in the bitmap
                    return new VoxelTexel(125, 125, 125, '?');
                }

            default:
                // Fallback Safety for the Switch Statement: if for some reason we passed a
                // value for 'face' that wasn't a value of TOP, BACK, RIGHT, FRONT, etc.
                return new VoxelTexel(255, 255, 255, '\u2588');
        }
    }
}
