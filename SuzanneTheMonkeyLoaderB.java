// "B" variant of SuzanneTheMonkeyLoader: instead of plotting each mesh
// vertex as an isolated character, this reconstructs actual triangular
// faces from the raw point cloud (see buildFaces() below) and fills
// them in, Gouraud-shaded from the same per-vertex normals the original
// loader already estimated -- so the point cloud actually reads as a
// continuous lit surface instead of a scatter of dots.

public class SuzanneTheMonkeyLoaderB extends Loader {
    private static final StatusStage[] STAGES = {
        new StatusStage(10, "Loading suzanne.obj (507 mesh vertices)..."),
        new StatusStage(25, "Nearest-neighbor search across the point cloud..."),
        new StatusStage(45, "Estimating normals via local PCA (Jacobi eigensolve)..."),
        new StatusStage(70, "Fan-triangulating neighborhoods into surface faces..."),
        new StatusStage(90, "Z-Buffer occlusion test successful..."),
        new StatusStage(100, "Gouraud-Shaded Rasterization Complete!")
    };


    private double angleX = 0.0;
    private double angleY = 0.0;

    private final int width;
    private final int height;

    // Vertex positions for the Suzanne (Blender monkey) test mesh, 507
    // points, sourced from the standard low-poly OBJ export (507
    // vertices / 500 faces). Only positions are kept here -- exactly
    // like the Stanford Bunny loader, per-vertex normals are re-derived
    // below via local-neighborhood PCA rather than trusting any
    // face/normal data that shipped with the source file, so this
    // loader's pipeline is a straight drop-in swap of the point cloud.
    private static final double[][] RAW_MESH_POSITIONS = {
        {-2.056562, 1.415748, 4.869517}, {-2.931562, 1.415748, 4.869517}, {-1.994062, 1.345436, 4.791392}, {-2.994062, 1.345436, 4.791392},
        {-1.947187, 1.306373, 4.682017}, {-3.040937, 1.306373, 4.682017}, {-2.142500, 1.228248, 4.721080}, {-2.845625, 1.228248, 4.721080},
        {-2.142500, 1.282936, 4.822642}, {-2.845625, 1.282936, 4.822642}, {-2.142500, 1.384498, 4.885142}, {-2.845625, 1.384498, 4.885142},
        {-2.220625, 1.415748, 4.900767}, {-2.767500, 1.415748, 4.900767}, {-2.290937, 1.345436, 4.846080}, {-2.697187, 1.345436, 4.846080},
        {-2.337812, 1.306373, 4.752330}, {-2.650312, 1.306373, 4.752330}, {-2.415937, 1.493873, 4.760142}, {-2.572187, 1.493873, 4.760142},
        {-2.353437, 1.493873, 4.846080}, {-2.634687, 1.493873, 4.846080}, {-2.251875, 1.493873, 4.900767}, {-2.736250, 1.493873, 4.900767},
        {-2.220625, 1.579811, 4.900767}, {-2.767500, 1.579811, 4.900767}, {-2.290937, 1.642311, 4.846080}, {-2.697187, 1.642311, 4.846080},
        {-2.337812, 1.689186, 4.752330}, {-2.650312, 1.689186, 4.752330}, {-2.142500, 1.767311, 4.721080}, {-2.845625, 1.767311, 4.721080},
        {-2.142500, 1.704811, 4.822642}, {-2.845625, 1.704811, 4.822642}, {-2.142500, 1.611061, 4.885142}, {-2.845625, 1.611061, 4.885142},
        {-2.056562, 1.579811, 4.869517}, {-2.931562, 1.579811, 4.869517}, {-1.994062, 1.642311, 4.791392}, {-2.994062, 1.642311, 4.791392},
        {-1.947187, 1.689186, 4.682017}, {-3.040937, 1.689186, 4.682017}, {-1.869062, 1.493873, 4.666392}, {-3.119062, 1.493873, 4.666392},
        {-1.931562, 1.493873, 4.775767}, {-3.056562, 1.493873, 4.775767}, {-2.025312, 1.493873, 4.861705}, {-2.962812, 1.493873, 4.861705},
        {-2.017500, 1.493873, 4.877330}, {-2.970625, 1.493873, 4.877330}, {-2.048750, 1.587623, 4.885142}, {-2.939375, 1.587623, 4.885142},
        {-2.142500, 1.626686, 4.908580}, {-2.845625, 1.626686, 4.908580}, {-2.228437, 1.587623, 4.924205}, {-2.759687, 1.587623, 4.924205},
        {-2.267500, 1.493873, 4.924205}, {-2.720625, 1.493873, 4.924205}, {-2.228437, 1.407936, 4.924205}, {-2.759687, 1.407936, 4.924205},
        {-2.142500, 1.493873, 4.932017}, {-2.845625, 1.493873, 4.932017}, {-2.142500, 1.368873, 4.908580}, {-2.845625, 1.368873, 4.908580},
        {-2.048750, 1.407936, 4.885142}, {-2.939375, 1.407936, 4.885142}, {-2.494062, 1.681373, 4.846080}, {-2.494062, 1.603248, 4.924205},
        {-2.494062, 0.571998, 4.838267}, {-2.494062, 0.931373, 4.885142}, {-2.494062, 1.064186, 4.900767}, {-2.494062, 0.478248, 4.822642},
        {-2.494062, 1.657936, 4.705455}, {-2.494062, 1.821998, 4.674205}, {-2.494062, 2.150123, 3.557017}, {-2.494062, 1.814186, 3.252330},
        {-2.494062, 1.321998, 3.275767}, {-2.494062, 0.868873, 3.752330}, {-2.290937, 1.064186, 4.666392}, {-2.697187, 1.064186, 4.666392},
        {-2.181562, 0.814186, 4.674205}, {-2.806562, 0.814186, 4.674205}, {-2.142500, 0.556373, 4.674205}, {-2.845625, 0.556373, 4.674205},
        {-2.126875, 0.361061, 4.635142}, {-2.861250, 0.361061, 4.635142}, {-2.165937, 0.306373, 4.627330}, {-2.822187, 0.306373, 4.627330},
        {-2.314375, 0.282936, 4.658580}, {-2.673750, 0.282936, 4.658580}, {-2.494062, 0.267311, 4.682017}, {-2.056562, 1.111061, 4.635142},
        {-2.931562, 1.111061, 4.635142}, {-1.861250, 1.212623, 4.642955}, {-3.126875, 1.212623, 4.642955}, {-1.665937, 1.400123, 4.549205},
        {-3.322187, 1.400123, 4.549205}, {-1.634687, 1.681373, 4.697642}, {-3.353437, 1.681373, 4.697642}, {-1.783125, 1.736061, 4.728892},
        {-3.205000, 1.736061, 4.728892}, {-2.001875, 1.853248, 4.791392}, {-2.986250, 1.853248, 4.791392}, {-2.173750, 2.009498, 4.838267},
        {-2.814375, 2.009498, 4.838267}, {-2.337812, 1.970436, 4.861705}, {-2.650312, 1.970436, 4.861705}, {-2.431562, 1.743873, 4.853892},
        {-2.556562, 1.743873, 4.853892}, {-2.330000, 1.665748, 4.877330}, {-2.658125, 1.665748, 4.877330}, {-2.369062, 1.556373, 4.869517},
        {-2.619062, 1.556373, 4.869517}, {-2.290937, 1.345436, 4.846080}, {-2.697187, 1.345436, 4.846080}, {-2.119062, 1.267311, 4.807017},
        {-2.869062, 1.267311, 4.807017}, {-2.001875, 1.314186, 4.775767}, {-2.986250, 1.314186, 4.775767}, {-1.869062, 1.439186, 4.752330},
        {-3.119062, 1.439186, 4.752330}, {-1.853437, 1.548561, 4.752330}, {-3.134687, 1.548561, 4.752330}, {-1.892500, 1.626686, 4.767955},
        {-3.095625, 1.626686, 4.767955}, {-2.064375, 1.689186, 4.822642}, {-2.923750, 1.689186, 4.822642}, {-2.244062, 1.720436, 4.861705},
        {-2.744062, 1.720436, 4.861705}, {-2.494062, 0.486061, 4.838267}, {-2.384687, 0.532936, 4.838267}, {-2.603437, 0.532936, 4.838267},
        {-2.376875, 0.415748, 4.814830}, {-2.611250, 0.415748, 4.814830}, {-2.431562, 0.368873, 4.799205}, {-2.556562, 0.368873, 4.799205},
        {-2.494062, 0.361061, 4.791392}, {-2.494062, 1.056373, 4.853892}, {-2.494062, 1.111061, 4.846080}, {-2.392500, 1.103248, 4.846080},
        {-2.595625, 1.103248, 4.846080}, {-2.369062, 1.025123, 4.853892}, {-2.619062, 1.025123, 4.853892}, {-2.408125, 0.962623, 4.846080},
        {-2.580000, 0.962623, 4.846080}, {-2.095625, 1.204811, 4.775767}, {-2.892500, 1.204811, 4.775767}, {-1.876875, 1.306373, 4.728892},
        {-3.111250, 1.306373, 4.728892}, {-1.767500, 1.454811, 4.705455}, {-3.220625, 1.454811, 4.705455}, {-1.751875, 1.626686, 4.760142},
        {-3.236250, 1.626686, 4.760142}, {-1.806562, 1.665748, 4.830455}, {-3.181562, 1.665748, 4.830455}, {-2.056562, 1.798561, 4.900767},
        {-2.931562, 1.798561, 4.900767}, {-2.181562, 1.892311, 4.939830}, {-2.806562, 1.892311, 4.939830}, {-2.290937, 1.868873, 4.955455},
        {-2.697187, 1.868873, 4.955455}, {-2.392500, 1.681373, 4.947642}, {-2.595625, 1.681373, 4.947642}, {-2.369062, 1.150123, 4.916392},
        {-2.619062, 1.150123, 4.916392}, {-2.283125, 0.806373, 4.814830}, {-2.705000, 0.806373, 4.814830}, {-2.244062, 0.548561, 4.791392},
        {-2.744062, 0.548561, 4.791392}, {-2.228437, 0.431373, 4.767955}, {-2.759687, 0.431373, 4.767955}, {-2.259687, 0.337623, 4.736705},
        {-2.728437, 0.337623, 4.736705}, {-2.330000, 0.321998, 4.736705}, {-2.658125, 0.321998, 4.736705}, {-2.494062, 0.306373, 4.744517},
        {-2.494062, 1.298561, 4.830455}, {-2.494062, 1.462623, 4.869517}, {-2.165937, 1.728248, 4.846080}, {-2.822187, 1.728248, 4.846080},
        {-2.330000, 1.392311, 4.853892}, {-2.658125, 1.392311, 4.853892}, {-2.361250, 1.462623, 4.861705}, {-2.626875, 1.462623, 4.861705},
        {-2.376875, 0.564186, 4.838267}, {-2.611250, 0.564186, 4.838267}, {-2.415937, 0.806373, 4.853892}, {-2.572187, 0.806373, 4.853892},
        {-2.494062, 0.806373, 4.853892}, {-2.494062, 0.923561, 4.846080}, {-2.400312, 0.978248, 4.885142}, {-2.587812, 0.978248, 4.885142},
        {-2.361250, 1.025123, 4.900767}, {-2.626875, 1.025123, 4.900767}, {-2.384687, 1.118873, 4.885142}, {-2.603437, 1.118873, 4.885142},
        {-2.455000, 1.126686, 4.885142}, {-2.533125, 1.126686, 4.885142}, {-2.494062, 1.048561, 4.932017}, {-2.447187, 1.103248, 4.916392},
        {-2.540937, 1.103248, 4.916392}, {-2.400312, 1.095436, 4.916392}, {-2.587812, 1.095436, 4.916392}, {-2.384687, 1.025123, 4.932017},
        {-2.603437, 1.025123, 4.932017}, {-2.415937, 1.001686, 4.908580}, {-2.572187, 1.001686, 4.908580}, {-2.494062, 0.962623, 4.908580},
        {-2.236250, 0.939186, 4.658580}, {-2.751875, 0.939186, 4.658580}, {-2.330000, 1.009498, 4.814830}, {-2.658125, 1.009498, 4.814830},
        {-2.314375, 0.939186, 4.814830}, {-2.673750, 0.939186, 4.814830}, {-2.259687, 1.001686, 4.658580}, {-2.728437, 1.001686, 4.658580},
        {-2.494062, 0.376686, 4.791392}, {-2.447187, 0.384498, 4.791392}, {-2.540937, 0.384498, 4.791392}, {-2.400312, 0.431373, 4.814830},
        {-2.587812, 0.431373, 4.814830}, {-2.400312, 0.509498, 4.830455}, {-2.587812, 0.509498, 4.830455}, {-2.494062, 0.470436, 4.760142},
        {-2.400312, 0.501686, 4.767955}, {-2.587812, 0.501686, 4.767955}, {-2.400312, 0.439186, 4.744517}, {-2.587812, 0.439186, 4.744517},
        {-2.447187, 0.400123, 4.736705}, {-2.540937, 0.400123, 4.736705}, {-2.494062, 0.392311, 4.736705}, {-2.322187, 1.470436, 4.885142},
        {-2.665937, 1.470436, 4.885142}, {-2.306562, 1.407936, 4.877330}, {-2.681562, 1.407936, 4.877330}, {-2.158125, 1.681373, 4.861705},
        {-2.830000, 1.681373, 4.861705}, {-2.220625, 1.673561, 4.877330}, {-2.767500, 1.673561, 4.877330}, {-2.072187, 1.650123, 4.877330},
        {-2.915937, 1.650123, 4.877330}, {-1.931562, 1.603248, 4.799205}, {-3.056562, 1.603248, 4.799205}, {-1.908125, 1.540748, 4.791392},
        {-3.080000, 1.540748, 4.791392}, {-1.915937, 1.446998, 4.783580}, {-3.072187, 1.446998, 4.783580}, {-2.017500, 1.353248, 4.822642},
        {-2.970625, 1.353248, 4.822642}, {-2.119062, 1.314186, 4.846080}, {-2.869062, 1.314186, 4.846080}, {-2.267500, 1.361061, 4.885142},
        {-2.720625, 1.361061, 4.885142}, {-2.314375, 1.548561, 4.885142}, {-2.673750, 1.548561, 4.885142}, {-2.283125, 1.626686, 4.885142},
        {-2.705000, 1.626686, 4.885142}, {-2.259687, 1.611061, 4.861705}, {-2.728437, 1.611061, 4.861705}, {-2.298750, 1.548561, 4.861705},
        {-2.689375, 1.548561, 4.861705}, {-2.251875, 1.376686, 4.861705}, {-2.736250, 1.376686, 4.861705}, {-2.119062, 1.337623, 4.830455},
        {-2.869062, 1.337623, 4.830455}, {-2.033125, 1.368873, 4.807017}, {-2.955000, 1.368873, 4.807017}, {-1.947187, 1.462623, 4.775767},
        {-3.040937, 1.462623, 4.775767}, {-1.939375, 1.532936, 4.775767}, {-3.048750, 1.532936, 4.775767}, {-1.962812, 1.587623, 4.783580},
        {-3.025312, 1.587623, 4.783580}, {-2.080000, 1.642311, 4.853892}, {-2.908125, 1.642311, 4.853892}, {-2.212812, 1.650123, 4.869517},
        {-2.775312, 1.650123, 4.869517}, {-2.158125, 1.657936, 4.853892}, {-2.830000, 1.657936, 4.853892}, {-2.290937, 1.423561, 4.853892},
        {-2.697187, 1.423561, 4.853892}, {-2.298750, 1.478248, 4.853892}, {-2.689375, 1.478248, 4.853892}, {-2.384687, 1.712623, 4.713267},
        {-2.603437, 1.712623, 4.713267}, {-2.298750, 1.915748, 4.721080}, {-2.689375, 1.915748, 4.721080}, {-2.158125, 1.939186, 4.697642},
        {-2.830000, 1.939186, 4.697642}, {-2.009687, 1.806373, 4.658580}, {-2.978437, 1.806373, 4.658580}, {-1.814375, 1.704811, 4.596080},
        {-3.173750, 1.704811, 4.596080}, {-1.697187, 1.657936, 4.564830}, {-3.290937, 1.657936, 4.564830}, {-1.720625, 1.415748, 4.478892},
        {-3.267500, 1.415748, 4.478892}, {-1.892500, 1.251686, 4.517955}, {-3.095625, 1.251686, 4.517955}, {-2.056562, 1.157936, 4.572642},
        {-2.931562, 1.157936, 4.572642}, {-2.494062, 2.150123, 4.392955}, {-2.494062, 2.236061, 4.025767}, {-2.494062, 1.056373, 3.432017},
        {-2.494062, 0.790748, 4.291392}, {-2.494062, 0.275123, 4.564830}, {-2.494062, 0.446998, 4.447642}, {-2.494062, 0.681373, 4.424205},
        {-2.494062, 0.767311, 4.385142}, {-1.642500, 1.486061, 4.158580}, {-3.345625, 1.486061, 4.158580}, {-1.634687, 1.571998, 4.057017},
        {-3.353437, 1.571998, 4.057017}, {-1.720625, 1.517311, 3.666392}, {-3.267500, 1.517311, 3.666392}, {-2.033125, 1.689186, 3.400767},
        {-2.955000, 1.689186, 3.400767}, {-1.759687, 1.204811, 4.174205}, {-3.228437, 1.204811, 4.174205}, {-1.900312, 1.126686, 3.939830},
        {-3.087812, 1.126686, 3.939830}, {-1.853437, 1.243873, 3.674205}, {-3.134687, 1.243873, 3.674205}, {-2.158125, 1.306373, 3.439830},
        {-2.830000, 1.306373, 3.439830}, {-2.259687, 0.900123, 4.510142}, {-2.728437, 0.900123, 4.510142}, {-2.314375, 0.837623, 4.361705},
        {-2.673750, 0.837623, 4.361705}, {-2.205000, 0.540748, 4.486705}, {-2.783125, 0.540748, 4.486705}, {-2.244062, 0.751686, 4.494517},
        {-2.744062, 0.751686, 4.494517}, {-2.165937, 0.337623, 4.502330}, {-2.822187, 0.337623, 4.502330}, {-2.353437, 0.493873, 4.471080},
        {-2.634687, 0.493873, 4.471080}, {-2.369062, 0.712623, 4.463267}, {-2.619062, 0.712623, 4.463267}, {-2.330000, 0.306373, 4.541392},
        {-2.658125, 0.306373, 4.541392}, {-2.275312, 0.970436, 4.533580}, {-2.712812, 0.970436, 4.533580}, {-2.283125, 1.025123, 4.572642},
        {-2.705000, 1.025123, 4.572642}, {-2.290937, 1.079811, 4.603892}, {-2.697187, 1.079811, 4.603892}, {-2.283125, 0.861061, 4.267955},
        {-2.705000, 0.861061, 4.267955}, {-2.197187, 0.939186, 3.838267}, {-2.790937, 0.939186, 3.838267}, {-2.150312, 1.103248, 3.564830},
        {-2.837812, 1.103248, 3.564830}, {-2.040937, 2.118873, 3.721080}, {-2.947187, 2.118873, 3.721080}, {-2.040937, 2.181373, 4.033580},
        {-2.947187, 2.181373, 4.033580}, {-2.040937, 2.103248, 4.338267}, {-2.947187, 2.103248, 4.338267}, {-2.033125, 1.775123, 4.533580},
        {-2.955000, 1.775123, 4.533580}, {-1.767500, 1.657936, 4.439830}, {-3.220625, 1.657936, 4.439830}, {-1.861250, 1.704811, 4.385142},
        {-3.126875, 1.704811, 4.385142}, {-1.853437, 1.954811, 4.158580}, {-3.134687, 1.954811, 4.158580}, {-1.697187, 1.814186, 4.228892},
        {-3.290937, 1.814186, 4.228892}, {-1.697187, 1.868873, 3.986705}, {-3.290937, 1.868873, 3.986705}, {-1.853437, 2.001686, 3.908580},
        {-3.134687, 2.001686, 3.908580}, {-1.853437, 1.931373, 3.658580}, {-3.134687, 1.931373, 3.658580}, {-1.697187, 1.790748, 3.744517},
        {-3.290937, 1.790748, 3.744517}, {-1.876875, 1.579811, 3.517955}, {-3.111250, 1.579811, 3.517955}, {-2.009687, 1.275123, 3.557017},
        {-2.978437, 1.275123, 3.557017}, {-1.673750, 1.579811, 3.900767}, {-3.314375, 1.579811, 3.900767}, {-2.087812, 1.079811, 4.252330},
        {-2.900312, 1.079811, 4.252330}, {-2.064375, 1.056373, 3.892955}, {-2.923750, 1.056373, 3.892955}, {-1.603437, 1.657936, 3.869517},
        {-3.384687, 1.657936, 3.869517}, {-1.720625, 1.111061, 3.978892}, {-3.267500, 1.111061, 3.978892}, {-1.455000, 1.150123, 3.775767},
        {-3.533125, 1.150123, 3.775767}, {-1.212812, 1.306373, 3.674205}, {-3.775312, 1.306373, 3.674205}, {-1.142500, 1.571998, 3.682017},
        {-3.845625, 1.571998, 3.682017}, {-1.259687, 1.759498, 3.682017}, {-3.728437, 1.759498, 3.682017}, {-1.470625, 1.728248, 3.791392},
        {-3.517500, 1.728248, 3.791392}, {-1.478437, 1.665748, 3.814830}, {-3.509687, 1.665748, 3.814830}, {-1.306562, 1.689186, 3.713267},
        {-3.681562, 1.689186, 3.713267}, {-1.228437, 1.540748, 3.697642}, {-3.759687, 1.540748, 3.697642}, {-1.283125, 1.329811, 3.697642},
        {-3.705000, 1.329811, 3.697642}, {-1.462812, 1.212623, 3.799205}, {-3.525312, 1.212623, 3.799205}, {-1.665937, 1.181373, 3.971080},
        {-3.322187, 1.181373, 3.971080}, {-1.572187, 1.611061, 3.885142}, {-3.415937, 1.611061, 3.885142}, {-1.548750, 1.556373, 3.814830},
        {-3.439375, 1.556373, 3.814830}, {-1.611250, 1.228248, 3.892955}, {-3.376875, 1.228248, 3.892955}, {-1.455000, 1.251686, 3.736705},
        {-3.533125, 1.251686, 3.736705}, {-1.306562, 1.345436, 3.658580}, {-3.681562, 1.345436, 3.658580}, {-1.259687, 1.501686, 3.658580},
        {-3.728437, 1.501686, 3.658580}, {-1.322187, 1.611061, 3.666392}, {-3.665937, 1.611061, 3.666392}, {-1.470625, 1.595436, 3.744517},
        {-3.517500, 1.595436, 3.744517}, {-1.650312, 1.540748, 3.892955}, {-3.337812, 1.540748, 3.892955}, {-1.658125, 1.423561, 3.830455},
        {-3.330000, 1.423561, 3.830455}, {-1.736250, 1.345436, 3.830455}, {-3.251875, 1.345436, 3.830455}, {-1.673750, 1.337623, 3.830455},
        {-3.314375, 1.337623, 3.830455}, {-1.650312, 1.267311, 3.830455}, {-3.337812, 1.267311, 3.830455}, {-1.681562, 1.236061, 3.830455},
        {-3.306562, 1.236061, 3.830455}, {-1.767500, 1.251686, 4.033580}, {-3.220625, 1.251686, 4.033580}, {-1.775312, 1.228248, 3.932017},
        {-3.212812, 1.228248, 3.932017}, {-1.775312, 1.290748, 3.916392}, {-3.212812, 1.290748, 3.916392}, {-1.697187, 1.454811, 3.892955},
        {-3.290937, 1.454811, 3.892955}, {-1.603437, 1.493873, 3.838267}, {-3.384687, 1.493873, 3.838267}, {-1.603437, 1.486061, 3.783580},
        {-3.384687, 1.486061, 3.783580}, {-1.681562, 1.236061, 3.783580}, {-3.306562, 1.236061, 3.783580}, {-1.642500, 1.267311, 3.783580},
        {-3.345625, 1.267311, 3.783580}, {-1.665937, 1.329811, 3.783580}, {-3.322187, 1.329811, 3.783580}, {-1.728437, 1.345436, 3.783580},
        {-3.259687, 1.345436, 3.783580}, {-1.650312, 1.423561, 3.783580}, {-3.337812, 1.423561, 3.783580}, {-1.455000, 1.579811, 3.689830},
        {-3.533125, 1.579811, 3.689830}, {-1.306562, 1.595436, 3.619517}, {-3.681562, 1.595436, 3.619517}, {-1.236250, 1.493873, 3.611705},
        {-3.751875, 1.493873, 3.611705}, {-1.283125, 1.337623, 3.619517}, {-3.705000, 1.337623, 3.619517}, {-1.447187, 1.251686, 3.682017},
        {-3.540937, 1.251686, 3.682017}, {-1.611250, 1.236061, 3.838267}, {-3.376875, 1.236061, 3.838267}, {-1.540937, 1.540748, 3.760142},
        {-3.447187, 1.540748, 3.760142}, {-1.603437, 1.361061, 3.775767}, {-3.384687, 1.361061, 3.775767}, {-1.556562, 1.314186, 3.767955},
        {-3.431562, 1.314186, 3.767955}, {-1.494062, 1.376686, 3.736705}, {-3.494062, 1.376686, 3.736705}, {-1.533125, 1.423561, 3.752330},
        {-3.455000, 1.423561, 3.752330}, {-1.478437, 1.486061, 3.728892}, {-3.509687, 1.486061, 3.728892}, {-1.439375, 1.439186, 3.721080},
        {-3.548750, 1.439186, 3.721080}, {-1.384687, 1.462623, 3.713267}, {-3.603437, 1.462623, 3.713267}, {-1.408125, 1.525123, 3.713267},
        {-3.580000, 1.525123, 3.713267}, {-1.470625, 1.689186, 3.619517}, {-3.517500, 1.689186, 3.619517}, {-1.244062, 1.720436, 3.557017},
        {-3.744062, 1.720436, 3.557017}, {-1.126875, 1.548561, 3.603892}, {-3.861250, 1.548561, 3.603892}, {-1.181562, 1.306373, 3.572642},
        {-3.806562, 1.306373, 3.572642}, {-1.455000, 1.165748, 3.611705}, {-3.533125, 1.165748, 3.611705}, {-1.705000, 1.126686, 3.775767},
        {-3.283125, 1.126686, 3.775767}, {-1.634687, 1.634498, 3.721080}, {-3.353437, 1.634498, 3.721080},
    };

    // How many nearest neighbors are used to fit the local plane (and
    // therefore the normal) at each mesh vertex. Suzanne is a sparse,
    // low-poly mesh (507 points vs. the bunny scan's 1,888), so a
    // smaller neighborhood is used here than in the bunny loader --
    // 16 neighbors on this mesh would often reach across sharp
    // features (eyes, ear rims, the nose point) and over-smooth them.
    private static final int NEIGHBOR_COUNT = 10;

    // Tuning knobs for buildFaces()'s fan triangulation, defined here
    // rather than down next to that method: MAX_ANGULAR_GAP isn't a
    // compile-time constant (Math.toRadians() is a real method call),
    // and Java runs non-constant static field initializers in strict
    // textual order -- if this sat below the "static { FACES = ... }"
    // block instead of above it, buildFaces() would read it as its
    // not-yet-initialized default of 0.0 and reject almost every
    // candidate triangle.
    private static final int FAN_NEIGHBOR_COUNT = 9;
    private static final double MAX_EDGE_FACTOR = 2.3;
    private static final double MAX_ANGULAR_GAP = Math.toRadians(150);

    // Baked once at class-load: centered/scaled positions, a real
    // locally-estimated normal for every one of the 507 mesh vertices,
    // and the triangular faces reconstructed from that point cloud
    // (see buildFaces() near the bottom of this file).
    private static final double[][] MESH_POSITIONS;
    private static final double[][] MESH_NORMALS;
    private static final int[][] FACES;

    static {
        double[][][] baked = bakeMesh();
        MESH_POSITIONS = baked[0];
        MESH_NORMALS = baked[1];
        FACES = buildFaces(MESH_POSITIONS, MESH_NORMALS);
    }

    // Reusable per-vertex scratch buffers (screen position, depth, and
    // shaded luminance), computed ONCE per frame in renderGeometry()'s
    // first pass and then read many times over by the triangle
    // rasterizer in its second pass -- allocated once here, per
    // instance (not static/shared -- see the frameTimeNanos comment in
    // Loader.java for why that matters), to keep this at 0% GC pressure
    // per frame just like the rest of the pipeline.
    private final double[] screenX;
    private final double[] screenY;
    private final double[] depthBuf;
    private final double[] vertexLum;
    private final double[] rotNormalY;

    public SuzanneTheMonkeyLoaderB(StatusStage[] stages, int width, int height) {
        super(stages, width, height);
        this.width = width;
        this.height = height;
        int vertexCount = MESH_POSITIONS.length;
        this.screenX = new double[vertexCount];
        this.screenY = new double[vertexCount];
        this.depthBuf = new double[vertexCount];
        this.vertexLum = new double[vertexCount];
        this.rotNormalY = new double[vertexCount];
    }

    public SuzanneTheMonkeyLoaderB() {
        super(STAGES, 80, 22);
        this.height = this.window_height;
        this.width = this.window_width;
        int vertexCount = MESH_POSITIONS.length;
        this.screenX = new double[vertexCount];
        this.screenY = new double[vertexCount];
        this.depthBuf = new double[vertexCount];
        this.vertexLum = new double[vertexCount];
        this.rotNormalY = new double[vertexCount];
    }

    @Override
    protected void initialize() {
        this.angleX = 0.3;
        this.angleY = 0.0;

        if (!this.isRawCanvas) {
            TerminalConfig.restoreMode();
        }
    }

    @Override
    protected void renderGeometry(String[] outputBuffer, double[] zBuffer) {
        angleY -= 0.028;

        double cosX = Math.cos(angleX), sinX = Math.sin(angleX);
        double cosY = Math.cos(angleY), sinY = Math.sin(angleY);

        double lightX = 0.577, lightY = -0.577, lightZ = 0.577;
        double fillX = -0.4, fillY = 0.4, fillZ = -0.5;

        // Pass 1: transform every vertex exactly once per frame --
        // rotated screen position, depth, and shaded luminance -- and
        // cache the results in the scratch buffers above. FACES only
        // ever references vertices by index, so nothing here is
        // recomputed per-triangle-corner in pass 2 below.
        for (int i = 0; i < MESH_POSITIONS.length; i++) {
            double[] p = MESH_POSITIONS[i];
            double[] nrm = MESH_NORMALS[i];

            double r1x = p[0] * cosY - p[1] * sinY;
            double r1y = p[0] * sinY + p[1] * cosY;
            double r1z = p[2];
            double rotX = r1x;
            double rotY = r1y * cosX - r1z * sinX;
            double rotZ = r1y * sinX + r1z * cosX;

            double n1x = nrm[0] * cosY - nrm[1] * sinY;
            double n1y = nrm[0] * sinY + nrm[1] * cosY;
            double n1z = nrm[2];
            double rotNX = n1x;
            double rotNY = n1y * cosX - n1z * sinX;
            double rotNZ = n1y * sinX + n1z * cosX;
            double nLen = Math.sqrt(rotNX * rotNX + rotNY * rotNY + rotNZ * rotNZ);
            if (nLen > 0) {
                rotNX /= nLen;
                rotNY /= nLen;
                rotNZ /= nLen;
            }

            double cameraDepth = rotY + 5.0;
            double D = 1.0 / cameraDepth;

            this.screenX[i] = width / 2.0 + (width * 0.62) * D * rotX;
            this.screenY[i] = height / 2.0 - (height * 1.05) * D * (rotZ + 0.1);
            this.depthBuf[i] = D;
            this.rotNormalY[i] = rotNY;

            double dotKey = Math.max(0.0, rotNX * lightX + rotNY * lightY + rotNZ * lightZ);
            double dotFill = Math.max(0.0, rotNX * fillX + rotNY * fillY + rotNZ * fillZ);
            double luminance = 0.14 + 0.62 * dotKey + 0.30 * dotFill;
            if (luminance > 1.0) luminance = 1.0;
            this.vertexLum[i] = luminance;
        }

        // Pass 2: fill in every reconstructed triangular face, z-buffered
        // and Gouraud-shaded from the three cached vertex luminances
        // above instead of only ever touching three isolated pixels.
        for (int[] face : FACES) {
            rasterizeTriangle(face[0], face[1], face[2], outputBuffer, zBuffer);
        }
    }

    // Fills one triangular face into out/zb using the per-vertex values
    // pass 1 (above) already cached for its three corner indices.
    // Barycentric weights across the triangle interpolate depth (for
    // the z-buffer test) and luminance (for the character chosen from
    // the palette) the same way a GPU would Gouraud-shade a polygon --
    // just landing on ASCII cells instead of real pixels.
    private void rasterizeTriangle(int ia, int ib, int ic, String[] out, double[] zb) {
        // Backface cull: the camera looks toward +Y (see cameraDepth
        // in pass 1), so a face whose averaged, rotated normal is
        // still pointing toward +Y is facing away from the viewer this
        // frame -- skip it rather than let it fight the z-buffer.
        double avgNY = (rotNormalY[ia] + rotNormalY[ib] + rotNormalY[ic]) / 3.0;
        if (avgNY > 0.0) {
            return;
        }

        double x0 = screenX[ia], y0 = screenY[ia], d0 = depthBuf[ia], l0 = vertexLum[ia];
        double x1 = screenX[ib], y1 = screenY[ib], d1 = depthBuf[ib], l1 = vertexLum[ib];
        double x2 = screenX[ic], y2 = screenY[ic], d2 = depthBuf[ic], l2 = vertexLum[ic];

        int minX = (int) Math.floor(Math.min(x0, Math.min(x1, x2)));
        int maxX = (int) Math.ceil(Math.max(x0, Math.max(x1, x2)));
        int minY = (int) Math.floor(Math.min(y0, Math.min(y1, y2)));
        int maxY = (int) Math.ceil(Math.max(y0, Math.max(y1, y2)));
        if (minX < 0) minX = 0;
        if (minY < 0) minY = 0;
        if (maxX >= width) maxX = width - 1;
        if (maxY >= height) maxY = height - 1;
        if (minX > maxX || minY > maxY) {
            return;
        }

        double area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (Math.abs(area) < 1e-9) {
            return;
        }
        double invArea = 1.0 / area;

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                double w0 = ((x1 - px) * (y2 - py) - (x2 - px) * (y1 - py)) * invArea;
                double w1 = ((x2 - px) * (y0 - py) - (x0 - px) * (y2 - py)) * invArea;
                double w2 = 1.0 - w0 - w1;
                if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) {
                    continue;
                }

                double D = w0 * d0 + w1 * d1 + w2 * d2;
                int o = px + width * py;
                if (D > zb[o] + 0.0001) {
                    zb[o] = D;

                    double luminance = w0 * l0 + w1 * l1 + w2 * l2;

                    // Finer-grained grayscale ramp for smoother shading gradients
                    String palette = " .'`^\",:;Il!i><~+_-?][}{1)(|\\/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$";
                    int charIndex = (int) (luminance * (palette.length() - 1));
                    charIndex = Math.max(0, Math.min(palette.length() - 1, charIndex));
                    char asciiChar = palette.charAt(charIndex);

                    out[o] = (asciiChar != ' ') ? (WHITE + asciiChar + RESET) : " ";
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Point-cloud preparation: axis remap, centering/scaling, and real
    // per-point normal estimation from local neighborhoods. Suzanne's
    // OBJ source does ship its own per-vertex normals, but they are
    // deliberately ignored here -- this loader re-derives normals from
    // the raw positions the same way the bunny loader does, so the two
    // loaders share one proven pipeline instead of two divergent ones.
    // -----------------------------------------------------------------

    private static double[][][] bakeMesh() {
        int n = RAW_MESH_POSITIONS.length;

        // The mesh is Y-up (same convention as the bunny scan). Our
        // rotation pipeline spins (yaw) around the Z axis, so we remap
        // the source's up-axis (Y) into our Z slot, and its forward
        // axis (Z) into our Y (depth) slot. X stays X.
        double[][] modelPos = new double[n][3];
        for (int i = 0; i < n; i++) {
            double[] raw = RAW_MESH_POSITIONS[i];
            modelPos[i][0] = raw[0];
            modelPos[i][1] = raw[2];
            modelPos[i][2] = raw[1];
        }

        // Center on the origin and uniformly scale to a canvas-friendly size.
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] p : modelPos) {
            if (p[0] < minX) minX = p[0];
            if (p[0] > maxX) maxX = p[0];
            if (p[1] < minY) minY = p[1];
            if (p[1] > maxY) maxY = p[1];
            if (p[2] < minZ) minZ = p[2];
            if (p[2] > maxZ) maxZ = p[2];
        }
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;
        double extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        double targetSize = 5.5;
        double scale = targetSize / extent;

        double[][] positions = new double[n][3];
        for (int i = 0; i < n; i++) {
            positions[i][0] = (modelPos[i][0] - cx) * scale;
            positions[i][1] = (modelPos[i][1] - cy) * scale;
            positions[i][2] = (modelPos[i][2] - cz) * scale;
        }

        // For each point: find its K nearest neighbors, fit a local
        // plane via PCA (eigenvector of the neighborhood covariance
        // matrix with the smallest eigenvalue = the plane normal), then
        // orient it outward from the model's center.
        double[][] normals = new double[n][3];
        int k = NEIGHBOR_COUNT;
        double[] bestDist = new double[k];
        int[] bestIdx = new int[k];

        for (int i = 0; i < n; i++) {
            int found = 0;
            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                double dx = positions[j][0] - positions[i][0];
                double dy = positions[j][1] - positions[i][1];
                double dz = positions[j][2] - positions[i][2];
                double d2 = dx * dx + dy * dy + dz * dz;

                if (found < k) {
                    bestDist[found] = d2;
                    bestIdx[found] = j;
                    found++;
                } else {
                    int worst = 0;
                    for (int m = 1; m < k; m++) {
                        if (bestDist[m] > bestDist[worst]) worst = m;
                    }
                    if (d2 < bestDist[worst]) {
                        bestDist[worst] = d2;
                        bestIdx[worst] = j;
                    }
                }
            }

            double meanX = positions[i][0], meanY = positions[i][1], meanZ = positions[i][2];
            for (int m = 0; m < found; m++) {
                meanX += positions[bestIdx[m]][0];
                meanY += positions[bestIdx[m]][1];
                meanZ += positions[bestIdx[m]][2];
            }
            int total = found + 1;
            meanX /= total;
            meanY /= total;
            meanZ /= total;

            double cxx = 0, cyy = 0, czz = 0, cxy = 0, cxz = 0, cyz = 0;
            double ddx = positions[i][0] - meanX, ddy = positions[i][1] - meanY, ddz = positions[i][2] - meanZ;
            cxx += ddx * ddx; cyy += ddy * ddy; czz += ddz * ddz;
            cxy += ddx * ddy; cxz += ddx * ddz; cyz += ddy * ddz;
            for (int m = 0; m < found; m++) {
                double[] p = positions[bestIdx[m]];
                ddx = p[0] - meanX; ddy = p[1] - meanY; ddz = p[2] - meanZ;
                cxx += ddx * ddx; cyy += ddy * ddy; czz += ddz * ddz;
                cxy += ddx * ddy; cxz += ddx * ddz; cyz += ddy * ddz;
            }

            double[] normal = smallestEigenvector(cxx, cxy, cxz, cyy, cyz, czz);

            double dot = normal[0] * positions[i][0] + normal[1] * positions[i][1] + normal[2] * positions[i][2];
            if (dot < 0) {
                normal[0] = -normal[0];
                normal[1] = -normal[1];
                normal[2] = -normal[2];
            }
            normals[i] = normal;
        }

        return new double[][][] { positions, normals };
    }

    // Returns the eigenvector associated with the smallest eigenvalue of
    // the given symmetric 3x3 matrix, via the classical cyclic Jacobi
    // eigenvalue algorithm (a handful of sweeps is plenty of accuracy
    // for a local-plane-fit normal).
    private static double[] smallestEigenvector(double cxx, double cxy, double cxz,
                                                  double cyy, double cyz, double czz) {
        double[][] a = { { cxx, cxy, cxz }, { cxy, cyy, cyz }, { cxz, cyz, czz } };
        double[][] v = { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };

        for (int sweep = 0; sweep < 8; sweep++) {
            for (int pairIdx = 0; pairIdx < 3; pairIdx++) {
                int p, q;
                if (pairIdx == 0) { p = 0; q = 1; }
                else if (pairIdx == 1) { p = 0; q = 2; }
                else { p = 1; q = 2; }
                int r = 3 - p - q;

                double apq = a[p][q];
                if (Math.abs(apq) < 1e-14) continue;

                double app = a[p][p], aqq = a[q][q];
                double theta = (aqq - app) / (2 * apq);
                double t = Math.signum(theta == 0 ? 1 : theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
                double c = 1.0 / Math.sqrt(t * t + 1);
                double s = t * c;

                double newApp = app - t * apq;
                double newAqq = aqq + t * apq;

                double arp = a[r][p], arq = a[r][q];
                double newArp = c * arp - s * arq;
                double newArq = s * arp + c * arq;

                a[p][p] = newApp;
                a[q][q] = newAqq;
                a[p][q] = 0;
                a[q][p] = 0;
                a[r][p] = newArp;
                a[p][r] = newArp;
                a[r][q] = newArq;
                a[q][r] = newArq;

                for (int i = 0; i < 3; i++) {
                    double vip = v[i][p], viq = v[i][q];
                    v[i][p] = c * vip - s * viq;
                    v[i][q] = s * vip + c * viq;
                }
            }
        }

        int minIdx = 0;
        if (a[1][1] < a[minIdx][minIdx]) minIdx = 1;
        if (a[2][2] < a[minIdx][minIdx]) minIdx = 2;

        double nx = v[0][minIdx], ny = v[1][minIdx], nz = v[2][minIdx];
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-12) {
            return new double[] { 0, 0, 1 };
        }
        return new double[] { nx / len, ny / len, nz / len };
    }

    // -----------------------------------------------------------------
    // Surface reconstruction: turns the bare point cloud (plus the
    // per-point normals bakeMesh() already estimated) into an actual
    // triangle mesh. Suzanne's OBJ source does ship real face data,
    // but -- like the vertex normals -- it's deliberately not used
    // here, so this loader shares one reconstruction pipeline with the
    // Stanford Bunny's raw laser scan, which has no face data at all.
    //
    // For each point, its FAN_NEIGHBOR_COUNT nearest neighbors are
    // projected onto that point's own local tangent plane (the plane
    // perpendicular to its estimated normal) and sorted by angle around
    // it, turning the unordered neighborhood into a "fan" of wedges
    // going all the way around the point; consecutive neighbors in that
    // fan become a triangle with the point itself. Two guards keep this
    // from bridging across gaps it shouldn't:
    //   - a max-edge-length cutoff, scaled to that particular point's
    //     own local neighbor spacing (density varies a lot across the
    //     mesh), throws out triangles that would reach across a real
    //     gap instead of following the surface, and
    //   - a max-angular-gap cutoff refuses to close the fan across a
    //     wide empty wedge, which is what a true boundary of the
    //     sampled surface (or a thin feature like an ear rim) looks
    //     like locally.
    // Every point runs this fan independently, so the same physical
    // triangle is typically rediscovered from two or three different
    // apex points; a dedup pass (keyed on the triangle's three vertex
    // indices, order-independent) collapses those back down to one
    // triangle each. Finally, each surviving triangle's winding order
    // is flipped if needed so its geometric normal points the same way
    // as the outward-oriented vertex normals bakeMesh() already
    // computed -- that shared orientation is what lets
    // rasterizeTriangle() backface-cull and shade every face correctly.
    // -----------------------------------------------------------------

    private static int[][] buildFaces(double[][] positions, double[][] normals) {
        int n = positions.length;
        int k = Math.min(FAN_NEIGHBOR_COUNT, n - 1);
        java.util.List<int[]> faces = new java.util.ArrayList<>();
        java.util.HashSet<Long> seen = new java.util.HashSet<>();

        int[] neighborIdx = new int[k];
        double[] neighborDist = new double[k];

        for (int i = 0; i < n; i++) {
            int found = findKNearest(positions, i, k, neighborIdx, neighborDist);
            if (found < 2) {
                continue;
            }

            double sumDist = 0;
            for (int j = 0; j < found; j++) {
                sumDist += neighborDist[j];
            }
            double maxEdge = (sumDist / found) * MAX_EDGE_FACTOR;

            // Orthonormal (u, v) basis spanning the plane perpendicular
            // to this point's normal, so neighbors can be sorted by
            // angle "around" the point along its own local surface.
            double nx = normals[i][0], ny = normals[i][1], nz = normals[i][2];
            double ax, ay, az;
            if (Math.abs(nz) < 0.9) {
                ax = 0; ay = 0; az = 1;
            } else {
                ax = 1; ay = 0; az = 0;
            }
            double ux = ny * az - nz * ay;
            double uy = nz * ax - nx * az;
            double uz = nx * ay - ny * ax;
            double ulen = Math.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= ulen; uy /= ulen; uz /= ulen;
            double vx = ny * uz - nz * uy;
            double vy = nz * ux - nx * uz;
            double vz = nx * uy - ny * ux;

            double[] angle = new double[found];
            for (int j = 0; j < found; j++) {
                double[] q = positions[neighborIdx[j]];
                double rx = q[0] - positions[i][0];
                double ry = q[1] - positions[i][1];
                double rz = q[2] - positions[i][2];
                double pu = rx * ux + ry * uy + rz * uz;
                double pv = rx * vx + ry * vy + rz * vz;
                angle[j] = Math.atan2(pv, pu);
            }

            Integer[] order = new Integer[found];
            for (int j = 0; j < found; j++) {
                order[j] = j;
            }
            final double[] angleRef = angle;
            java.util.Arrays.sort(order, (a, b) -> Double.compare(angleRef[a], angleRef[b]));

            for (int t = 0; t < found; t++) {
                int j0 = order[t];
                int j1 = order[(t + 1) % found];
                double gap = angle[j1] - angle[j0];
                if (gap < 0) {
                    gap += 2 * Math.PI;
                }
                if (gap > MAX_ANGULAR_GAP) {
                    continue;
                }
                if (neighborDist[j0] > maxEdge || neighborDist[j1] > maxEdge) {
                    continue;
                }

                int a = neighborIdx[j0];
                int b = neighborIdx[j1];

                double dx = positions[a][0] - positions[b][0];
                double dy = positions[a][1] - positions[b][1];
                double dz = positions[a][2] - positions[b][2];
                double abDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (abDist > maxEdge * 1.3) {
                    continue;
                }

                // Orient winding to match the already outward-facing
                // vertex normals, rather than whatever order the
                // angular sort happened to produce.
                double e1x = positions[a][0] - positions[i][0];
                double e1y = positions[a][1] - positions[i][1];
                double e1z = positions[a][2] - positions[i][2];
                double e2x = positions[b][0] - positions[i][0];
                double e2y = positions[b][1] - positions[i][1];
                double e2z = positions[b][2] - positions[i][2];
                double fnx = e1y * e2z - e1z * e2y;
                double fny = e1z * e2x - e1x * e2z;
                double fnz = e1x * e2y - e1y * e2x;
                double refx = normals[i][0] + normals[a][0] + normals[b][0];
                double refy = normals[i][1] + normals[a][1] + normals[b][1];
                double refz = normals[i][2] + normals[a][2] + normals[b][2];
                double dot = fnx * refx + fny * refy + fnz * refz;

                int v0 = i, v1, v2;
                if (dot < 0) {
                    v1 = b; v2 = a;
                } else {
                    v1 = a; v2 = b;
                }

                // Dedup key: this same physical triangle is typically
                // rediscovered from two or three different apex points,
                // so collapse repeats down to one entry regardless of
                // which vertex order they were found in.
                int lo = Math.min(v0, Math.min(v1, v2));
                int hi = Math.max(v0, Math.max(v1, v2));
                int mid = v0 + v1 + v2 - lo - hi;
                long key = ((long) lo << 42) | ((long) mid << 21) | (long) hi;

                if (seen.add(key)) {
                    faces.add(new int[] { v0, v1, v2 });
                }
            }
        }

        return faces.toArray(new int[0][]);
    }

    // Brute-force k-nearest-neighbor search reused for triangulation --
    // an independent pass from the one bakeMesh() runs for normal
    // estimation above. Simple and cheap at these point counts, and
    // keeps this stage fully decoupled from whatever k tuning normal
    // estimation ends up needing.
    private static int findKNearest(double[][] positions, int i, int k, int[] outIdx, double[] outDist) {
        int found = 0;
        for (int j = 0; j < positions.length; j++) {
            if (j == i) {
                continue;
            }
            double dx = positions[j][0] - positions[i][0];
            double dy = positions[j][1] - positions[i][1];
            double dz = positions[j][2] - positions[i][2];
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (found < k) {
                outIdx[found] = j;
                outDist[found] = d;
                found++;
            } else {
                int worst = 0;
                for (int m = 1; m < k; m++) {
                    if (outDist[m] > outDist[worst]) {
                        worst = m;
                    }
                }
                if (d < outDist[worst]) {
                    outDist[worst] = d;
                    outIdx[worst] = j;
                }
            }
        }
        return found;
    }
}