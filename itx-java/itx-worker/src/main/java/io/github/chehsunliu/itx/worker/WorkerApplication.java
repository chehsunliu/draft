package io.github.chehsunliu.itx.worker;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;

@SpringBootApplication(
    exclude = {
      DataSourceAutoConfiguration.class,
      DataSourceTransactionManagerAutoConfiguration.class,
      JdbcTemplateAutoConfiguration.class,
    })
public class WorkerApplication {

  public static void main(String[] args) {
    String mode = "control";
    List<String> rest = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if (a.startsWith("--mode=")) {
        mode = a.substring("--mode=".length());
      } else if ("--mode".equals(a) && i + 1 < args.length) {
        mode = args[++i];
      } else if ("--log-level".equals(a) && i + 1 < args.length) {
        System.setProperty("logging.level.root", args[++i]);
      } else {
        rest.add(a);
      }
    }
    System.setProperty("itx.worker.mode", mode);
    System.setProperty("spring.profiles.active", mode);

    new SpringApplicationBuilder(WorkerApplication.class)
        .web(WebApplicationType.NONE)
        .build()
        .run(rest.toArray(new String[0]));
  }
}
