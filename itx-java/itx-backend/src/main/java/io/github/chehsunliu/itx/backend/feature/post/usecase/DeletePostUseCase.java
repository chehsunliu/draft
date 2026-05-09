package io.github.chehsunliu.itx.backend.feature.post.usecase;

import io.github.chehsunliu.itx.contract.repo.PostRepo;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DeletePostUseCase {
    public record ExecuteParams(long id, UUID userId) {}

    private final PostRepo postRepo;

    public void execute(ExecuteParams params) {
        postRepo.delete(new PostRepo.DeleteParams(params.id, params.userId));
    }
}
