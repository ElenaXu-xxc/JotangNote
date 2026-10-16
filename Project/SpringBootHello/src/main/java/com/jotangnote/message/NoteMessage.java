package com.jotangnote.message;

import com.jotangnote.entity.Note;

public class NoteMessage {

    private String action; // action表示操作类型，例如 "create"、"update"、"delete" 等。

    private Note note; 

    private Long noteId;

    private Long userId;

    public NoteMessage() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}