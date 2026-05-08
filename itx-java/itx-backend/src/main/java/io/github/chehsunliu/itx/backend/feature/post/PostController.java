package io.github.chehsunliu.itx.backend.feature.post;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.middleware.ItxContext;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.message.PostCreatedMessageBody;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostRepo postRepo;
  private final MessageQueue controlStandardQueue;
  private final PostMapper mapper;
  private final ObjectMapper objectMapper;

  @Value
  @Builder
  static class ListResponse {
    List<PostDto> items;
  }

  @Value
  static class CreatePostRequest {
    String title;
    String body;
    List<String> tags;
  }

  @Value
  static class UpdatePostRequest {
    String title;
    String body;
    List<String> tags;
  }

  @GetMapping
  ListResponse list(
      HttpServletRequest req,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    UUID userId = ctx(req).getUserId();
    List<Post> posts =
        postRepo.list(
            PostRepo.ListParams.builder().authorId(userId).limit(limit).offset(offset).build());
    return ListResponse.builder().items(posts.stream().map(mapper::toDto).toList()).build();
  }

  @GetMapping("/{id}")
  PostDto get(HttpServletRequest req, @PathVariable long id) {
    UUID userId = ctx(req).getUserId();
    Post post = postRepo.get(PostRepo.GetParams.builder().id(id).build());
    if (!post.getAuthorId().equals(userId)) {
      throw BackendException.notFound();
    }
    return mapper.toDto(post);
  }

  @PostMapping
  ResponseEntity<PostDto> create(HttpServletRequest req, @RequestBody CreatePostRequest body) {
    UUID userId = ctx(req).getUserId();
    Post post =
        postRepo.create(
            PostRepo.CreateParams.builder()
                .authorId(userId)
                .title(body.getTitle())
                .body(body.getBody())
                .tags(body.getTags() == null ? List.of() : body.getTags())
                .build());

    try {
      String payload =
          objectMapper.writeValueAsString(
              PostCreatedMessageBody.of(post.getId(), post.getAuthorId()));
      controlStandardQueue.publish(payload);
    } catch (Exception e) {
      throw BackendException.unknown(e.getMessage());
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(post));
  }

  @PatchMapping("/{id}")
  PostDto update(
      HttpServletRequest req, @PathVariable long id, @RequestBody UpdatePostRequest body) {
    UUID userId = ctx(req).getUserId();
    Post post =
        postRepo.update(
            PostRepo.UpdateParams.builder()
                .id(id)
                .authorId(userId)
                .title(body.getTitle())
                .body(body.getBody())
                .tags(body.getTags())
                .build());
    return mapper.toDto(post);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Map<String, Object>> delete(HttpServletRequest req, @PathVariable long id) {
    UUID userId = ctx(req).getUserId();
    postRepo.delete(PostRepo.DeleteParams.builder().id(id).authorId(userId).build());
    return ResponseEntity.noContent().build();
  }

  private static ItxContext ctx(HttpServletRequest req) {
    return (ItxContext) req.getAttribute(ItxContext.ATTR);
  }
}
