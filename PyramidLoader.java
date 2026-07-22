public class PyramidLoader extends Loader {
    private static final StatusStage[] PYRAMID_STAGES = {
        new StatusStage(40, "Carving limestone blocks:"),
        new StatusStage(80, "Aligning golden capstone:"),
        new StatusStage(100, "Monument Complete!")
    };

    // Shading levels based on character density (similar to the classic donut)
    private static final char[] SHADE_CHARS = { '.', ':', '-', '=', '+', '*', 'X', '%', '#', '@' };
    
    private double angle = 0.0;

    public PyramidLoader() {
        super(PYRAMID_STAGES);
    }

    @Override
    protected void initialize() {
        // No heavy initialization needed for the basic shape geometry
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        // Only rotating around the Y-axis (Horizontal spin) to match your requirement
        double cosY = Math.cos(angle);
        double sinY = Math.sin(angle);

        // Define the 5 perfect key 3D structural vertices of our pyramid
        // Coordinate layout: X = Left/Right, Y = Up/Down, Z = Forward/Backward
        double[][] vertices = {
            {  0.0,  1.0,  0.0 }, // Vertex 0: Apex (Top Point)
            { -1.0, -1.0,  1.0 }, // Vertex 1: Front-Left Corner
            {  1.0, -1.0,  1.0 }, // Vertex 2: Front-Right Corner
            {  1.0, -1.0, -1.0 }, // Vertex 3: Back-Right Corner
            { -1.0, -1.0, -1.0 }  // Vertex 4: Back-Left Corner
        };

        // Define the 5 structural faces using indices from our vertices array above
        int[][] faces = {
            { 0, 1, 2 }, // Face 0: Front Triangle Side
            { 0, 2, 3 }, // Face 1: Right Triangle Side
            { 0, 3, 4 }, // Face 2: Back Triangle Side
            { 0, 4, 1 }, // Face 3: Left Triangle Side
            { 4, 3, 2, 1 } // Face 4: Square Base (Bottom Plane)
        };

        // Static directional lighting vector coming down over the viewer's right shoulder
        double lx = 0.5, ly = 0.7, lz = -0.5;
        // Normalize the light vector manually
        double lMag = Math.sqrt(lx*lx + ly*ly + lz*lz);
        lx /= lMag; ly /= lMag; lz /= lMag;

        // Loop through all 5 geometric faces of the pyramid
        for (int i = 0; i < faces.length; i++) {
            int[] faceVertices = faces[i];

            // 1. Calculate Face Normal using Cross-Product of two edge vectors
            double[] v0 = vertices[faceVertices[0]];
            double[] v1 = vertices[faceVertices[1]];
            double[] v2 = vertices[faceVertices[2]];

            double edge1x = v1[0] - v0[0];
            double edge1y = v1[1] - v0[1];
            double edge1z = v1[2] - v0[2];

            double edge2x = v2[0] - v0[0];
            double edge2y = v2[2] == -1.0 && faceVertices.length > 3 ? v2[1] - v0[1] : v2[1] - v0[1]; // Adjust for base configuration mapping
            if (i == 4) { // Explicit base overrides for accuracy
                edge2x = vertices[faceVertices[2]][0] - v0[0];
                edge2y = vertices[faceVertices[2]][1] - v0[1];
            }
            double edge2z = v2[2] - v0[2];
            if (i == 4) {
                edge2z = vertices[faceVertices[2]][2] - v0[2];
            }

            // Cross product vector math calculation
            double nx = edge1y * edge2z - edge1z * edge2y;
            double ny = edge1z * edge2x - edge1x * edge2z;
            double nz = edge1x * edge2y - edge1y * edge2x;

            // Normalize the calculated normal vector coordinates
            double nMag = Math.sqrt(nx*nx + ny*ny + nz*nz);
            nx /= nMag; ny /= nMag; nz /= nMag;

            // 2. Apply Y-Axis Rotation to the Normal Vector
            double rNx = nx * cosY + nz * sinY;
            double rNy = ny;
            double rNz = -nx * sinY + nz * cosY;

            // Back-face culling check: Skip rendering if pointing away from the camera lens
            if (rNz > 0) {
                continue;
            }

            // 3. Compute static structural shading intensity based on lighting directional dot product
            double dotProduct = rNx * lx + rNy * ly + rNz * lz;
            int shadeIndex = (int) ((dotProduct + 1.0) * 4.9); // Map range [-1, 1] across the 10 shade characters
            shadeIndex = Math.max(0, Math.min(9, shadeIndex));
            char renderChar = SHADE_CHARS[shadeIndex];

            // 4. Face Point Plotter (Triangles vs Base Squares)
            if (i < 4) { // Side Triangular Geometry Surfaces
                // Linearly interpolate coordinates inside triangle barycentric frameworks
                for (double u = 0; u <= 1.0; u += 0.02) {
                    for (double v = 0; v <= 1.0 - u; v += 0.02) {
                        double w = 1.0 - u - v;

                        // Calculate raw local structural coordinate placements
                        double x = u * v0[0] + v * v1[0] + w * v2[0];
                        double y = u * v0[1] + v * v1[1] + w * v2[1];
                        double z = u * v0[2] + v * v1[2] + w * v2[2];

                        projectAndPlot(x, y, z, cosY, sinY, renderChar, outputBuffer, zBuffer);
                    }
                }
            } else { // Square Bottom Base Geometry Surface
                for (double u = 0; u <= 1.0; u += 0.03) {
                    for (double v = 0; v <= 1.0; v += 0.03) {
                        // Bilinear interpolation strategy layout over base planes
                        double x = -1.0 + 2.0 * u;
                        double y = -1.0;
                        double z = -1.0 + 2.0 * v;

                        projectAndPlot(x, y, z, cosY, sinY, renderChar, outputBuffer, zBuffer);
                    }
                }
            }
        }
        angle += 0.05; // Spin increment rate step 
    }

    // Handles 3D rotation matrix calculations, camera offsets, perspective projections, and screen placement loops.
    private void projectAndPlot(double x, double y, double z, double cosY, double sinY, char renderChar, String[] outputBuffer, double[] zBuffer) {
        // Apply Y-axis rotation matrix formulas to coordinate positions
        double rx = x * cosY + z * sinY;
        double ry = y;
        double rz = -x * sinY + z * cosY;

        // Apply spatial focal perspective mapping properties
        double distanceToCamera = 3.5;
        double ooz = 1.0 / (rz + distanceToCamera);

        int xp = (int) (40 + 42 * ooz * rx);
        int yp = (int) (11 + 20 * ooz * ry);

        if (xp >= 0 && xp < 80 && yp >= 0 && yp < 22) {
            int index = xp + 80 * yp;
            if (ooz > zBuffer[index] + 0.0001) {
                zBuffer[index] = ooz;
                // Outputting pure monochrome text representation matches original donut engine principles
                outputBuffer[index] = String.valueOf(renderChar);
            }
        }
    }
}
