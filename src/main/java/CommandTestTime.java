import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class CommandTestTime {
    public static void main(String[] args) {

        List<String[]> commands = new ArrayList<>();
        List<String> executionTimes = new ArrayList<>();


        commands.add(new String[]{"ls", "-l", "./src/documents/ls"});
        commands.add(new String[]{"wc", "-l", "./src/documents/wc/example.txt"});
        commands.add(new String[]{"curl", "-I", "https://www.google.com"});
        commands.add(new String[]{"openssl", "rand", "-base64", "1048576"});
        commands.add(new String[]{"echo", "Hello, World!"});
        commands.add(new String[]{"df", "-h"});

        final double[] time1 = {0}, time2 = {0}, time3 = {0}, time4 = {0};
        int repeat_times = 100;
        for (String[] command : commands) {
            String commandStr = String.join(" ", command);
            String str0 = "command: " + commandStr + "\n";
            System.out.println("===== Testing Command: " + String.join(" ", command) + " =====");
            for (int i = 0; i < repeat_times; i++) {
                List<Runnable> methods = new ArrayList<>();
                methods.add(() -> {
                    System.out.println("===== Apache Exec: capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - Apache Exec: capture");
                    time1[0] += measureTime(() -> apacheExecCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== ProcessBuilder: capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - ProcessBuilder: capture");
                    time2[0] += measureTime(() -> processBuilderCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== Apache Exec: no capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - Apache Exec: no capture");
                    time3[0] += measureTime(() -> apacheExecNoCapture(command));
                });
                methods.add(() -> {
                    System.out.println("===== ProcessBuilder: no capture =====");
                    System.out.println("Order: " + System.nanoTime() + " - ProcessBuilder: no capture");
                    time4[0] += measureTime(() -> processBuilderNoCapture(command));
                });
                Collections.shuffle(methods);
                methods.forEach(Runnable::run);

            }

            time1[0] = time1[0] / repeat_times;
            String str1 = "Apache Exec (capture)" + " - Time: " + time1[0] + " ms\n";
            time2[0] = time2[0] / repeat_times;
            String str2 = "ProcessBuilder (capture)" + " - Time: " + time2[0] + " ms\n";
            time3[0] = time3[0] / repeat_times;
            String str3 = "Apache Exec (no capture)" + " - Time: " + time3[0] + " ms\n";
            time4[0] = time4[0] / repeat_times;
            String str4 = "ProcessBuilder (no capture)" + " - Time: " + time4[0] + " ms\n";
            executionTimes.add(str0 + str1 + str2 + str3 + str4);
        }
        System.out.println("===== Execution Times =====");
        for (String result : executionTimes) {
            System.out.println(result);
        }

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
            System.out.println("Standard Output: " + outputStream);
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

    public static long measureTime(Runnable task) {
        long startTime = System.nanoTime();
        task.run();
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000;
    }
}
