package io.github.chehsunliu.itx.backend.feature.post.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.backend.feature.post.PostDto;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.PostCreatedMessageBody;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CreatePostUseCase {
    public record ExecuteParams(UUID userId, String title, String body, List<String> tags) {}

    private final PostRepo postRepo;
    private final MessageQueue controlStandardQueue;
    private final ObjectMapper mapper;

    public PostDto execute(ExecuteParams params) {
        Post post = postRepo.create(new PostRepo.CreateParams(params.userId, params.title, params.body, params.tags));
        String body;
        try {
            body = mapper.writeValueAsString(PostCreatedMessageBody.of(post.id(), post.authorId()));
        } catch (Exception e) {
            throw BackendException.unknown(e.getMessage());
        }
        controlStandardQueue.publish(body);
        return PostDto.fromPost(post);
    }
}
