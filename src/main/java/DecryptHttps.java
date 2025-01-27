import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.environment.EnvironmentUtils;

import java.util.Map;

public class DecryptHttps {
    public static void main(String[] args) {
        try {
            CommandLine cmd = new CommandLine("https");
            cmd.addArgument("-h");
            cmd.addArgument("www.google.com");

            Map<String, String> env = EnvironmentUtils.getProcEnvironment();
            env.put("SSLKEYLOGFILE", "sslkeys.log");

            DefaultExecutor executor = new DefaultExecutor();
            executor.execute(cmd, env);

            System.out.println("SSLKEYLOGFILE generated at sslkeys.log");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
