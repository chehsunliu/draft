package io.github.chehsunliu.itx.impl.repo.jpa;

import io.github.chehsunliu.itx.impl.repo.entity.TagEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepo extends JpaRepository<TagEntity, Long> {
  Optional<TagEntity> findByName(String name);
}
