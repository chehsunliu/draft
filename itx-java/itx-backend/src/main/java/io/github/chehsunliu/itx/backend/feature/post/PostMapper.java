package io.github.chehsunliu.itx.backend.feature.post;

import io.github.chehsunliu.itx.contract.repo.Post;
import org.mapstruct.Mapper;

@Mapper
public interface PostMapper {
  PostDto toDto(Post post);
}
