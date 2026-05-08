package io.github.chehsunliu.itx.worker;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("io.github.chehsunliu.itx.impl.repo.entity")
@EnableJpaRepositories("io.github.chehsunliu.itx.impl.repo.jpa")
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

    String dbProvider = System.getenv().getOrDefault("ITX_DB_PROVIDER", "postgres");
    // Compute mode never reads from the DB, so we keep its profile DB-less to
    // avoid forcing DB env vars on compute pods.
    String activeProfiles = "control".equals(mode) ? mode + "," + dbProvider : mode;
    System.setProperty("spring.profiles.active", activeProfiles);

    new SpringApplicationBuilder(WorkerApplication.class)
        .web(WebApplicationType.NONE)
        .build()
        .run(rest.toArray(new String[0]));
  }
}
