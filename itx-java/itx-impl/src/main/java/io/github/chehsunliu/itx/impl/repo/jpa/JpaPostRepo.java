package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.contract.repo.Post;
import io.github.chehsunliu.itx.contract.repo.PostRepo;
import io.github.chehsunliu.itx.contract.repo.RepoNotFoundException;
import io.github.chehsunliu.itx.impl.repo.entity.PostEntity;
import io.github.chehsunliu.itx.impl.repo.entity.TagEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnExpression("!'${itx.db.provider:}'.isEmpty()")
@RequiredArgsConstructor
class JpaPostRepo implements PostRepo {

  private final PostJpaRepo postJpaRepo;
  private final IdempotentInserter inserter;

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional(readOnly = true)
  public List<Post> list(ListParams params) {
    int pageSize = params.getLimit() == 0 ? 50 : params.getLimit();
    String jpql =
        params.getAuthorId() == null
            ? "SELECT p FROM PostEntity p ORDER BY p.id DESC"
            : "SELECT p FROM PostEntity p WHERE p.authorId = :authorId ORDER BY p.id DESC";
    var query = em.createQuery(jpql, PostEntity.class);
    if (params.getAuthorId() != null) {
      query.setParameter("authorId", params.getAuthorId());
    }
    return query.setFirstResult(params.getOffset()).setMaxResults(pageSize).getResultList().stream()
        .map(JpaPostRepo::toContract)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Post get(GetParams params) {
    return postJpaRepo
        .findById(params.getId())
        .map(JpaPostRepo::toContract)
        .orElseThrow(RepoNotFoundException::new);
  }

  @Override
  @Transactional
  public Post create(CreateParams params) {
    Set<TagEntity> tags = resolveTags(params.getTags());
    PostEntity entity = new PostEntity();
    entity.setAuthorId(params.getAuthorId());
    entity.setTitle(params.getTitle());
    entity.setBody(params.getBody());
    entity.setTags(tags);
    return toContract(postJpaRepo.save(entity));
  }

  @Override
  @Transactional
  public Post update(UpdateParams params) {
    PostEntity entity =
        postJpaRepo
            .findForUpdate(params.getId(), params.getAuthorId())
            .orElseThrow(RepoNotFoundException::new);
    if (params.getTitle() != null) entity.setTitle(params.getTitle());
    if (params.getBody() != null) entity.setBody(params.getBody());
    if (params.getTags() != null) {
      entity.setTags(resolveTags(params.getTags()));
    }
    return toContract(entity);
  }

  @Override
  @Transactional
  public void delete(DeleteParams params) {
    int rows = postJpaRepo.deleteByIdAndAuthorId(params.getId(), params.getAuthorId());
    if (rows == 0) {
      throw new RepoNotFoundException();
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
