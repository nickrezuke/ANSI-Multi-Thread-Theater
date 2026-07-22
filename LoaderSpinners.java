// This class acts as my heavy processing example application
public class LoaderSpinners {
    public static void main(String[] args) {
        Loader loader = null;
        // 1. Check if a preference was passed and try to match it
        if (args.length > 0) {
            switch (args[0]) {
                case "Donut":   
                    loader = new DonutLoader();   
                    break;
                case "Cube":    
                    loader = new CubeLoader();    
                    break;
                //case "Pyramid": 
                    //loader = new PyramidLoader(); 
                    //break;
                //case "Snake":   
                    //loader = new SnakeLoader();   
                    //break;
                default: 
                    break; // Unsupported string falls through to random fallback below
            }
        }

        // 2. If no arg was provided OR the arg was unsupported, pick a random variant
        if (loader == null) {
            switch ((int) (Math.random() * 2) + 1) {
                case 1:
                    loader = new DonutLoader();
                    break;
                case 2:
                    loader = new CubeLoader();
                    break;
                case 3:
                    //loader = new PyramidLoader();
                    //break;
                case 4:
                default:
                    //loader = new SnakeLoader();
                    //break;
            }
        }
        
        // 3. Spin up the desired loader on a new thread, and start it
        Thread loadingThread = new Thread(loader);
        loadingThread.start();

        // 4. SIMULATE HEAVY TASK!!  This is done by just running sleep for a bit before 
        // updating the progress variable in increments from 0% to 100% over a few seconds
        try {
            for (int p = 0; p <= 100; p += 1) {
                loader.setProgress(p); // Push progress values to the loader
                Thread.sleep(100);          // Simulating 100ms of work time
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Dummy Task is complete! Shut down the loader cleanly
        loader.stopLoading();
        try {
            loadingThread.join(); // Wait for final frame cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 6. Fresh clear so our completion message prints cleanly
        System.out.print("\u001b[2J\u001b[H");
    }
}