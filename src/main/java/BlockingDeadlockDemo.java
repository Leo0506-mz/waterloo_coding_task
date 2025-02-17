import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

public class BlockingDeadlockDemo {
    public static void main(String[] args) throws Exception {
        // 演示 ProcessBuilder 示例：未消费子进程输出，可能导致缓冲区填满从而阻塞或死锁
        System.out.println("Running ProcessBuilder example (可能会因输出缓冲区满而阻塞)...");
        runProcessBuilderExample();

        // 演示 Apache Commons Exec 示例：通过 PumpStreamHandler 自动处理 I/O，避免阻塞与死锁
        System.out.println("Running Apache Commons Exec example (自动处理 I/O 避免阻塞)...");
        runApacheExecExample();
    }

    // 使用 ProcessBuilder 启动子进程，但不消费其输出流，可能导致缓冲区填满从而阻塞或死锁
    public static void runProcessBuilderExample() throws Exception {
        // 使用 shell 命令生成大量输出：
        // 通过 "yes Line" 不断输出 "Line"，再用 "head -n 100000" 限制输出为 100000 行
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "yes Line | head -n 100000");
        Process process = pb.start();
        // 注意：此处未读取 process.getInputStream() 和 process.getErrorStream()，可能导致缓冲区填满而阻塞
        boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            System.out.println("ProcessBuilder 示例: 进程在 10 秒内未结束，可能发生死锁。");
            process.destroyForcibly();
        } else {
            System.out.println("ProcessBuilder exit code: " + process.exitValue());
        }
    }

    // 使用 Apache Commons Exec 启动子进程，自动通过 PumpStreamHandler 处理输出，避免阻塞或死锁
    public static void runApacheExecExample() throws Exception {
        // 构造调用 shell 命令生成大量输出
        CommandLine cmd = new CommandLine("sh");
        cmd.addArgument("-c");
        // 第二个参数 false 表示该参数不会被额外转义
        cmd.addArgument("yes Line | head -n 100000", false);

        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
        executor.execute(cmd);
    }
}
