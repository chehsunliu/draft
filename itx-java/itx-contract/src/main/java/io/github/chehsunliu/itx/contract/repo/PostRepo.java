package io.github.chehsunliu.itx.contract.repo;

import java.util.List;
import java.util.UUID;

public interface PostRepo {
    List<Post> list(ListParams params);

    /** Throws {@link RepoNotFoundException} if no post with {@code params.id()} exists. */
    Post get(GetParams params);

    Post create(CreateParams params);

    /**
     * Updates a post owned by {@code params.authorId()}. Throws {@link RepoNotFoundException} if the
     * post does not exist or is not owned by the caller.
     */
    Post update(UpdateParams params);

    /**
     * Deletes a post owned by {@code params.authorId()}. Throws {@link RepoNotFoundException} if the
     * post does not exist or is not owned by the caller.
     */
    void delete(DeleteParams params);

    /** Stamps {@code notified_at = now()} for the given post. Idempotent — safe to retry. */
    void markNotified(long id);

    record ListParams(UUID authorId, int limit, int offset) {
        public ListParams {
            if (limit < 0) throw new IllegalArgumentException("limit must be non-negative");
            if (offset < 0) throw new IllegalArgumentException("offset must be non-negative");
        }
    }

    record GetParams(long id) {}

    record CreateParams(UUID authorId, String title, String body, List<String> tags) {}

    record UpdateParams(long id, UUID authorId, String title, String body, List<String> tags) {}

    record DeleteParams(long id, UUID authorId) {}
}
