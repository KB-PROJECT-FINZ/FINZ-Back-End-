package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.Auth.vo.UserVo;


@Mapper
public interface AuthMapper {
    int insertUser(UserVo user);
    int existsByUsername(String username);
    UserVo findByUsernameAndPassword(@Param("username") String username,
                                     @Param("password") String password);
}