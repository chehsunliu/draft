package io.github.chehsunliu.itx.backend;

import org.springframework.context.ConfigurableApplicationContext;
import sun.misc.Signal;

/**
 * Installs SIGINT/SIGTERM handlers that close the Spring context and exit with code 0.
 *
 * <p>By default the JVM exits with 128 + signal-number on signal, so SIGINT yields exit code 130.
 * The integration tests assert {@code proc.wait() == 0}, matching the Rust/Go binaries which
 * intercept the signal and exit cleanly. Mirroring that here keeps the Python test driver
 * language-agnostic.
 */
@SuppressWarnings("removal")
final class Signals {
  private Signals() {}

  static void installCleanShutdown(ConfigurableApplicationContext ctx) {
    Signal.handle(new Signal("INT"), s -> shutdown(ctx));
    Signal.handle(new Signal("TERM"), s -> shutdown(ctx));
  }

  private static void shutdown(ConfigurableApplicationContext ctx) {
    try {
      ctx.close();
    } finally {
      System.exit(0);
    }
  }
}
