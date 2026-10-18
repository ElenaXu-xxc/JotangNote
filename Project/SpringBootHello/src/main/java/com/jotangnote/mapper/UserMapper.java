
package com.jotangnote.mapper;

import com.jotangnote.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users (username, password) VALUES (#{username}, #{password})")
    int insert(User user);

    @Select("SELECT id, username, password, created_at AS createdAt " +
            "FROM users WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT id, username FROM users WHERE id = #{id}")
    User findById(@Param("id") Long id);
}
