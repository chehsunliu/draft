package io.github.chehsunliu.itx.worker.control;

import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.email.SendEmailMessage;
import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.message.MessageBody;
import io.github.chehsunliu.itx.contract.queue.message.PostCreatedMessageBody;
import io.github.chehsunliu.itx.impl.repo.entity.PostEntity;
import io.github.chehsunliu.itx.impl.repo.entity.UserEntity;
import io.github.chehsunliu.itx.impl.repo.jpa.PostJpaRepo;
import io.github.chehsunliu.itx.impl.repo.jpa.SubscriptionJpaRepo;
import io.github.chehsunliu.itx.impl.repo.jpa.UserJpaRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Profile("control")
@RequiredArgsConstructor
public class ControlDispatcher implements MessageHandler {

  private final PostJpaRepo postRepo;
  private final UserJpaRepo userRepo;
  private final SubscriptionJpaRepo subscriptionRepo;
  private final EmailClient emailClient;
  private final ObjectMapper objectMapper;

  @Override
  public void handle(String body) throws Exception {
    MessageBody msg = objectMapper.readValue(body, MessageBody.class);
    if (msg instanceof PostCreatedMessageBody pc) {
      handlePostCreated(pc);
    } else {
      log.warn("unrecognized message body: {}", msg);
    }
  }

  @Transactional(readOnly = true)
  void handlePostCreated(PostCreatedMessageBody body) {
    PostEntity post =
        postRepo
            .findById(body.postId())
            .orElseThrow(() -> new IllegalStateException("missing post: " + body.postId()));
    UserEntity author =
        userRepo
            .findById(body.authorId())
            .orElseThrow(() -> new IllegalStateException("missing author: " + body.authorId()));
    List<UserEntity> subscribers = subscriptionRepo.listSubscribersByAuthorId(body.authorId());
    log.info(
        "sending post.created notifications post_id={} author={} subscribers={}",
        body.postId(),
        author.getEmail(),
        subscribers.size());

    for (UserEntity subscriber : subscribers) {
      emailClient.send(
          SendEmailMessage.builder()
              .to(subscriber.getEmail())
              .subject(author.getEmail() + " just published a new post")
              .body("Check out the new post: " + post.getTitle())
              .build());
    }
  }
}
