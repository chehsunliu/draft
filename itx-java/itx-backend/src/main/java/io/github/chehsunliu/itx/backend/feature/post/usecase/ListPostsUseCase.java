package io.github.chehsunliu.itx.backend.feature.post.usecase;

import io.github.chehsunliu.itx.backend.feature.post.PostDto;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ListPostsUseCase {
    public record ExecuteParams(UUID userId, int limit, int offset) {}

    public record ExecuteOutput(List<PostDto> items) {}

    private final PostRepo postRepo;

    public ExecuteOutput execute(ExecuteParams params) {
        List<Post> posts = postRepo.list(new PostRepo.ListParams(params.userId, params.limit, params.offset));
        return new ExecuteOutput(posts.stream().map(PostDto::fromPost).collect(Collectors.toList()));
    }
}
