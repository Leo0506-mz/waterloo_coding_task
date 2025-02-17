import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class CommandTestMemory {
    public static void main(String[] args) {

        List<String[]> commands = new ArrayList<>();
        List<String> executionResults = new ArrayList<>();

        commands.add(new String[]{"ls", "-l", "./src/documents/ls"});
        commands.add(new String[]{"du", "-sh", "./src/documents"});

        commands.add(new String[]{"curl", "-I", "https://www.google.com"});
        commands.add(new String[]{"ping", "-c", "1", "www.google.com"});

        commands.add(new String[]{"openssl", "rand", "-base64", "1048576"});
        commands.add(new String[]{"sha256sum",  "./src/documents/ls/test_file_1.txt"});

        commands.add(new String[]{"df",  "-h"});
        commands.add(new String[]{"top",  "-l", "1"});

        int warmup_times = 10;
        int repeat_times = 100;
        for (String[] command : commands) {
            final double[] memory1 = {0}, memory2 = {0}, memory3 = {0}, memory4 = {0};
            String commandStr = String.join(" ", command);
            String str0 = "command: " + commandStr + "\n";
            System.out.println("===== Testing Command: " + String.join(" ", command) + " =====");

            for (int i = 0; i < warmup_times; i++) {
                List<Runnable> warmupMethods = new ArrayList<>();
                warmupMethods.add(() -> apacheExecCapture(command));
                warmupMethods.add(() -> processBuilderCapture(command));
                warmupMethods.add(() -> apacheExecNoCapture(command));
                warmupMethods.add(() -> processBuilderNoCapture(command));
                Collections.shuffle(warmupMethods);
                warmupMethods.forEach(Runnable::run);
            }

            for (int i = 0; i < repeat_times; i++) {
                List<Runnable> methods = new ArrayList<>();
                methods.add(() -> {
                    System.out.println("===== Apache Exec: capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - Apache Exec: capture");
                    memory1[0] += measureExecutionWithPeak(() -> apacheExecCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== ProcessBuilder: capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - ProcessBuilder: capture");
                    memory2[0] += measureExecutionWithPeak(() -> processBuilderCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== Apache Exec: no capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - Apache Exec: no capture");
                    memory3[0] += measureExecutionWithPeak(() -> apacheExecNoCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== ProcessBuilder: no capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - ProcessBuilder: no capture");
                    memory4[0] += measureExecutionWithPeak(() -> processBuilderNoCapture(command));
                });
                Collections.shuffle(methods);
                methods.forEach(Runnable::run);

            }

            memory1[0] = memory1[0] / repeat_times;
            String str1 = String.format("Apache Exec (capture) - Memory: %.2f MB\n", memory1[0] / (1024.0 * 1024.0));

            memory2[0] = memory2[0] / repeat_times;
            String str2 = String.format("ProcessBuilder (capture) - Memory: %.2f MB\n", memory2[0] / (1024.0 * 1024.0));

            memory3[0] = memory3[0] / repeat_times;
            String str3 = String.format("Apache Exec (no capture) - Memory: %.2f MB\n", memory3[0] / (1024.0 * 1024.0));

            memory4[0] = memory4[0] / repeat_times;
            String str4 = String.format("ProcessBuilder (no capture) - Memory: %.2f MB\n", memory4[0] / (1024.0 * 1024.0));
            executionResults.add(str0 + str1 + str2 + str3 + str4);
        }
        System.out.println("===== Execution Times =====");
        for (String result : executionResults) {
            System.out.println(result);
        }
    }


    public static long measureExecutionWithPeak(Runnable task) {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Runtime runtime = Runtime.getRuntime();

        AtomicBoolean running = new AtomicBoolean(true);
        long[] peakMemory = {0};

        Thread monitorThread = new Thread(() -> {
            while (running.get()) {
                long currentUsedMem = runtime.totalMemory() - runtime.freeMemory();
                synchronized (peakMemory) {
                    if (currentUsedMem > peakMemory[0]) {
                        peakMemory[0] = currentUsedMem;
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        monitorThread.start();
        task.run();

        running.set(false);
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return peakMemory[0];
    }
    public static void apacheExecCapture(String... command) {
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

    public static void apacheExecNoCapture(String... command) {
        try {
            CommandLine cmd = new CommandLine(command[0]);
            for (int i = 1; i < command.length; i++) {
                cmd.addArgument(command[i]);
            }

            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out));
            executor.execute(cmd);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }

    public static void processBuilderCapture(String... command) {
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

    public static void processBuilderNoCapture(String... command) {
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

