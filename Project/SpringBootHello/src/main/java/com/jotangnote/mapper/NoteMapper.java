package com.jotangnote.mapper;

import com.jotangnote.entity.Note;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param; // @Param 用于给 SQL 参数指定明确的名称。尤其是一个方法有多个参数时，写清楚名称可以避免 MyBatis 无法正确对应参数。

import java.util.List;

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
    Note findById(Long id); // 定义了一个叫 findById 的方法：Note：返回类型，表示方法执行后会返回一个 Note 对象。findById：方法名，按命名习惯表示“根据编号查找”。Long id：参数，调用方法时传入要查找的编号，类型是 Long。

    // 删除笔记（逻辑删除）
    @Update("UPDATE notes SET is_deleted = 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0") // 检查发布者 ID 是否与当前登录用户一致。
    int deleteById(@Param("id") Long id, @Param("userId") Long userId); // @Param("id") 和 @Param("userId") 分别给两个参数命名，供 SQL 中的 #{id} 和 #{userId} 使用。

    // 修改笔记标题和正文
    @Update("UPDATE notes SET title = #{title}, content = #{content} " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_deleted = 0")
    int update(Note note); // int：返回类型，表示方法执行后返回一个整数。update：方法名，按命名习惯表示“更新”。Note note：参数，表示要更新的笔记对象。参数类型是 Note，参数名是 note。

    @Select("SELECT id, title, content, created_at AS createdAt, " +
            "updated_at AS updatedAt, is_deleted AS isDeleted " +
            "FROM notes WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY created_at DESC, id DESC " +
            "LIMIT #{size} OFFSET #{offset}")
    List<Note> findByUserId( // List<Note>：返回类型，表示返回一个列表，列表里的每个元素都是 Note 对象。如果没有查到笔记，通常返回空列表，具体取决于实现。
            @Param("userId") Long userId, // @Param("userId")：给参数起一个名为 userId 的映射名称，常见于 MyBatis，用来在 SQL 中通过 #{userId} 引用它。Long userId：用户编号参数，类型是 Long。
            @Param("offset") long offset,
            @Param("size") int size // @Param("size") int size：本次查询最多返回多少条记录。例如 size 为 10 时，通常表示取 10 条。
    );

    @Select("SELECT COUNT(*) FROM notes " +
            "WHERE user_id = #{userId} AND is_deleted = 0")
    long countByUserId(@Param("userId") Long userId); // long：方法返回一个 long 整数，通常是符合条件的笔记总数。countByUserId：方法名，表示“按用户编号统计笔记数量”。
        }
