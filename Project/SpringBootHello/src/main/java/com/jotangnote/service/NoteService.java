
package com.jotangnote.service;

import com.jotangnote.entity.Note;
import com.jotangnote.mapper.NoteMapper;
import org.springframework.stereotype.Service;

@Service
public class NoteService {

    private final NoteMapper noteMapper;

    public NoteService(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    // 新增笔记
    public int createNote(Note note) {
        return noteMapper.insert(note);
    }

    // 根据 ID 查询笔记
    public Note getNoteById(Long id) {
        return noteMapper.findById(id);
    }

    // 删除笔记
    public int deleteNote(Long id, Long userId) {
        return noteMapper.deleteById(id, userId);
}

    // 修改笔记
    public int updateNote(Note note) {
        return noteMapper.update(note);
    }
}
