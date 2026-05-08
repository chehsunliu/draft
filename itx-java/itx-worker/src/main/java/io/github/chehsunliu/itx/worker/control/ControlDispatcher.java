package io.github.chehsunliu.itx.worker.control;

import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.email.SendEmailMessage;
import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.message.MessageBody;
import io.github.chehsunliu.itx.contract.queue.message.PostCreatedMessageBody;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@Profile("control")
@RequiredArgsConstructor
public class ControlDispatcher implements MessageHandler {

  private final PostRepo postRepo;
  private final UserRepo userRepo;
  private final SubscriptionRepo subscriptionRepo;
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

  private void handlePostCreated(PostCreatedMessageBody body) {
    Post post = postRepo.get(PostRepo.GetParams.builder().id(body.postId()).build());
    User author = userRepo.get(body.authorId());
    List<User> subscribers = subscriptionRepo.listSubscribers(body.authorId());
    log.info(
        "sending post.created notifications post_id={} author={} subscribers={}",
        body.postId(),
        author.getEmail(),
        subscribers.size());

    for (User subscriber : subscribers) {
      emailClient.send(
          SendEmailMessage.builder()
              .to(subscriber.getEmail())
              .subject(author.getEmail() + " just published a new post")
              .body("Check out the new post: " + post.getTitle())
              .build());
    }
  }
}
