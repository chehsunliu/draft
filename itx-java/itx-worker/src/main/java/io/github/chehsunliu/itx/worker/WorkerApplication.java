package io.github.chehsunliu.itx.worker;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"io.github.chehsunliu.itx.worker", "io.github.chehsunliu.itx.impl"})
@ConfigurationPropertiesScan("io.github.chehsunliu.itx.impl")
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
    // Compute mode keeps DB autoconfig dormant by leaving itx.db.provider unset (the
    // application.yml default is the empty string). Control mode opts in.
    if ("control".equals(mode)) {
      System.setProperty(
          "itx.db.provider", System.getenv().getOrDefault("ITX_DB_PROVIDER", "postgres"));
    }

    SpringApplication.run(WorkerApplication.class, rest.toArray(new String[0]));
  }
}
