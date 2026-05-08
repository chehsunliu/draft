package io.github.chehsunliu.itx.backend;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class BackendApplication {

  public static void main(String[] args) {
    // The integration test driver spawns the binary as `itx-backend --host X --port Y`,
    // i.e. with space-separated values. Spring's --key=value binding doesn't accept the
    // space form, so translate to system properties for server.address / server.port
    // before bootstrapping.
    List<String> rest = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if ("--host".equals(a) && i + 1 < args.length) {
        System.setProperty("server.address", args[++i]);
      } else if ("--port".equals(a) && i + 1 < args.length) {
        System.setProperty("server.port", args[++i]);
      } else if ("--log-level".equals(a) && i + 1 < args.length) {
        System.setProperty("logging.level.root", args[++i]);
      } else {
        rest.add(a);
      }
    }
    ConfigurableApplicationContext ctx =
        SpringApplication.run(BackendApplication.class, rest.toArray(new String[0]));
    Signals.installCleanShutdown(ctx);
  }
}
