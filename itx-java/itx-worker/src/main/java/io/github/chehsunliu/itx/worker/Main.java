package io.github.chehsunliu.itx.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.impl.email.HttpEmailClient;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import io.github.chehsunliu.itx.impl.repo.mariadb.MariaDbRepoFactory;
import io.github.chehsunliu.itx.impl.repo.postgres.PostgresRepoFactory;
import io.github.chehsunliu.itx.worker.compute.ComputeDispatcher;
import io.github.chehsunliu.itx.worker.control.ControlDispatcher;
import io.github.chehsunliu.itx.worker.run.QueueRunner;
import java.util.List;
import sun.misc.Signal;

public final class Main {
  private Main() {}

  public static void main(String[] argv) {
    String mode = "control";
    int i = 0;
    while (i < argv.length) {
      String a = argv[i];
      if (a.startsWith("--mode=")) {
        mode = a.substring("--mode=".length());
      } else if (a.equals("--mode")) {
        mode = argv[++i];
      } else if (a.equals("--log-level")) {
        System.setProperty("itx.log.level", argv[++i]);
      }
      i++;
    }

    MessageQueueFactory queueFactory = queueFactory();
    Wired wired = wire(mode, queueFactory);
    QueueRunner.runQueueLoops(wired.queues(), wired.handler(), true);
    Signal.handle(new Signal("INT"), s -> Runtime.getRuntime().halt(0));
  }

  private record Wired(List<MessageQueue> queues, MessageHandler handler) {}

  private static MessageQueueFactory queueFactory() {
    String provider = System.getenv().getOrDefault("ITX_QUEUE_PROVIDER", "sqs");
    return switch (provider) {
      case "sqs" -> SqsMessageQueueFactory.fromEnv();
      case "rabbitmq" -> RabbitMessageQueueFactory.fromEnv();
      default -> throw new IllegalStateException("unknown ITX_QUEUE_PROVIDER: " + provider);
    };
  }

  private static Wired wire(String mode, MessageQueueFactory factory) {
    return switch (mode) {
      case "control" -> {
        String dbProvider = System.getenv().getOrDefault("ITX_DB_PROVIDER", "postgres");
        RepoFactory repoFactory =
            switch (dbProvider) {
              case "postgres" -> PostgresRepoFactory.fromEnv();
              case "mariadb" -> MariaDbRepoFactory.fromEnv();
              default -> throw new IllegalStateException("unknown ITX_DB_PROVIDER: " + dbProvider);
            };
        ControlDispatcher dispatcher =
            new ControlDispatcher(
                repoFactory.createPostRepo(),
                repoFactory.createUserRepo(),
                repoFactory.createSubscriptionRepo(),
                HttpEmailClient.fromEnv(),
                new ObjectMapper());
        yield new Wired(
            List.of(factory.createControlStandardQueue(), factory.createControlPremiumQueue()),
            dispatcher);
      }
      case "compute" ->
          new Wired(
              List.of(factory.createComputeStandardQueue(), factory.createComputePremiumQueue()),
              new ComputeDispatcher());
      default ->
          throw new IllegalStateException(
              "unknown --mode: " + mode + " (expected control|compute)");
    };
  }
}
