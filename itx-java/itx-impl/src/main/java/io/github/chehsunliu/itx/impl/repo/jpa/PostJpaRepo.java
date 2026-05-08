package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.impl.repo.entity.PostEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostJpaRepo extends JpaRepository<PostEntity, Long> {

  @EntityGraph(attributePaths = "tags")
  Optional<PostEntity> findById(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = "tags")
  @Query("SELECT p FROM PostEntity p WHERE p.id = :id AND p.authorId = :authorId")
  Optional<PostEntity> findForUpdate(@Param("id") long id, @Param("authorId") UUID authorId);

  @Modifying
  @Query("DELETE FROM PostEntity p WHERE p.id = :id AND p.authorId = :authorId")
  int deleteByIdAndAuthorId(@Param("id") long id, @Param("authorId") UUID authorId);
}
