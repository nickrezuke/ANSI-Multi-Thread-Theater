// This class acts as my heavy processing application
public class LoaderSpinners {
    public static void main(String[] args) {
        // 1. Initialize and start the background donut thread
        DonutLoader donutLoader = new DonutLoader();
        Thread loadingThread = new Thread(donutLoader);
        loadingThread.start();

        // 2. SIMULATE HEAVY TASK (Increments 0% to 100% over 15 seconds)
        try {
            for (int p = 0; p <= 100; p += 1) {
                donutLoader.setProgress(p); // Push progress values to the donut loader
                Thread.sleep(100);          // Simulating work time
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Task is complete! Shut down the donut cleanly
        donutLoader.stopLoading();

        try {
            loadingThread.join(); // Wait for final frame cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Fresh clear so our completion message prints cleanly
        System.out.print("\u001b[2J\u001b[H");
    }
}