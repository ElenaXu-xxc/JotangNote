package com.jotangnote.entity;


import java.time.LocalDateTime; // 导入 Java 的 LocalDateTime 类型，用来表示日期和时间，不包含时区信息。

public class Note {

    private Long id; // 定义私有字段 id，用于保存笔记的编号。Long 是包装类型，因此可以为 null。
    private String title; // 定义私有字段 title，用于保存笔记标题。
    private String content;
    private Long userId;
    private LocalDateTime createdAt; // 定义私有字段 createdAt，用于保存笔记的创建时间。
    private LocalDateTime updatedAt; // 定义私有字段 updatedAt，用于保存笔记最近一次更新时间。
    private Integer isDeleted; // 定义私有字段 isDeleted，通常用于表示笔记是否已删除，例如约定 0 表示未删除、1 表示已删除。

    public Long getId() {
        return id;
    }

    public void setId(Long id) { // 定义公开的 setId 方法，用传入的 id 设置当前对象的编号。
        this.id = id; // 括号里的 Long id 是方法参数：调用方法时传进来的值，类型必须是 Long。这里参数名也叫 id。
    } // this.id 指的是当前这个 Note 对象的字段；等号右边的 id 指的是传进来的参数值。所以这行代码的意思是：把传入的编号保存到当前对象的 id 字段里。

    public String getTitle() { // 返回当前笔记的标题。
        return title;
    }

    public void setTitle(String title) { // 设置当前笔记的标题。
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) { // 设置笔记的删除状态。
        this.isDeleted = isDeleted;
    }

    public LocalDateTime getUpdatedAt() { // 返回笔记的最近更新时间。
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUserId() { // 返回这条笔记所属用户的编号。
    return userId;
    }

    public void setUserId(Long userId) { // 设置这条笔记所属用户的编号。
        this.userId = userId;
    }
}

