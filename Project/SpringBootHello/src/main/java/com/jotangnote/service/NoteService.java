
package com.jotangnote.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jotangnote.entity.Note;
import com.jotangnote.mapper.NoteMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteMapper noteMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    public NoteService(NoteMapper noteMapper,
                   StringRedisTemplate redisTemplate,
                   ObjectMapper objectMapper) {
    this.noteMapper = noteMapper;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    }

    // 新增笔记
    public int createNote(Note note) {
        return noteMapper.insert(note);
    }

    // 根据 ID 查询笔记：先查缓存，再查数据库
    public Note getNoteById(Long id) {
        // 1. 根据笔记 ID 生成 Redis 缓存键
        String key = "note:" + id;

        // 2. 尝试从 Redis 读取缓存
        String cachedNote = redisTemplate.opsForValue().get(key);

        // 3. 缓存命中时，下一步将 JSON 还原为 Note 对象
        if (cachedNote != null) {
            try {
                return objectMapper.readValue(cachedNote, Note.class);
            } catch (JacksonException e) {
                // 缓存数据格式异常时，删除错误缓存，继续查询数据库
                redisTemplate.delete(key);
            }
        }

        // 4. 缓存未命中，查询 MySQL
        Note note = noteMapper.findById(id);

        // 5. 没查到笔记，返回 null，不缓存不存在的数据
        if (note == null) {
            return null;
        }

        // 6. 查到了笔记，下一步将 Note 转成 JSON 并写入 Redis
        try {
            String json = objectMapper.writeValueAsString(note);
            redisTemplate.opsForValue().set(key, json);
        } catch (JacksonException e) {
            // 序列化失败时仍然返回数据库查到的笔记
            System.err.println("笔记缓存序列化失败：" + e.getMessage());
        }

        return note;
    }

    // 删除笔记
    public int deleteNote(Long id, Long userId) { // 方法现在同时接收笔记 ID 和当前用户 ID。
        int result = noteMapper.deleteById(id, userId); // 调用 noteMapper 中的 deleteById 方法，告诉它要删除哪篇笔记、这篇笔记属于哪个用户，并把执行结果保存到 result 变量中。
        if (result > 0) { // 只有数据库操作确实影响了记录，才继续清理缓存。
            redisTemplate.delete("note:" + id); // 删除该笔记对应的 Redis 缓存。例如，ID 为 2 时删除 note:2。
        }

        return result; // 返回原来的数据库操作结果，保持现有接口的行为不变。
    }

    // 修改笔记,并清除对应的 Redis 缓存
    public int updateNote(Note note) {
        int result = noteMapper.update(note);

        if (result > 0) {
            redisTemplate.delete("note:" + note.getId());
        }

        return result;
    }

    public List<Note> getNotesByUserId(
            Long userId,
            long offset,
            int size
    ) {
        return noteMapper.findByUserId(userId, offset, size);
    }

    public long countNotesByUserId(Long userId) {
        return noteMapper.countByUserId(userId);
    }
}
