package io.github.chehsunliu.itx.backend.feature.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.AppState;
import io.github.chehsunliu.itx.backend.feature.post.usecase.CreatePostUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.DeletePostUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.GetPostUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.ListPostsUseCase;
import io.github.chehsunliu.itx.backend.feature.post.usecase.UpdatePostUseCase;
import io.github.chehsunliu.itx.backend.middleware.Envelope;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.javalin.Javalin;

public final class PostRoutes {
  private PostRoutes() {}

  public static void register(Javalin app, ObjectMapper mapper, AppState state) {
    app.get(
        "/api/v1/posts",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          int limit = parseInt(ctx.queryParam("limit"), 50);
          int offset = parseInt(ctx.queryParam("offset"), 0);
          ListPostsUseCase.ExecuteOutput out =
              state.listPosts.execute(new ListPostsUseCase.ExecuteParams(c.userId, limit, offset));
          Envelope.data(ctx, out);
        });

    app.post(
        "/api/v1/posts",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          CreatePostRequest body = mapper.readValue(ctx.body(), CreatePostRequest.class);
          PostDto dto =
              state.createPost.execute(
                  new CreatePostUseCase.ExecuteParams(
                      c.userId, body.title(), body.body(), body.tags()));
          Envelope.data(ctx, dto, 201);
        });

    app.get(
        "/api/v1/posts/{id}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          long id = Long.parseLong(ctx.pathParam("id"));
          PostDto dto = state.getPost.execute(new GetPostUseCase.ExecuteParams(id, c.userId));
          Envelope.data(ctx, dto);
        });

    app.patch(
        "/api/v1/posts/{id}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          long id = Long.parseLong(ctx.pathParam("id"));
          UpdatePostRequest body = mapper.readValue(ctx.body(), UpdatePostRequest.class);
          PostDto dto =
              state.updatePost.execute(
                  new UpdatePostUseCase.ExecuteParams(
                      id, c.userId, body.title(), body.body(), body.tags()));
          Envelope.data(ctx, dto);
        });

    app.delete(
        "/api/v1/posts/{id}",
        ctx -> {
          ItxContext c = ItxContext.from(ctx);
          long id = Long.parseLong(ctx.pathParam("id"));
          state.deletePost.execute(new DeletePostUseCase.ExecuteParams(id, c.userId));
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
