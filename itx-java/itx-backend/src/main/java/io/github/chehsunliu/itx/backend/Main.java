package io.github.chehsunliu.itx.backend;

import io.javalin.Javalin;
import sun.misc.Signal;

public final class Main {
  private Main() {}

  private record Args(String host, int port) {
    static Args parse(String[] argv) {
      String host = "127.0.0.1";
      int port = 8080;
      int i = 0;
      while (i < argv.length) {
        switch (argv[i]) {
          case "--host" -> host = argv[++i];
          case "--port" -> port = Integer.parseInt(argv[++i]);
          case "--log-level" -> System.setProperty("itx.log.level", argv[++i]);
          default -> {}
        }
        i++;
      }
      return new Args(host, port);
    }
  }

  public static void main(String[] argv) {
    Args args = Args.parse(argv);
    AppState state = AppState.fromEnv();
    Javalin app = Module.build(state);

    // The integration test driver asserts proc.wait() == 0 on SIGINT. Default JVM behavior
    // exits 130 on signal, so trap SIGINT/SIGTERM, drain the engine, and exit cleanly.
    Runnable shutdown =
        () -> {
          try {
            app.stop();
          } catch (Exception ignored) {
            // best-effort shutdown
          }
          try {
            state.close();
          } catch (Exception ignored) {
            // best-effort close
          }
          Runtime.getRuntime().halt(0);
        };
    Signal.handle(new Signal("INT"), s -> shutdown.run());
    Signal.handle(new Signal("TERM"), s -> shutdown.run());

    app.start(args.host(), args.port());
  }
}
