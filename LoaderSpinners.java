// This class acts as my heavy processing example application
public class LoaderSpinners {
    public static void main(String[] args) {
        // 1. Initialize and start the background loader thread
        
        //DonutLoader loader = new DonutLoader();
        CubeLoader loader = new CubeLoader();
        
        Thread loadingThread = new Thread(loader);
        loadingThread.start();

        // 2. SIMULATE HEAVY TASK (Increments 0% to 100% over a few seconds)
        try {
            for (int p = 0; p <= 100; p += 1) {
                loader.setProgress(p); // Push progress values to the loader
                Thread.sleep(100);          // Simulating 100ms of work time
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Dummy Task is complete! Shut down the loader cleanly
        loader.stopLoading();

        try {
            loadingThread.join(); // Wait for final frame cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Fresh clear so our completion message prints cleanly
        System.out.print("\u001b[2J\u001b[H");
    }
}