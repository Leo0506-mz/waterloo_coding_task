import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class CommandTestMemory {
    public static void main(String[] args) {

        List<String[]> commands = new ArrayList<>();
        List<String> executionResults = new ArrayList<>();

        commands.add(new String[]{"ls", "-l", "./src/documents/ls"});
        commands.add(new String[]{"wc", "-l", "./src/documents/wc/example.txt"});
        commands.add(new String[]{"curl", "-I", "https://www.google.com"});
        commands.add(new String[]{"openssl", "rand", "-base64", "1048576"});
        commands.add(new String[]{"echo", "Hello, World!"});
        commands.add(new String[]{"df", "-h"});

        for (String[] command : commands) {
            int repeatTimes = 100;
            String commandStr = String.join(" ", command);
            System.out.println("===== Testing Command: " + commandStr + " =====");
            String resultHeader = "Command: " + commandStr + "\n";

            double totalMemory1 = 0, totalMemory2 = 0, totalMemory3 = 0, totalMemory4 = 0;

            for (int i = 0; i < repeatTimes; i++) {
                System.out.println("Iteration " + (i + 1) + " of " + repeatTimes);
                System.out.println("===== Apache Exec: capture =====");
                Long m1 = measureExecution(() -> apacheExecCapture(command));
                totalMemory1 += m1;
                System.out.println("===== ProcessBuilder: capture =====");
                Long m2 = measureExecution(() -> processBuilderCapture(command));
                totalMemory2 += m2;
                System.out.println("===== Apache Exec: no capture =====");
                Long m3 = measureExecution(() -> apacheExecNoCapture(command));
                totalMemory3 += m3;
                System.out.println("===== ProcessBuilder: no capture =====");
                Long m4 = measureExecution(() -> processBuilderNoCapture(command));
                totalMemory4 += m4;
            }

            double avgMemory1 = totalMemory1 / repeatTimes;
            String result1 = String.format("Apache Exec (capture) - Average Memory: %.2f KB\n", avgMemory1 / 1024);

            double avgMemory2 = totalMemory2 / repeatTimes;
            String result2 = String.format("ProcessBuilder (capture) -  Average Memory: %.2f KB\n", avgMemory2 / 1024);

            double avgMemory3 = totalMemory3 / repeatTimes;
            String result3 = String.format("Apache Exec (no capture) - Average Memory: %.2f KB\n", avgMemory3 / 1024);

            double avgMemory4 = totalMemory4 / repeatTimes;
            String result4 = String.format("ProcessBuilder (no capture) - Average Memory: %.2f KB\n\n", avgMemory4 / 1024);

            executionResults.add(resultHeader + result1 + result2 + result3 + result4);
            System.out.println();
        }

        System.out.println("===== Execution Times and Memory Usage =====");
        for (String result : executionResults) {
            System.out.println(result);
        }
    }

    public static Long measureExecution(Runnable task) {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Runtime runtime = Runtime.getRuntime();
        long beforeUsedMem = runtime.totalMemory() - runtime.freeMemory();

        task.run();

        long afterUsedMem = runtime.totalMemory() - runtime.freeMemory();

        return afterUsedMem - beforeUsedMem;
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

