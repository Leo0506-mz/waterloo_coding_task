import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommandTestHttps {
    public static void main(String[] args) {

        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{"http", "-h", "--ignore-stdin",  "google.com"});
//        commands.add(new String[]{"http", "-h",  "google.com"});


        for (String[] command : commands) {
            String commandStr = String.join(" ", command);
            System.out.println("===== Testing Command: " + commandStr + " =====");

            System.out.println("===== Apache Exec: capture =====");
            apacheExecCapture(command);

            System.out.println("===== ProcessBuilder: capture =====");
            processBuilderCapture(command);

            System.out.println("===== Apache Exec: no capture =====");
            apacheExecNoCapture(command);

            System.out.println("===== ProcessBuilder: no capture =====");
            processBuilderNoCapture(command);

        }

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
            builder.redirectInput(new File("/dev/null")); //
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
