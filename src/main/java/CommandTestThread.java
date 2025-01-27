import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandTestThread {
    public static void main(String[] args) {

        List<String[]> commands = new ArrayList<>();
        List<String> executionResults = new ArrayList<>();

        commands.add(new String[]{"ls", "-l", "./src/documents/ls"});
        commands.add(new String[]{"wc", "-l", "./src/documents/wc/example.txt"});
        commands.add(new String[]{"curl", "-I", "https://www.google.com"});
        commands.add(new String[]{"openssl", "rand", "-base64", "1048576"});
        commands.add(new String[]{"echo", "Hello, World!"});
        commands.add(new String[]{"df", "-h"});

//        commands.add(new String[]{"https", "-h","google.com"});


        for (String[] command : commands) {
            String commandStr = String.join(" ", command);
            System.out.println("===== Testing Command: " + commandStr + " =====");
            String resultHeader = "Command: " + commandStr + "\n";

            System.out.println("===== Apache Exec: capture =====");
            long Threads1 = measureThreadUsage(() -> apacheExecCapture(command));

            System.out.println("===== ProcessBuilder: capture =====");
            long Threads2 = measureThreadUsage(() -> processBuilderCapture(command));

            System.out.println("===== Apache Exec: no capture =====");
            long Threads3 = measureThreadUsage(() -> apacheExecNoCapture(command));

            System.out.println("===== ProcessBuilder: no capture =====");
            long Threads4 = measureThreadUsage(() -> processBuilderNoCapture(command));

            System.out.println();
            String result1 = String.format("Apache Exec (capture) - Threads Used: %d\n", Threads1);
            String result2 = String.format("ProcessBuilder (capture) - Threads Used: %d\n", Threads2);
            String result3 = String.format("Apache Exec (no capture) - Threads Used: %d\n", Threads3);
            String result4 = String.format("ProcessBuilder (no capture) - Threads Used: %d\n", Threads4);

            executionResults.add(resultHeader + result1 + result2 + result3 + result4);
            System.out.println("===== Execution Thread Usage =====");
            for (String result : executionResults) {
                System.out.println(result);
            }
        }



    }

    public static long measureThreadUsage(Runnable task) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final int initialThreadCount = threadMXBean.getThreadCount();
        final AtomicBoolean running = new AtomicBoolean(true);
        final long[] maxThreadCount = {initialThreadCount};

        Thread monitor = new Thread(() -> {
            while (running.get()) {
                int currentThreadCount = threadMXBean.getThreadCount();
                if (currentThreadCount > maxThreadCount[0]) {
                    maxThreadCount[0] = currentThreadCount;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        monitor.start();

        task.run();

        running.set(false);
        try {
            monitor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return maxThreadCount[0] - initialThreadCount;
    }


    public static void apacheExecCapture(String[] command) {
        try {
            CommandLine cmd = new CommandLine(command[0]);
            for (int i = 1; i < command.length; i++) {
                cmd.addArgument(command[i]);
            }
            DefaultExecutor executor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmd);
            System.out.println("Standard Output: " + outputStream.toString());
            System.out.println("Standard Error: " + errorStream.toString());
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    public static void apacheExecNoCapture(String[] command) {
        try {
            CommandLine cmd = new CommandLine(command[0]);
            for (int i = 1; i < command.length; i++) {
                cmd.addArgument(command[i]);
            }

            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            executor.execute(cmd);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    public static void processBuilderCapture(String[] command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(false);
            Process process = builder.start();

            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            System.out.println("Standard Output:");
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Standard Error:");
            while ((line = stderrReader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }


    public static void processBuilderNoCapture(String[] command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            Process process = builder.start();
            process.waitFor();
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }
}
