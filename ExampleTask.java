// This class acts as my heavy processing example application
public class ExampleTask {
    // ASCII Codes for resetting the terminal after running
    protected static final String CLEAR_SCREEN = "\u001b[2J";
    protected static final String CURSOR_HOME = "\u001b[H";
    protected static final String SHOW_CURSOR = "\u001b[?25h";

    public static void main(String[] args) {
        // This serves as an example file / existing code 
        // segment that would like to use a loader.
        // Here are the steps to do so in your own code:

        // ...
        // ...
        // ... ^ your existing code ^
        // ...

        // 1. Evaluate the desired loader and create it
        String userPreference = (args.length > 0) ? args[0] : null;
        // Here we pass one in when we ran ExampleTask, but you can 
        // do whatever you like, use a specific one, random one, etc.
        Loader loader = LoaderFactory.createLoaderInstance(userPreference); // Create it using our factory
        //Loader loader = LoaderFactory.createLoaderInstance("Donut"); // Or do a specific one...
        //Loader loader = LoaderFactory.createLoaderInstance(); // Or do a random one...

        // 2. Register an emergency thread that fires exclusively when Control+C is hit.
        // This acts as a bulletproof structural cleanup routine in case your users are
        // a little impatient and like hitting ctrl-c before things are finished...
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loader != null) {
                // Forcibly drop out of raw terminal structures immediately before closing
                loader.forceTerminalCleanup();
            } else {
                // Generic fallback if no loader was actively generated
                System.out.print(CLEAR_SCREEN + CURSOR_HOME + SHOW_CURSOR);
                System.out.flush();
            }
        }));

        // 3. Spin up the desired loader on a new thread, and start it!
        Thread loadingThread = new Thread(loader);
        loadingThread.start();

        // 4. Perform your heavy task that needs a loader to watch while waiting
        try {

            // In theory, you would be running your code block right inside
            // this try block right here:_____ and if you're able to
            // meaningfully calculate/evaluate your progress with
            // some int p ranged [0,100], you can pass that through to update the loading
            // bar progress

            // I simulate this "long intense process" by just looping and running
            // sleep() for a bit before updating the progress variable in increments
            // from 0% to 100% over a few seconds
            for (int p = 0; p <= 100; p += 1) {
                loader.setProgress(p); // Push progress values to the loader to update it
                Thread.sleep(250); // Simulating 250ms of work time...
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 5. When Task is complete (or has failed), shut down the loader cleanly
            loader.stopLoading();
            try {
                loadingThread.join(); // Wait for final frame cleanup
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 6. Fresh clear so our completion message prints cleanly
            // Clear Screen + Cursor Home + Show Cursor
            loader.forceTerminalCleanup();
        }
    }

}