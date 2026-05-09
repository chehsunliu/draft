package io.github.chehsunliu.itx.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.feature.post.usecase.CreatePostUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.DeletePostUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.GetPostUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.ListPostsUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.UpdatePostUseCase;
import io.github.chehsunliu.itx.backend.feature.subscription.usecase.SubscribeUseCase;
import io.github.chehsunliu.itx.backend.feature.subscription.usecase.UnsubscribeUseCase;
import io.github.chehsunliu.itx.backend.feature.user.usecase.GetMeUseCase;
import io.github.chehsunliu.itx.backend.feature.user.usecase.ListSubscriptionsUseCase;
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
  public final ListPostsUseCase listPosts;
  public final GetPostUseCase getPost;
  public final CreatePostUseCase createPost;
  public final UpdatePostUseCase updatePost;
  public final DeletePostUseCase deletePost;
  public final GetMeUseCase getMe;
  public final ListSubscriptionsUseCase listSubscriptions;
  public final SubscribeUseCase subscribe;
  public final UnsubscribeUseCase unsubscribe;

  public AppState(
      PostRepo postRepo,
      UserRepo userRepo,
      SubscriptionRepo subscriptionRepo,
      MessageQueue controlStandardQueue,
      ObjectMapper mapper) {
    this.listPosts = new ListPostsUseCase(postRepo);
    this.getPost = new GetPostUseCase(postRepo);
    this.createPost = new CreatePostUseCase(postRepo, controlStandardQueue, mapper);
    this.updatePost = new UpdatePostUseCase(postRepo);
    this.deletePost = new DeletePostUseCase(postRepo);
    this.getMe = new GetMeUseCase(userRepo);
    this.listSubscriptions = new ListSubscriptionsUseCase(userRepo, subscriptionRepo);
    this.subscribe = new SubscribeUseCase(userRepo, subscriptionRepo);
    this.unsubscribe = new UnsubscribeUseCase(userRepo, subscriptionRepo);
  }

  public void close() {
    // no-op for now; HikariCP closes via DataSource lifecycle
  }

  public static AppState fromEnv(ObjectMapper mapper) {
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
        queueFactory.createControlStandardQueue(),
        mapper);
  }
}
