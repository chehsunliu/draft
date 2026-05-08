package io.github.chehsunliu.itx.backend.state;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import io.github.chehsunliu.itx.impl.queue.QueueProperties;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitProperties;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsProperties;
import io.github.chehsunliu.itx.impl.repo.DbProperties;
import io.github.chehsunliu.itx.impl.repo.jpa.IdempotentInserter;
import io.github.chehsunliu.itx.impl.repo.jpa.JpaPostRepo;
import io.github.chehsunliu.itx.impl.repo.jpa.JpaSubscriptionRepo;
import io.github.chehsunliu.itx.impl.repo.jpa.JpaUserRepo;
import io.github.chehsunliu.itx.impl.repo.jpa.data.PostEntityRepository;
import io.github.chehsunliu.itx.impl.repo.jpa.data.SubscriptionEntityRepository;
import io.github.chehsunliu.itx.impl.repo.jpa.data.TagEntityRepository;
import io.github.chehsunliu.itx.impl.repo.jpa.data.UserEntityRepository;
import io.github.chehsunliu.itx.impl.repo.mariadb.MariadbProperties;
import io.github.chehsunliu.itx.impl.repo.postgres.PostgresProperties;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AppStateConfig {

  private final DbProperties dbProps;
  private final QueueProperties queueProps;
  private final PostgresProperties postgresProps;
  private final MariadbProperties mariadbProps;
  private final SqsProperties sqsProps;
  private final RabbitProperties rabbitProps;

  @Bean
  DataSource dataSource() {
    HikariConfig cfg = new HikariConfig();
    switch (dbProps.getProvider()) {
      case "postgres" -> {
        cfg.setJdbcUrl(
            "jdbc:postgresql://"
                + postgresProps.getHost()
                + ":"
                + postgresProps.getPort()
                + "/"
                + postgresProps.getDbName());
        cfg.setUsername(postgresProps.getUser());
        cfg.setPassword(postgresProps.getPassword());
        cfg.setDriverClassName("org.postgresql.Driver");
        cfg.setMaximumPoolSize(postgresProps.getMaximumPoolSize());
      }
      case "mariadb" -> {
        cfg.setJdbcUrl(
            "jdbc:mariadb://"
                + mariadbProps.getHost()
                + ":"
                + mariadbProps.getPort()
                + "/"
                + mariadbProps.getDbName());
        cfg.setUsername(mariadbProps.getUser());
        cfg.setPassword(mariadbProps.getPassword());
        cfg.setDriverClassName("org.mariadb.jdbc.Driver");
        cfg.setMaximumPoolSize(mariadbProps.getMaximumPoolSize());
      }
      default ->
          throw new IllegalStateException("unknown itx.db.provider: " + dbProps.getProvider());
    }
    return new HikariDataSource(cfg);
  }

  // MariaDB stores TIMESTAMP without offset, so normalize OffsetDateTime through UTC; UUID
  // columns are CHAR(36) on this schema so steer Hibernate to that JDBC type.
  @Bean
  HibernatePropertiesCustomizer hibernateCustomizer() {
    return props -> {
      if ("mariadb".equals(dbProps.getProvider())) {
        props.put("hibernate.timezone.default_storage", "NORMALIZE_UTC");
        props.put("hibernate.type.preferred_uuid_jdbc_type", "CHAR");
      }
    };
  }

  @Bean
  MessageQueueFactory messageQueueFactory() {
    return switch (queueProps.getProvider()) {
      case "sqs" -> new SqsMessageQueueFactory(sqsProps);
      case "rabbitmq" -> new RabbitMessageQueueFactory(rabbitProps);
      default ->
          throw new IllegalStateException(
              "unknown itx.queue.provider: " + queueProps.getProvider());
    };
  }

  @Bean(name = "controlStandardQueue")
  MessageQueue controlStandardQueue(MessageQueueFactory factory) {
    return factory.createControlStandardQueue();
  }

  @Bean
  IdempotentInserter idempotentInserter(
      UserEntityRepository userEntityRepo,
      TagEntityRepository tagEntityRepo,
      SubscriptionEntityRepository subscriptionEntityRepo) {
    return new IdempotentInserter(userEntityRepo, tagEntityRepo, subscriptionEntityRepo);
  }

  @Bean
  PostRepo postRepo(PostEntityRepository postEntityRepo, IdempotentInserter inserter) {
    return new JpaPostRepo(postEntityRepo, inserter);
  }

  @Bean
  UserRepo userRepo(UserEntityRepository userEntityRepo, IdempotentInserter inserter) {
    return new JpaUserRepo(userEntityRepo, inserter);
  }

  @Bean
  SubscriptionRepo subscriptionRepo(
      SubscriptionEntityRepository subscriptionEntityRepo, IdempotentInserter inserter) {
    return new JpaSubscriptionRepo(subscriptionEntityRepo, inserter);
  }
}
