package io.github.chehsunliu.itx.worker.control

import io.github.chehsunliu.itx.contract.email.EmailClient
import io.github.chehsunliu.itx.contract.email.SendEmailMessage
import io.github.chehsunliu.itx.contract.queue.MessageBody
import io.github.chehsunliu.itx.contract.queue.MessageHandler
import io.github.chehsunliu.itx.contract.queue.PostCreatedMessageBody
import io.github.chehsunliu.itx.contract.repo.PostRepo
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.UserRepo
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class ControlDispatcher(
    private val postRepo: PostRepo,
    private val userRepo: UserRepo,
    private val subscriptionRepo: SubscriptionRepo,
    private val emailClient: EmailClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : MessageHandler {
    private val log = LoggerFactory.getLogger(ControlDispatcher::class.java)

    override suspend fun handle(body: String) {
        when (val msg = json.decodeFromString(MessageBody.serializer(), body)) {
            is PostCreatedMessageBody -> handlePostCreated(msg)
        }
    }

    private suspend fun handlePostCreated(body: PostCreatedMessageBody) {
        val authorId = UUID.fromString(body.authorId)
        val post = postRepo.get(PostRepo.GetParams(body.postId))
        val author = userRepo.get(authorId)
        val subscribers = subscriptionRepo.listSubscribers(authorId)
        log.info(
            "sending post.created notifications post_id={} author={} subscribers={}",
            body.postId,
            author.email,
            subscribers.size,
        )
        for (subscriber in subscribers) {
            emailClient.send(
                SendEmailMessage(
                    to = subscriber.email,
                    subject = "${author.email} just published a new post",
                    body = "Check out the new post: ${post.title}",
                ),
            )
        }
    }
}
