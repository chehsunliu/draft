package io.github.chehsunliu.itx.backend;

import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import io.github.chehsunliu.itx.impl.repo.mariadb.MariaDbRepoFactory;
import io.github.chehsunliu.itx.impl.repo.postgres.PostgresRepoFactory;

public final class AppState {
  public final PostRepo postRepo;
  public final UserRepo userRepo;
  public final SubscriptionRepo subscriptionRepo;
  public final MessageQueue controlStandardQueue;

  public AppState(
      PostRepo postRepo,
      UserRepo userRepo,
      SubscriptionRepo subscriptionRepo,
      MessageQueue controlStandardQueue) {
    this.postRepo = postRepo;
    this.userRepo = userRepo;
    this.subscriptionRepo = subscriptionRepo;
    this.controlStandardQueue = controlStandardQueue;
  }

  public void close() {
    // no-op for now; HikariCP closes via DataSource lifecycle
  }

  public static AppState fromEnv() {
    String dbProvider = System.getenv().getOrDefault("ITX_DB_PROVIDER", "postgres");
    RepoFactory repoFactory =
        switch (dbProvider) {
          case "postgres" -> PostgresRepoFactory.fromEnv();
          case "mariadb" -> MariaDbRepoFactory.fromEnv();
          default -> throw new IllegalStateException("unknown ITX_DB_PROVIDER: " + dbProvider);
        };
    String queueProvider = System.getenv().getOrDefault("ITX_QUEUE_PROVIDER", "sqs");
    MessageQueueFactory queueFactory =
        switch (queueProvider) {
          case "sqs" -> SqsMessageQueueFactory.fromEnv();
          case "rabbitmq" -> RabbitMessageQueueFactory.fromEnv();
          default ->
              throw new IllegalStateException("unknown ITX_QUEUE_PROVIDER: " + queueProvider);
        };
    return new AppState(
        repoFactory.createPostRepo(),
        repoFactory.createUserRepo(),
        repoFactory.createSubscriptionRepo(),
        queueFactory.createControlStandardQueue());
  }
}
