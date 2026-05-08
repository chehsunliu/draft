package io.github.chehsunliu.itx.backend.feature.user;

import io.github.chehsunliu.itx.contract.repo.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
  UserDto toDto(User user);
}
