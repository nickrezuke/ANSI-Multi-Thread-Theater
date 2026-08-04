import java.io.IOException;

public class TerminalConfig {
    private static String originalAttributes = null;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static void setRawMode() {
        try {
            if (IS_WINDOWS) {
                // Windows: Disable both Line Input (0x0002) AND Echo Input (0x0004) using a
                // deeper PowerShell script
                // We use bitwise operations to clear the echo and line buffering states
                // completely
                new ProcessBuilder("powershell", "-Command",
                        "$p = [System.Console]::In; $h = [System.IntPtr]::Zero; " +
                                "Add-Type 'using System; using System.Runtime.InteropServices; public class Win { " +
                                "[DllImport(\"kernel32.dll\")] public static extern IntPtr GetStdHandle(int n); " +
                                "[DllImport(\"kernel32.dll\")] public static extern boolean GetConsoleMode(IntPtr h, out int m); "
                                +
                                "[DllImport(\"kernel32.dll\")] public static extern boolean SetConsoleMode(IntPtr h, int m); }'; "
                                +
                                "$h = [Win]::GetStdHandle(-10); " + // STD_INPUT_HANDLE = -10
                                "[Win]::GetConsoleMode($h, [ref]$m); " +
                                // m = m & ~ENABLE_LINE_INPUT (0x0002) & ~ENABLE_ECHO_INPUT (0x0004)
                                "$m = $m -band -3 -band -5; " +
                                "[Win]::SetConsoleMode($h, $m)")
                        .inheritIO().start().waitFor();
            } else {
                // Mac / Linux: Save current state, then strictly disable canonical mode AND
                // echo
                originalAttributes = runCommand("stty -g");

                // CRUCIAL: We use inheritIO() so the command modifies the EXACT terminal window
                // running your app,
                // and we combine 'raw' and '-echo' into a single execution statement.
                new ProcessBuilder("sh", "-c", "stty raw -echo isig < /dev/tty").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            // Fallback if system environment limits access
        }
    }

    public static void restoreMode() {
        try {
            if (IS_WINDOWS) {
                // Windows: Reset console state back to default profiles
                new ProcessBuilder("powershell", "-Command", "[System.Console]::ResetColor()").inheritIO().start().waitFor();
            } else if (originalAttributes != null) {
                // Mac / Linux: Re-apply original terminal attributes saved at startup
                new ProcessBuilder("sh", "-c", "stty " + originalAttributes + " < /dev/tty").inheritIO().start().waitFor();
            } else {
                // Fallback: If original attributes were lost due to a crash, force a standard sanity restore
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
