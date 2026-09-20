// This class acts as my heavy processing example application
public class ExampleTask {
    public static void main(String[] args) {
        // This serves as an example file / existing code
        // segment that would like to use a loader.
        // Here are the steps to do so in your own code:

        // ...
        // ...
        // ... ▲ your existing code ▲
        // ...

        // 0. Evaluate the desired loader we just typed in
        String userPreference = (args.length > 0) ? args[0] : null;
        // Here we passed one in when we ran ExampleTask, but you can
        // do whatever you like, use a specific one, random one, etc.

        // 1. Create the loader you wish to use...
        // We can create it using our factory...
        Loader loader = LoaderFactory.createLoaderInstance(userPreference);

        // If you'd like to, you can pass in a specific one...
        // Loader loader = LoaderFactory.createLoaderInstance("Donut");
        // ...or pass nothing to do a random one...
        // Loader loader = LoaderFactory.createLoaderInstance();

        // 2. Register an emergency thread that fires exclusively when Control+C is hit.
        // This acts as a bulletproof structural cleanup routine in case your users are
        // a little impatient and like hitting ctrl-c before things are finished...
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (loader != null) {
                // Forcibly drop out of raw terminal structures immediately before closing
                loader.forceTerminalCleanup();
            } else {
                // Generic fallback if no loader was actively generated
                System.out.print("\u001b[2J" + "\u001b[H" + "\u001b[?25h");
                // ANSI codes for [Clear Screen] + [Cursor Home] + [Show Cursor]
                System.out.flush();
            }
        }));

        // 3. Just spin up the desired loader on a new thread, and start it!
        Thread loadingThread = new Thread(loader);
        loadingThread.start();

        // 4. Perform your heavy task (inside a try block))
        try {
            // MillionThings.sort();
            // TravelingSalesman.solve();
            // ...whatever etc..

            // In theory, you would be running your code block right inside
            // this try block right here:___ and if you're able to
            // meaningfully calculate/evaluate your progress with
            // some int p ranged [0,100], you can pass that through to setProgress
            // to pereodically update the loading bar progress

            // I simulate this "long intense process" by just looping and running
            // sleep() for a bit before updating the progress variable in increments
            // from 0% to 100% over a few seconds. This provides us with a nice long time
            // to see the loading screen in this example run.
            for (double p = 0; p <= 100; p = p + 0.01) {
                // This setProgress should be called whenever you have meaningful updates...
                loader.setProgress(p); // Push progress values to the loader to update it

                // Here I just simulate 300ms of work time per unit of percentage,
                // to give you enough time to see and substantially watch the loaders
                Thread.sleep(3);
            }

            // After all your processing, its finally time to join back up and resume
            // control...
            // Now its time to deal with the finally block before moving on
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // because I KNOW you're impatient and will be using ctrl-c...
        } finally {
            // 5. When Task is complete (or has failed), shut down the loader cleanly
            loader.stopLoading();
            try {
                loadingThread.join(); // Wait for final frame cleanup
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 6. Fresh clear so we return to the state how we were before the loader
            loader.forceTerminalCleanup();
        }

        // 7. By this point everything should be cleared up and ended
        // Continue with the rest of your program's following code

        // ...
        // ... ▼ your existing code ▼
        // ...
        // ...
    }
}