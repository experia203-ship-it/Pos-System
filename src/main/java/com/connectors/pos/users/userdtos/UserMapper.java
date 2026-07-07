package com.connectors.pos.users.userdtos;

import com.connectors.pos.users.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target="password",ignore =true)
  Users toEntity(CreateUserDto dto);


 UserResponseDto toResponse(Users user);
}
