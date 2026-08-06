import java.io.IOException;

public class TerminalConfig {
    private static String originalAttributes = null;
    private static int originalWinMode = -1;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static void setRawMode() {
        try {
            if (IS_WINDOWS) {
                // Stage 1: Query and save the current Windows Console Mode flags
                Process p = new ProcessBuilder("powershell", "-Command",
                        "Add-Type 'using System; using System.Runtime.InteropServices; public class Win { " +
                                "[DllImport(\"kernel32.dll\")] public static extern IntPtr GetStdHandle(int n); " +
                                "[DllImport(\"kernel32.dll\")] public static extern bool GetConsoleMode(IntPtr h, out int m); }'; "
                                + "$h = [Win]::GetStdHandle(-10); [Win]::GetConsoleMode($h, [ref]$m); return $m")
                        .start();
                p.waitFor();
                String output = new String(p.getInputStream().readAllBytes()).trim();
                if (!output.isEmpty()) {
                    originalWinMode = Integer.parseInt(output);
                }

                // Stage 2: Strip ENABLE_LINE_INPUT (0x0002) and ENABLE_ECHO_INPUT (0x0004)
                new ProcessBuilder("powershell", "-Command",
                        "$p = [System.Console]::In; $h = [System.IntPtr]::Zero; " +
                                "Add-Type 'using System; using System.Runtime.InteropServices; public class Win { " +
                                "[DllImport(\"kernel32.dll\")] public static extern IntPtr GetStdHandle(int n); " +
                                "[DllImport(\"kernel32.dll\")] public static extern bool GetConsoleMode(IntPtr h, out int m); "
                                +
                                "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleMode(IntPtr h, int m); }'; "
                                +
                                "$h = [Win]::GetStdHandle(-10); [Win]::GetConsoleMode($h, [ref]$m); " +
                                "$m = $m -band -3 -band -5; " + // Clear bits 0x0002 and 0x0004
                                "[Win]::SetConsoleMode($h, $m)")
                        .inheritIO().start().waitFor();
            } else {
                // Mac & Linux: Save current state, then strictly
                // disable canonical mode AND echo
                originalAttributes = runCommand("stty -g");
                new ProcessBuilder("sh", "-c", "stty raw -echo isig < /dev/tty").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Fallback if system environment limits access
        }
    }

    public static void restoreMode() {
        try {
            if (IS_WINDOWS) {
                // Windows: Restore original console mode flags if captured
                if (originalWinMode != -1) {
                    new ProcessBuilder("powershell", "-Command",
                            "Add-Type 'using System; using System.Runtime.InteropServices; public class Win { " +
                                    "[DllImport(\"kernel32.dll\")] public static extern IntPtr GetStdHandle(int n); " +
                                    "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleMode(IntPtr h, int m); }'; "
                                    +
                                    "$h = [Win]::GetStdHandle(-10); [Win]::SetConsoleMode($h, " + originalWinMode + ")")
                            .inheritIO().start().waitFor();
                }
                new ProcessBuilder("powershell", "-Command", "[System.Console]::ResetColor()").inheritIO().start()
                        .waitFor();
            } else if (originalAttributes != null) {
                // Mac & Linux: Re-apply original terminal attributes saved at startup
                new ProcessBuilder("sh", "-c", "stty " + originalAttributes +
                        " < /dev/tty").inheritIO().start().waitFor();
            } else {
                // Fallback: If original attributes were lost due to 
                // a crash, force a standard sanity restore
                new ProcessBuilder("sh", "-c", "stty sane < /dev/tty").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Fail silently
        }
    }

    private static String runCommand(String cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder("sh", "-c", cmd).start();
        p.waitFor();
        return new String(p.getInputStream().readAllBytes()).trim();
    }
}
