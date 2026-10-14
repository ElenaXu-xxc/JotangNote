package com.jotangnote.mapper;

import com.jotangnote.entity.Note;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param; // @Param 用于给 SQL 参数指定明确的名称。尤其是一个方法有多个参数时，写清楚名称可以避免 MyBatis 无法正确对应参数。

@Mapper
public interface NoteMapper {

    // 新增笔记
    @Insert("INSERT INTO notes (title, content, user_id) " +
            "VALUES (#{title}, #{content}, #{userId})") // Insert:向笔记写入。
    int insert(Note note);

    // 根据 ID 查询笔记
    @Select("SELECT id, title, content, created_at AS createdAt, " +
            "updated_at AS updatedAt, is_deleted AS isDeleted " +
            "FROM notes WHERE id = #{id} AND is_deleted = 0")
    Note findById(Long id);

    // 删除笔记（逻辑删除）
    @Update("UPDATE notes SET is_deleted = 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0") // 检查发布者 ID 是否与当前登录用户一致。
    int deleteById(@Param("id") Long id, @Param("userId") Long userId); // @Param("id") 和 @Param("userId") 分别给两个参数命名，供 SQL 中的 #{id} 和 #{userId} 使用。

    // 修改笔记标题和正文
    @Update("UPDATE notes SET title = #{title}, content = #{content} " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int update(Note note);
        }