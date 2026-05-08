package io.github.chehsunliu.itx.contract.repo

import java.time.OffsetDateTime
import java.util.UUID

data class Post(
    val id: Long,
    val authorId: UUID,
    val title: String,
    val body: String,
    val tags: List<String>,
    val createdAt: OffsetDateTime,
)

interface PostRepo {
    suspend fun list(params: ListParams): List<Post>

    /** Throws [RepoNotFoundException] if no post with `params.id` exists. */
    suspend fun get(params: GetParams): Post

    suspend fun create(params: CreateParams): Post

    /**
     * Updates a post owned by `params.authorId`. Throws [RepoNotFoundException] if the post does
     * not exist or is not owned by the caller.
     */
    suspend fun update(params: UpdateParams): Post

    /**
     * Deletes a post owned by `params.authorId`. Throws [RepoNotFoundException] if the post does
     * not exist or is not owned by the caller.
     */
    suspend fun delete(params: DeleteParams)

    data class ListParams(
        val authorId: UUID? = null,
        val limit: Int = 0,
        val offset: Int = 0,
    )

    data class GetParams(
        val id: Long,
    )

    data class CreateParams(
        val authorId: UUID,
        val title: String,
        val body: String,
        val tags: List<String>,
    )

    data class UpdateParams(
        val id: Long,
        val authorId: UUID,
        val title: String? = null,
        val body: String? = null,
        val tags: List<String>? = null,
    )

    data class DeleteParams(
        val id: Long,
        val authorId: UUID,
    )
}
