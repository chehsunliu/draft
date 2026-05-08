package io.github.chehsunliu.itx.backend.feature.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.Envelope;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.PostCreatedMessageBody;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.javalin.Javalin;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PostRoutes {
  private PostRoutes() {}

  public static void register(
      Javalin app, ObjectMapper mapper, PostRepo postRepo, MessageQueue controlStandardQueue) {
    app.get(
        "/api/v1/posts",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          int limit = parseInt(ctx.queryParam("limit"), 50);
          int offset = parseInt(ctx.queryParam("offset"), 0);
          List<Post> posts = postRepo.list(new PostRepo.ListParams(c.userId, limit, offset));
          List<PostDto> items = posts.stream().map(PostDto::fromPost).collect(Collectors.toList());
          Envelope.respondData(ctx, mapper, Map.of("items", items));
        });

    app.post(
        "/api/v1/posts",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          CreatePostRequest body = mapper.readValue(ctx.body(), CreatePostRequest.class);
          Post created =
              postRepo.create(
                  new PostRepo.CreateParams(c.userId, body.title(), body.body(), body.tags()));
          String payload =
              mapper.writeValueAsString(
                  PostCreatedMessageBody.of(created.id(), created.authorId()));
          controlStandardQueue.publish(payload);
          Envelope.respondData(ctx, mapper, PostDto.fromPost(created), 201);
        });

    app.get(
        "/api/v1/posts/{id}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          long id = Long.parseLong(ctx.pathParam("id"));
          Post post = postRepo.get(new PostRepo.GetParams(id));
          if (!post.authorId().equals(c.userId)) throw BackendException.notFound();
          Envelope.respondData(ctx, mapper, PostDto.fromPost(post));
        });

    app.patch(
        "/api/v1/posts/{id}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          long id = Long.parseLong(ctx.pathParam("id"));
          UpdatePostRequest body = mapper.readValue(ctx.body(), UpdatePostRequest.class);
          Post updated =
              postRepo.update(
                  new PostRepo.UpdateParams(id, c.userId, body.title(), body.body(), body.tags()));
          Envelope.respondData(ctx, mapper, PostDto.fromPost(updated));
        });

    app.delete(
        "/api/v1/posts/{id}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          long id = Long.parseLong(ctx.pathParam("id"));
          postRepo.delete(new PostRepo.DeleteParams(id, c.userId));
          ctx.status(204);
        });
  }

  private static int parseInt(String raw, int fallback) {
    if (raw == null) return fallback;
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
