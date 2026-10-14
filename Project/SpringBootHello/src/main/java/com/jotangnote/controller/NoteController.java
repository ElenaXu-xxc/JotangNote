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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
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

        int result = noteService.createNote(note);

        if (result > 0) {
            return "笔记创建成功";
        } else {
            return "笔记创建失败";
        }
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
        int result = noteService.deleteNote(id, userId);

        if (result > 0) {
            return "笔记删除成功";
        } else {
            return "笔记不存在、已删除或无权删除";
        }
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

        int result = noteService.updateNote(note); // updateNote(note)：交给 Service，再由 Mapper 执行带归属条件的 SQL。

        if (result > 0) {
            return "笔记修改成功";
        } else {
            return "笔记不存在、已删除或无权修改";
        }
    }
}