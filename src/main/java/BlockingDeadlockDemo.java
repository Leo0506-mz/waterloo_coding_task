import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

public class BlockingDeadlockDemo {
    public static void main(String[] args) throws Exception {

        System.out.println("Running ProcessBuilder example...");
        runProcessBuilderExample();

        System.out.println("Running Apache Commons Exec example...");
        runApacheExecExample();
    }

    public static void runProcessBuilderExample() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "yes Line | head -n 100000");
        Process process = pb.start();
        boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            System.out.println("ProcessBuilder deadlock sample。");
            process.destroyForcibly();
        } else {
            System.out.println("ProcessBuilder exit code: " + process.exitValue());
        }
    }

    public static void runApacheExecExample() throws Exception {
        CommandLine cmd = new CommandLine("sh");
        cmd.addArgument("-c");
        cmd.addArgument("yes Line | head -n 100000", false);

        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
        executor.execute(cmd);
    }
}
