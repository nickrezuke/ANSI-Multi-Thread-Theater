// This class acts as my heavy processing example application
public class ExampleTask {
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
                case "RetroWave": 
                    loader = new RetroWaveLoader(); 
                    break;
                case "TextFall": 
                    loader = new TextFallLoader(); 
                    break;
                case "Radar": 
                    loader = new RadarLoader(); 
                    break;
                case "DNA": 
                    loader = new DNALoader(); 
                    break;
                case "BlackHole": 
                    loader = new BlackHoleLoader(); 
                    break;
                case "Lorenz": 
                    loader = new LorenzLoader(); 
                    break;
                case "Ring": 
                    loader = new RingLoader(); 
                    break;
                case "Shine": 
                    loader = new ShineLoader2(); 
                    break;
                case "Tesseract": 
                    loader = new TesseractLoader(); 
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
            int totalVarients = 19; // Right now I have 6 Donuts, 4 CubeA, 3 CubeB, 
            // 1 TextFall, 1 RetroWave, 1 DNA, 1 Radar, 1 BlackHole, and 1 Pyramid
            double rand = (Math.random() * totalVarients);
            if(rand < 6) { // Chances 0-5
                loader = new DonutLoader();
            } else if(rand < 10) { // Chances 6-9
                loader = new CubeLoaderA();
            } else if(rand < 13) { // Chances 10-12
                loader = new CubeLoaderB();
            } else if(rand < 14) { // Chance 13
                loader = new RetroWaveLoader();
            } else if(rand < 15) { // Chance 14
                loader = new PyramidLoader();
            } else if(rand < 16) { // Chance 15
                loader = new DNALoader();
            } else if(rand < 17) { // Chance 16
                loader = new LorenzLoader();
            } else if(rand < 18) { // Chance 17
                loader = new RadarLoader();
            } else if(rand < 19) { // Chance 18
                loader = new BlackHoleLoader();
            } else {               // Chance 19
                loader = new TextFallLoader();
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
                Thread.sleep(200);          // Simulating 200ms of work time
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