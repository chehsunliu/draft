package io.github.chehsunliu.itx.backend.state;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppStateConfig {

  @Bean
  RepoFactory repoFactory() {
    String provider = System.getenv().getOrDefault("ITX_DB_PROVIDER", "postgres");
    return switch (provider) {
      case "postgres" -> new PostgresRepoFactory(PostgresRepoFactory.dataSourceFromEnv());
      case "mariadb" -> new MariaDbRepoFactory(MariaDbRepoFactory.dataSourceFromEnv());
      default -> throw new IllegalStateException("unknown ITX_DB_PROVIDER: " + provider);
    };
  }

  @Bean
  MessageQueueFactory messageQueueFactory() {
    String provider = System.getenv().getOrDefault("ITX_QUEUE_PROVIDER", "sqs");
    return switch (provider) {
      case "sqs" -> new SqsMessageQueueFactory();
      case "rabbitmq" -> new RabbitMessageQueueFactory();
      default -> throw new IllegalStateException("unknown ITX_QUEUE_PROVIDER: " + provider);
    };
  }

  @Bean
  PostRepo postRepo(RepoFactory factory) {
    return factory.createPostRepo();
  }

  @Bean
  UserRepo userRepo(RepoFactory factory) {
    return factory.createUserRepo();
  }

  @Bean
  SubscriptionRepo subscriptionRepo(RepoFactory factory) {
    return factory.createSubscriptionRepo();
  }

  @Bean(name = "controlStandardQueue")
  MessageQueue controlStandardQueue(MessageQueueFactory factory) {
    return factory.createControlStandardQueue();
  }
}
