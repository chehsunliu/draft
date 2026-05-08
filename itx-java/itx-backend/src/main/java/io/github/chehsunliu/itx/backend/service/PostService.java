package io.github.chehsunliu.itx.backend.service;

import io.github.chehsunliu.itx.backend.error.BackendException;
import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.impl.repo.entity.PostEntity;
import io.github.chehsunliu.itx.impl.repo.entity.TagEntity;
import io.github.chehsunliu.itx.impl.repo.jpa.PostJpaRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostJpaRepo postRepo;
  private final IdempotentInserter inserter;

  @PersistenceContext private EntityManager em;

  @Transactional(readOnly = true)
  public List<Post> list(UUID authorId, int limit, int offset) {
    int pageSize = limit == 0 ? 50 : limit;
    String jpql =
        authorId == null
            ? "SELECT p FROM PostEntity p ORDER BY p.id DESC"
            : "SELECT p FROM PostEntity p WHERE p.authorId = :authorId ORDER BY p.id DESC";
    var query = em.createQuery(jpql, PostEntity.class);
    if (authorId != null) {
      query.setParameter("authorId", authorId);
    }
    List<PostEntity> entities =
        query.setFirstResult(offset).setMaxResults(pageSize).getResultList();
    return entities.stream().map(PostService::toContract).toList();
  }

  @Transactional(readOnly = true)
  public Post get(long id) {
    return postRepo
        .findById(id)
        .map(PostService::toContract)
        .orElseThrow(BackendException::notFound);
  }

  @Transactional
  public Post create(UUID authorId, String title, String body, List<String> tagNames) {
    Set<TagEntity> tags = resolveTags(tagNames);
    PostEntity entity = new PostEntity();
    entity.setAuthorId(authorId);
    entity.setTitle(title);
    entity.setBody(body);
    entity.setTags(tags);
    PostEntity saved = postRepo.save(entity);
    return toContract(saved);
  }

  @Transactional
  public Post update(long id, UUID authorId, String title, String body, List<String> tagNames) {
    PostEntity entity =
        postRepo.findForUpdate(id, authorId).orElseThrow(BackendException::notFound);
    if (title != null) entity.setTitle(title);
    if (body != null) entity.setBody(body);
    if (tagNames != null) {
      entity.setTags(resolveTags(tagNames));
    }
    return toContract(entity);
  }

  @Transactional
  public void delete(long id, UUID authorId) {
    int rows = postRepo.deleteByIdAndAuthorId(id, authorId);
    if (rows == 0) {
      throw BackendException.notFound();
    }
  }

  private Set<TagEntity> resolveTags(List<String> names) {
    Set<TagEntity> tags = new LinkedHashSet<>();
    for (String name : names) {
      tags.add(inserter.insertTagIfAbsent(name));
    }
    return tags;
  }

  private static Post toContract(PostEntity e) {
    return Post.builder()
        .id(e.getId())
        .authorId(e.getAuthorId())
        .title(e.getTitle())
        .body(e.getBody())
        .tags(e.tagNames())
        .createdAt(e.getCreatedAt())
        .build();
  }
}
