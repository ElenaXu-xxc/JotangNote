package com.jotangnote.message;

import com.jotangnote.config.RabbitMQConfig;
import com.jotangnote.entity.Note;
import com.jotangnote.service.NoteService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NoteMessageConsumer {

    private final NoteService noteService;

    public NoteMessageConsumer(NoteService noteService) {
        this.noteService = noteService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTE_QUEUE)
    public void handleMessage(NoteMessage message) { // Spring 会使用 JSON 消息转换器，把收到的消息转换为 NoteMessage 对象。
        String action = message.getAction();

        switch (action) {
            case "CREATE" -> { // 这里的 -> 是 Java 中 switch 新语法的一部分，表示：当 case 匹配时，执行箭头右边的代码。较普通写法，用箭头可以避免忘记写 break 导致的“穿透”问题。
                Note note = message.getNote();
                note.setUserId(message.getUserId());
                noteService.createNote(note);
            }
            case "UPDATE" -> {
                Note note = message.getNote();
                note.setId(message.getNoteId());
                note.setUserId(message.getUserId());
                noteService.updateNote(note);
            }
            case "DELETE" -> {
                noteService.deleteNote(
                        message.getNoteId(),
                        message.getUserId()
                );
            } // 这里同时传入笔记 ID 和用户 ID，保留数据库层的归属校验。不能因为操作改成异步，就允许用户删除其他人的笔记。
            default -> throw new IllegalArgumentException(
                    "未知的笔记操作类型：" + action
            );
        }
    }
}