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
                case "CubeA":    
                    loader = new CubeLoaderA();    
                    break;
                case "CubeB":    
                    loader = new CubeLoaderB();    
                    break;
                case "Pyramid": 
                    loader = new PyramidLoader(); 
                    break;
                //case "Snake":   
                    //loader = new SnakeLoader();   
                    //break;
                default: 
                    break; // Unsupported string falls through to random fallback below
            }
        }

        // 2. If no arg was provided OR the arg was unsupported, pick a random varient
        if (loader == null) {
            int totalVarients = 12; // Right now I have 6 Donuts, 4 CubeA, 3 CubeB, and 1 Pyramid
            double rand = (Math.random() * totalVarients);
            if(rand < 6) { // Chances 0-5
                loader = new DonutLoader();

            } else if(rand < 10) { // Chances 6-9
                loader = new CubeLoaderA();
            } else if(rand < 13) { // Chances 10-12
                loader = new CubeLoaderB();
            } else {
                loader = new PyramidLoader();
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
                Thread.sleep(200);          // Simulating 100ms of work time
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