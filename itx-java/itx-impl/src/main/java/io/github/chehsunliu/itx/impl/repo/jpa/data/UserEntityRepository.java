package io.github.chehsunliu.itx.impl.repo.jpa.data;

import io.github.chehsunliu.itx.impl.repo.entity.UserEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEntityRepository extends JpaRepository<UserEntity, UUID> {}
