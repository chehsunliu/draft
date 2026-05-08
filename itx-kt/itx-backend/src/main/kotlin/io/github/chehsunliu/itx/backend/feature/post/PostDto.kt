package io.github.chehsunliu.itx.backend.feature.post

import io.github.chehsunliu.itx.backend.feature.OffsetDateTimeSerializer
import io.github.chehsunliu.itx.backend.feature.UuidSerializer
import io.github.chehsunliu.itx.contract.repo.Post
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.util.UUID

@Serializable
data class PostDto(
    val id: Long,
    @Serializable(with = UuidSerializer::class) val authorId: UUID,
    val title: String,
    val body: String,
    val tags: List<String>,
    @Serializable(with = OffsetDateTimeSerializer::class) val createdAt: OffsetDateTime,
)

fun Post.toDto(): PostDto = PostDto(id, authorId, title, body, tags, createdAt)

@Serializable
data class ListPostsResponse(
    val items: List<PostDto>,
)

@Serializable
data class CreatePostRequest(
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class UpdatePostRequest(
    val title: String? = null,
    val body: String? = null,
    val tags: List<String>? = null,
)
