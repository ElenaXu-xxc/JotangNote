package com.jotangnote.controller;

import com.jotangnote.entity.Note;
import com.jotangnote.service.NoteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping; // 用于把 Java 方法映射到 HTTP 的 GET 请求，通常用于查询数据
import org.springframework.web.bind.annotation.PathVariable; // 用于读取 URL 路径中的变量，例如从 /notes/1 中取出 1
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping; // PutMapping是Spring Boot 提供的注解，用于处理 HTTP 的 PUT 请求。我们用它来表示修改一篇已经存在的笔记
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.jotangnote.message.NoteMessage; // NoteMessage：封装笔记操作的消息对象，包含操作类型、笔记内容、用户 ID 等信息。
import com.jotangnote.message.NoteMessageProducer; // NoteMessageProducer：负责发送消息到 RabbitMQ 的生产者类。

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final NoteService noteService;
    private final NoteMessageProducer noteMessageProducer; // 声明一个消息生产者变量，让Controller可以使用它发送消息。


    public NoteController(
            NoteService noteService,
            NoteMessageProducer noteMessageProducer
    ) {
        this.noteService = noteService;
        this.noteMessageProducer = noteMessageProducer; // 把注入的对象保存下来，共三个接口调用。
    }

    // 获取当前登录用户的笔记列表
    @GetMapping
    public NoteListResponse getNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        Long userId = getCurrentUserId(session);
        int normalizedPage = page < 1 ? DEFAULT_PAGE : page;
        int normalizedSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        long offset = (long) (normalizedPage - 1) * normalizedSize;

        List<NoteItemResponse> notes = noteService
                .getNotesByUserId(userId, offset, normalizedSize)
                .stream()
                .map(note -> new NoteItemResponse(
                        note.getId(),
                        note.getTitle(),
                        note.getContent(),
                        note.getCreatedAt(),
                        note.getUpdatedAt()
                ))
                .toList();
        long total = noteService.countNotesByUserId(userId);
        int totalPages = total == 0
                ? 0
                : (int) ((total + normalizedSize - 1) / normalizedSize);

        return new NoteListResponse(
                notes,
                normalizedPage,
                normalizedSize,
                total,
                totalPages
        );
    }

    // 获取当前登录用户    
    private Long getCurrentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId"); // session.getAttribute("userId")：从服务器保存的登录 Session 中获取用户 ID。

        if (userId == null) { // userId == null：如果没有保存用户 ID，说明当前会话没有有效的登录状态。
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "请先登录" // HttpStatus.UNAUTHORIZED：返回 HTTP 401，表示请求尚未通过身份认证。
            );
        }

        return (Long) userId;
    }

    // 新增笔记
    @PostMapping
    public String createNote(@RequestBody Note note, HttpSession session) {
        Long userId = getCurrentUserId(session); // 创建笔记时绑定用户。即使客户端提交了其他 userId，服务器也会用 Session 中的值覆盖它。因此，客户端不能通过修改请求 JSON 来冒充其他用户。
        note.setUserId(userId);

        NoteMessage message = new NoteMessage(); // 创建一个用来传递任务的消息对象。
        message.setAction("CREATE"); // 告诉消费者这是新增笔记任务。
        message.setNote(note); // 把笔记标题、正文等信息放入消息。
        message.setUserId(userId); // 把用户 ID 放入消息，确保消费者可以知道这条笔记属于哪个用户。

        noteMessageProducer.send(message);

        return "笔记创建任务已提交";
    }

    // 根据 ID 查询笔记
    @GetMapping("/{id}")
    public Note getNoteById(@PathVariable Long id) {
        return noteService.getNoteById(id);
    }

    // 删除笔记
    @DeleteMapping("/{id}")
    public String deleteNote(
            @PathVariable Long id,
            HttpSession session
    ) {
        Long userId = getCurrentUserId(session);
        NoteMessage message = new NoteMessage();
        message.setAction("DELETE");
        message.setNoteId(id);
        message.setUserId(userId);

        noteMessageProducer.send(message);

        return "笔记删除任务已提交";
    }

    // 修改笔记
    @PutMapping("/{id}")
    public String updateNote(
            @PathVariable Long id,
            @RequestBody Note note,
            HttpSession session
    ) {
        Long userId = getCurrentUserId(session);
        note.setId(id); // setId(id)：使用 URL 路径中的笔记 ID 确定要修改哪条笔记。  
        note.setUserId(userId); // setUserId(userId)：使用服务器确认的登录用户 ID。

        NoteMessage message = new NoteMessage();
        message.setAction("UPDATE");
        message.setNote(note);
        message.setNoteId(id);
        message.setUserId(userId);

        noteMessageProducer.send(message);

        return "笔记修改任务已提交";
        }

    public record NoteListResponse(
            List<NoteItemResponse> notes,
            int page,
            int size,
            long total,
            int totalPages
    ) {
    }

    public record NoteItemResponse(
            Long id,
            String title,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
