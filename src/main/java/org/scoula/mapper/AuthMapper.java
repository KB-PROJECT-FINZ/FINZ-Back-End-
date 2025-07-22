package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.userdetails.User;

@Mapper
public interface AuthMapper {
    int insertUser(User user);
    User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
    int existsByUsername(String username);
}