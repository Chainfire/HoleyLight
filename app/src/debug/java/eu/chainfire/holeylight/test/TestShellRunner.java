package eu.chainfire.holeylight.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

public class TestShellRunner {
    private static final String TAG = "TestShell";

    private static void runCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            char[] buf = new char[1024];
            while (bufferedReader.read(buf) == 1024) {
            }
        } catch (Exception e) {
            log("Exception: %s", e);
            e.printStackTrace();
        }
    }

    private static void log(String msg, Object... params) {
        if ((params != null) && (params.length > 0)) {
            msg = String.format(Locale.ENGLISH, msg, params);
        }

        runCommand(String.format("log -p d -t HoleyLight/%s %s", TAG, msg));
    }

    private static class LogcatThread extends Thread {
        @Override
        public void run() {
            log("ThreadStart");

            try {
                Process process = Runtime.getRuntime().exec("logcat");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("HoleyLight/Test") && !line.contains("HoleyLight/TestShell")) {
                        if (line.contains("EXECUTE")) {
                            String command = line.substring(line.indexOf("EXECUTE") + "EXECUTE ".length());
                            log("Executing: [" + command + "]");
                            runCommand(command);
                        }
                    }
                }
            } catch (Exception e) {
                log("Exception: %s", e);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        log( "Start");
        runCommand("logcat -c");

        LogcatThread logcatThread = new LogcatThread();
        logcatThread.start();

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {
            }
        }
    }
}
