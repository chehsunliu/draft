package io.github.chehsunliu.itx.backend.feature.post

import io.github.chehsunliu.itx.backend.error.BackendException
import io.github.chehsunliu.itx.backend.middleware.itxContext
import io.github.chehsunliu.itx.backend.middleware.respondData
import io.github.chehsunliu.itx.contract.queue.MessageQueue
import io.github.chehsunliu.itx.contract.queue.PostCreatedMessageBody
import io.github.chehsunliu.itx.contract.repo.PostRepo
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import kotlinx.serialization.json.Json

fun Route.postRoutes(
    postRepo: PostRepo,
    controlStandardQueue: MessageQueue,
) {
    route("/posts") {
        get {
            val ctx = call.itxContext()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val posts =
                postRepo.list(
                    PostRepo.ListParams(authorId = ctx.userId, limit = limit, offset = offset),
                )
            call.respondData(
                ListPostsResponse.serializer(),
                ListPostsResponse(items = posts.map { it.toDto() }),
            )
        }

        post {
            val ctx = call.itxContext()
            val body = call.receive<CreatePostRequest>()
            val created =
                postRepo.create(
                    PostRepo.CreateParams(
                        authorId = ctx.userId!!,
                        title = body.title,
                        body = body.body,
                        tags = body.tags,
                    ),
                )
            val payload =
                Json.encodeToString(
                    PostCreatedMessageBody.serializer(),
                    PostCreatedMessageBody.of(created.id, created.authorId),
                )
            controlStandardQueue.publish(payload)
            call.respondData(PostDto.serializer(), created.toDto(), HttpStatusCode.Created)
        }

        get("/{id}") {
            val ctx = call.itxContext()
            val id = call.parameters.getOrFail("id").toLong()
            val post = postRepo.get(PostRepo.GetParams(id))
            if (post.authorId != ctx.userId) throw BackendException.notFound()
            call.respondData(PostDto.serializer(), post.toDto())
        }

        patch("/{id}") {
            val ctx = call.itxContext()
            val id = call.parameters.getOrFail("id").toLong()
            val body = call.receive<UpdatePostRequest>()
            val updated =
                postRepo.update(
                    PostRepo.UpdateParams(
                        id = id,
                        authorId = ctx.userId!!,
                        title = body.title,
                        body = body.body,
                        tags = body.tags,
                    ),
                )
            call.respondData(PostDto.serializer(), updated.toDto())
        }

        delete("/{id}") {
            val ctx = call.itxContext()
            val id = call.parameters.getOrFail("id").toLong()
            postRepo.delete(PostRepo.DeleteParams(id, ctx.userId!!))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
