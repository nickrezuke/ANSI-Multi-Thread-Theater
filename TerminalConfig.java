import java.io.IOException;

public class TerminalConfig {
    private static String originalAttributes = null;
    private static int originalWinMode = -1;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private static final int ENABLE_VIRTUAL_TERMINAL_INPUT = 0x0200; // 512

    private static final int SANE_FALLBACK_MODE = 0x00C7; // 199

    private static final String WIN_HANDLE_PRELUDE = "Add-Type -Name Nat -Namespace TC -MemberDefinition '" +
            "[DllImport(\"kernel32.dll\", SetLastError=true)] public static extern IntPtr CreateFile(string name, uint access, uint share, IntPtr sa, uint disposition, uint flags, IntPtr template);"
            +
            "[DllImport(\"kernel32.dll\")] public static extern bool GetConsoleMode(IntPtr h, out int mode);" +
            "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleMode(IntPtr h, int mode);' " +
            "-ErrorAction SilentlyContinue; " +
            "$h = [TC.Nat]::CreateFile('CONIN$', 0xC0000000, 3, [IntPtr]::Zero, 3, 0, [IntPtr]::Zero); ";

    public static int[] getTerminalSize() {
        int defaultWidth = 80;
        int defaultHeight = 24;

        // Check common IDE/Terminal environment variable fallbacks first
        String envCols = System.getenv("COLUMNS");
        String envLines = System.getenv("LINES");
        if (envCols != null && envLines != null) {
            try {
                return new int[] { Integer.parseInt(envCols), Integer.parseInt(envLines) };
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            if (IS_WINDOWS) {
                // Query Windows console buffer width and height
                String output = runPowershell("[System.Console]::WindowWidth; [System.Console]::WindowHeight");
                String[] lines = output.split("\\r?\\n");
                if (lines.length >= 2) {
                    int w = Integer.parseInt(lines[0].trim());
                    int h = Integer.parseInt(lines[1].trim());
                    if (w > 0 && h > 0)
                        return new int[] { w, h };
                }
            } else {
                // Mac/Linux: Try regular stty size first
                String output = runCommand("stty size 2>/dev/null");

                // IDE Terminal Fallback: If regular stty fails, 
                // point explicitly to the tty device
                if (output.isEmpty()) {
                    output = runCommand("stty size < /dev/tty 2>/dev/null");
                }

                if (!output.isEmpty()) {
                    String[] parts = output.trim().split("\\s+");
                    if (parts.length >= 2) {
                        int h = Integer.parseInt(parts[0]);
                        int w = Integer.parseInt(parts[1]);
                        if (w > 0 && h > 0)
                            return new int[] { w, h };
                    }
                }
            }
        } catch (Exception e) {
            // Absorb exceptions and let system fall back safely, we already defined defaults
        }

        return new int[] { defaultWidth, defaultHeight };
    }

    public static void setRawMode() {
        try {
            if (IS_WINDOWS) {
                // Stage 1: Query and save the REAL current console mode via CONIN$,
                // capturing it back over an ordinary (piped) process. This is the
                // fix for the previous bug where the query ran against this
                // process's own pipe-backed stdin handle and returned nothing,
                // leaving originalWinMode permanently at -1.
                String output = runPowershell(WIN_HANDLE_PRELUDE +
                        "$m = 0; [TC.Nat]::GetConsoleMode($h, [ref]$m); Write-Output $m");
                if (!output.isEmpty()) {
                    try {
                        originalWinMode = Integer.parseInt(output.trim());
                    } catch (NumberFormatException nfe) {
                        originalWinMode = -1;
                    }
                }

                // Stage 2: Strip ENABLE_LINE_INPUT (0x0002) and ENABLE_ECHO_INPUT
                // (0x0004) for raw, unbuffered reads, and set
                // ENABLE_VIRTUAL_TERMINAL_INPUT so arrow keys arrive as the ANSI
                // escape sequences InteractiveLoader expects.
                new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        WIN_HANDLE_PRELUDE +
                                "$m = 0; [TC.Nat]::GetConsoleMode($h, [ref]$m); " +
                                "$m = $m -band -3 -band -5; " + // clear bits 0x0002 and 0x0004
                                "$m = $m -bor " + ENABLE_VIRTUAL_TERMINAL_INPUT + "; " + // set bit 0x0200
                                "[TC.Nat]::SetConsoleMode($h, $m)")
                        .start().waitFor();
            } else {
                // Mac & Linux: Save current state, then 
                // strictly disable canonical mode AND echo
                originalAttributes = runCommand("stty -g < /dev/tty");
                
                new ProcessBuilder("sh", "-c", "stty raw -echo isig < /dev/tty").start().waitFor();
            }
        } catch (Exception e) {
            // Fallback if system environment limits access
        }
    }


    public static void restoreMode() {
        try {
            if (IS_WINDOWS) {
                // Restore the captured mode, or fall back to a sane default if
                // we never managed to capture one (e.g. setRawMode() failed).
                int modeToRestore = (originalWinMode != -1) ? originalWinMode : SANE_FALLBACK_MODE;
                new ProcessBuilder("powershell", "-NoProfile", "-Command", WIN_HANDLE_PRELUDE + "[TC.Nat]::SetConsoleMode($h, " + modeToRestore + ")")
                    .start().waitFor();
                new ProcessBuilder("powershell", "-NoProfile", "-Command", "[System.Console]::ResetColor()")
                    .start().waitFor();
            } else if (originalAttributes != null) {
                new ProcessBuilder("sh", "-c", "stty " + originalAttributes + " < /dev/tty").start().waitFor();
            } else {
                new ProcessBuilder("sh", "-c", "stty sane < /dev/tty").start().waitFor();
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

    private static String runPowershell(String script) throws IOException, InterruptedException {
        Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command", script).start();
        p.waitFor();
        return new String(p.getInputStream().readAllBytes()).trim();
    }
}