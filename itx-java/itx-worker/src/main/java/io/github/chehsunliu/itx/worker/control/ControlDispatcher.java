package io.github.chehsunliu.itx.worker.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.email.SendEmailMessage;
import io.github.chehsunliu.itx.contract.queue.MessageBody;
import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.PostCreatedMessageBody;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo;
import io.github.chehsunliu.itx.contract.repo.User;
import io.github.chehsunliu.itx.contract.repo.UserRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class ControlDispatcher implements MessageHandler {
    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final EmailClient emailClient;
    private final ObjectMapper mapper;

    @Override
    public void handle(String body) throws Exception {
        MessageBody msg = mapper.readValue(body, MessageBody.class);
        if (msg instanceof PostCreatedMessageBody pc) {
            handlePostCreated(pc);
        }
    }

    private void handlePostCreated(PostCreatedMessageBody body) {
        UUID authorId = UUID.fromString(body.authorId());
        Post post = postRepo.get(new PostRepo.GetParams(body.postId()));
        User author = userRepo.get(authorId);
        List<User> subscribers = subscriptionRepo.listSubscribers(authorId);
        log.info(
                "sending post.created notifications post_id={} author={} subscribers={}",
                body.postId(),
                author.email(),
                subscribers.size());
        for (User subscriber : subscribers) {
            emailClient.send(new SendEmailMessage(
                    subscriber.email(),
                    author.email() + " just published a new post",
                    "Check out the new post: " + post.title()));
        }
        postRepo.markNotified(body.postId());
    }
}
