package io.github.chehsunliu.itx.contract.queue

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MessageBody {
    val type: String
}

@Serializable
@SerialName("post.created")
data class PostCreatedMessageBody(
    @kotlinx.serialization.Required override val type: String = "post.created",
    val postId: Long,
    val authorId: String,
) : MessageBody {
    companion object {
        const val TYPE: String = "post.created"

        fun of(
            postId: Long,
            authorId: java.util.UUID,
        ): PostCreatedMessageBody = PostCreatedMessageBody(postId = postId, authorId = authorId.toString())
    }
}
