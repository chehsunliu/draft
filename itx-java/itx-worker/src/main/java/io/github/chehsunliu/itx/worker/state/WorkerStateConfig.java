package io.github.chehsunliu.itx.worker.state;

import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoFactory;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import io.github.chehsunliu.itx.impl.email.HttpEmailClient;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import io.github.chehsunliu.itx.impl.repo.mariadb.MariaDbRepoFactory;
import io.github.chehsunliu.itx.impl.repo.postgres.PostgresRepoFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class WorkerStateConfig {

  @Bean
  MessageQueueFactory messageQueueFactory() {
    String provider = System.getenv().getOrDefault("ITX_QUEUE_PROVIDER", "sqs");
    return switch (provider) {
      case "sqs" -> new SqsMessageQueueFactory();
      case "rabbitmq" -> new RabbitMessageQueueFactory();
      default -> throw new IllegalStateException("unknown ITX_QUEUE_PROVIDER: " + provider);
    };
  }

  /**
   * Repo + email client are only needed by the control worker; the compute worker is queue-only, so
   * we gate this configuration to avoid forcing DB env vars on compute pods.
   */
  @Configuration
  @Profile("control")
  public static class ControlBeans {

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

    @Bean
    EmailClient emailClient() {
      return HttpEmailClient.fromEnv();
    }
  }
}
