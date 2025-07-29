package org.scoula.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.domain.Auth.vo.UserVo;


@Mapper
public interface AuthMapper {
    int insertUser(UserVo user);
    int existsByUsername(String username);
    int existsByNickname(String nickname);
    UserVo findByUsernameAndPassword(@Param("username") String username,
                                     @Param("password") String password);
    int updatePassword(@Param("userId") Long userId, @Param("newPassword") String newPassword);
    UserVo findByEmail(@Param("email") String email);
    UserVo findByNameAndEmail(@Param("name") String name, @Param("email") String email);
    int countByNickname(@Param("nickname") String nickname);
}

