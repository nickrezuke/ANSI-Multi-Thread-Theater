# Loading Spinners: Advanced Real-Time ASCII Shading Engine

Inspired by Andy Sloan’s legendary `donut.c`, these loaders are a collection of real-time 3D ASCII graphics able to be rendered with this rendering engine built entirely in vanilla Java. 

While the original concept proved that 3D geometries could be projected onto a flat text grid, this project transforms that novelty into a loading-screen framework. It introduces full ANSI TrueColor ray-lit compositing, geometric abstractions, and asynchronous thread messaging to serve as a functional, visually stunning spinner for active CLI background processes.

---

## 🚀 Key Features

* **Polymorphic Mesh Architecture**: Every rendering model inherits from an abstract `Loader` class, allowing you to seamlessly swap out structural geometries mid-execution.
* **Asynchronous Threaded Loaders**: Designed to be invoked by worker threads. The loader safely operates on a dedicated graphics loop while your primary logic processes data in the background.
* **Live Progress Bar Binding**: Real-time cross-thread communication loops allow your worker function to feed granular progress updates to the active `Loader`, updating a stylized ASCII status track dynamically.
* **TrueColor Shading & Custom Palettes**: Features custom illumination models, specular glints, edge-glow, and per-pixel programmatic tint mapping utilizing raw terminal ANSI sequences to bring style to an otherwise entirely mathematical endeveour.
* **Expanded Geometry Library**: Includes the iconic donut (torus), cubes, spheres ant pyramids, complex orbital configurations, and completely custom math meshes.

---

## 📐 The Geometric Library

This Project comes packed with a suite of rendering targets, all derived from a common footprint:

->1. **DonutLoader (The Heritage Torus)**: An expanded, color-customizable adaptation of Sloan's original code, refined with triginometric frosting and a sprinkle map.
->2. **CubeLoaderA (The Basic Cube)**: A standard cube with white checkerboad boarders and rivets.
->3. **CubeLoaderB (The Textured Cube)**: A hyper-stylized cube using a custom method to define the exact pixel colors, to create an exact texture (Voxel Texel)
->4. **Custom Meshes**: Completely unique 2D/3D math geometries built inside terminal boundaries (DNA, Black Hole, Tesseract, Lorentz, RetroWave, TextFall, Radar, etc.).

---

## 🛠️ Architecture & Core Framework

At the heart of the engine is an object-oriented rendering pipeline. All spinners utilize a uniform execution pattern managed via an abstract baseline class:


### Thread Safe Inter-Process Synchronization

Unlike single-threaded loop implementations, a `Loader` operates concurrently. The calling method passes down an active thread token, spins up the canvas lifecycle, and feeds numeric ticks over thread pipelines:

```java
// Instantiating your loader
Loader mySpinner = new DonutLoader();

// Fire up the visual thread context
Thread loaderThread = new Thread(mySpinner);
loaderThread.start();

try {
    for (int i = 0; i <= 100; i += 5) {
        // Perform hard calculations here...
        Thread.sleep(200); // Replace sleep with your code
        
        // Asynchronously update the spinner's internal state while your code is running
        mySpinner.updateProgress(i); // where i is an int between 0-100
    }
} finally {
    // Gracefully spin-down rendering frames
    mySpinner.stop();
}
// continue...
```

---

## 🎨 Under the Hood: Advanced Terminal Text Shading

Standard ASCII renderers rely on basic linear lighting character loops. This engine utilizes custom vector graphics techniques optimized for high-density console grids:

### Aspect Ratio Squish Fix
Standard terminal font cells possess an asymmetric roughly `1:2` width-to-height ratio, transforming standard sphere equations into egg configurations. Continuum3D embeds structural multiplier constraints during canvas mapping coordinates to enforce symmetry:

*   **Screen X Projection:** `40 + (82 * ooz * rx)`
*   **Screen Y Projection:** `11 + (44 * ooz * ry)`

### High Contrast Specular Overdrive & Fresnel Blooms
Real physics curves wash out vibrant colors. The pipeline overrides linear light curves by tracking silhouette matrices relative to the perspective viewport vector, bursting deep primary color profiles outwards while clipping highlights instantly to pure white blocks (`█`) if saturation limits break structural parameters ($> 0.85$).

---

## 🚀 Getting Started

### Prerequisites
* **Java Development Kit (JDK)**: 17 or higher recommended.
* **Terminal Emulator**: A terminal emulator supporting **ANSI Escape Codes** and **TrueColor (24-bit RGB) characters**.

### Running the Project
->1. Clone the repository to your local machine:
   ```bash
   git clone https://github.com/nickrezuke/Loading-Spinners.git
   cd Loading-Spinners
   ```
->2a. Compile and Run the java files:
   ```bash
   javac *.java
   java ExampleTask
   ```
->2b. Alternatively, use the multi-command:
   ```bash
   javac *.java && java ExampleTask ; rm *.class
   ```

->3. To run specific loaders, use their name in the command:
   ```bash
   javac *.java && java ExampleTask DNA ; rm *.class
   javac *.java && java ExampleTask Lorenz ; rm *.class
   javac *.java && java ExampleTask Tesseract ; rm *.class

   ```

---