package io.github.chehsunliu.itx.worker;

import io.github.chehsunliu.itx.worker.compute.WorkerComputeConfig;
import io.github.chehsunliu.itx.worker.control.WorkerControlConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

// Mode-specific configs are added explicitly by main() — exclude them from the default
// component scan so the unwanted one isn't picked up.
@SpringBootApplication
@ComponentScan(
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {WorkerControlConfig.class, WorkerComputeConfig.class}))
@ConfigurationPropertiesScan("io.github.chehsunliu.itx.impl")
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

    Class<?> modeConfig =
        switch (mode) {
          case "control" -> {
            // Control mode opts the worker into a DB; compute leaves itx.db.provider unset
            // so JPA autoconfig stays dormant.
            System.setProperty(
                "itx.db.provider", System.getenv().getOrDefault("ITX_DB_PROVIDER", "postgres"));
            yield WorkerControlConfig.class;
          }
          case "compute" -> WorkerComputeConfig.class;
          default -> throw new IllegalStateException("unknown --mode: " + mode);
        };

    new SpringApplicationBuilder(WorkerApplication.class, modeConfig)
        .web(WebApplicationType.NONE)
        .run(rest.toArray(new String[0]));
  }
}
