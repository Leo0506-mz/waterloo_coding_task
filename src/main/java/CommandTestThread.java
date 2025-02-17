import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandTestThread {
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
            final double[] thread1 = {0}, thread2 = {0}, thread3 = {0}, thread4 = {0};
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
                    thread1[0] += measureThreadUsage(() -> apacheExecCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== ProcessBuilder: capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - ProcessBuilder: capture");
                    thread2[0] += measureThreadUsage(() -> processBuilderCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== Apache Exec: no capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - Apache Exec: no capture");
                    thread3[0] += measureThreadUsage(() -> apacheExecNoCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== ProcessBuilder: no capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - ProcessBuilder: no capture");
                    thread4[0] += measureThreadUsage(() -> processBuilderNoCapture(command));
                });
                Collections.shuffle(methods);
                methods.forEach(Runnable::run);

            }

            thread1[0] = thread1[0] / repeat_times;
            String str1 = "Apache Exec (capture)" + " - Thread: " + thread1[0] + "\n";
            thread2[0] = thread2[0] / repeat_times;
            String str2 = "ProcessBuilder (capture)" + " - Thread: " + thread2[0] + "\n";
            thread3[0] = thread3[0] / repeat_times;
            String str3 = "Apache Exec (no capture)" + " - Thread: " + thread3[0] + "\n";
            thread4[0] = thread4[0] / repeat_times;
            String str4 = "ProcessBuilder (no capture)" + " - Thread: " + thread4[0] + "\n";
            executionResults.add(str0 + str1 + str2 + str3 + str4);
        }
        System.out.println("===== Execution Times =====");
        for (String result : executionResults) {
            System.out.println(result);
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
                    Thread.sleep(1);
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
