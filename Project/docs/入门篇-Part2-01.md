# Part 2：Redis 查询缓存

## 一、任务目标

为 JotangNote 笔记管理项目添加 Redis 查询缓存功能。

原本查询笔记时，后端需要访问 MySQL 数据库。添加缓存后，后端会优先查询 Redis。如果 Redis 中已经存在对应笔记，就直接返回缓存数据；如果缓存不存在，则查询 MySQL，并将查询到的笔记写入 Redis，供后续请求使用。

本任务的主要目标是减少重复查询数据库的开销，提高数据查询效率。

## 二、功能实现流程

查询笔记的整体流程如下：

1. 用户发送 GET 请求，例如 `GET /notes/2`。
2. 后端根据笔记 ID 构造 Redis 缓存键，例如 `note:2`。
3. 首先查询 Redis。
4. 如果 Redis 中存在缓存，将缓存中的 JSON 字符串转换为 `Note` 对象并返回。
5. 如果 Redis 中没有缓存，则查询 MySQL 数据库。
6. 如果 MySQL 中没有符合查询条件的笔记，返回空结果。
7. 如果 MySQL 中存在笔记，将笔记转换为 JSON 字符串，写入 Redis，然后返回笔记数据。

需要注意，当前查询语句只会查询 `is_deleted = 0` 的笔记，因此已逻辑删除的笔记不会被正常查询出来。

## 三、核心代码与个人理解

### 1. 引入 Redis 和 JSON 处理工具

在 `NoteService.java` 中使用以下依赖：

- `StringRedisTemplate`：用于操作 Redis 中的字符串类型数据。
- `ObjectMapper`：用于在 Java 对象与 JSON 字符串之间进行转换。
- `JacksonException`：用于处理 Jackson 进行 JSON 转换时可能出现的异常。

当前项目使用 Spring Boot 4，因此使用 Jackson 3 对应的包名：

```java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
```

### 2. 构造缓存键并查询 Redis

```java
String key = "note:" + id;
String cachedNote = redisTemplate.opsForValue().get(key);
```

第一行根据笔记 ID 生成缓存键。例如，笔记 ID 为 `2` 时，缓存键为 `note:2`。

第二行通过 `StringRedisTemplate` 查询 Redis 中对应的字符串。如果缓存存在，`cachedNote` 就会保存缓存内容；如果不存在，其值为 `null`。

### 3. 命中缓存后直接返回

```java
if (cachedNote != null) {
    try {
        return objectMapper.readValue(cachedNote, Note.class);
    } catch (JacksonException e) {
        redisTemplate.delete(key);
    }
}
```

如果缓存不为空，说明 Redis 中存在对应数据。

Redis 保存的是 JSON 字符串，而业务代码需要使用 `Note` 对象，因此使用 `readValue()` 将 JSON 字符串转换成 `Note` 对象并返回。

如果 JSON 转换失败，就删除该缓存键，随后继续执行数据库查询逻辑，尝试重新获取数据。

### 4. 缓存未命中时查询 MySQL

```java
Note note = noteMapper.findById(id);

if (note == null) {
    return null;
}
```

如果 Redis 中没有缓存，程序就调用 `NoteMapper` 查询 MySQL。

如果数据库中也没有符合条件的笔记，则返回 `null`，不将空结果写入 Redis。

### 5. 将查询结果写入 Redis

```java
try {
    String json = objectMapper.writeValueAsString(note);
    redisTemplate.opsForValue().set(key, json);
} catch (JacksonException e) {
    System.err.println("笔记缓存序列化失败：" + e.getMessage());
}
```

`writeValueAsString()` 将 Java 对象转换成 JSON 字符串。

随后使用 `redisTemplate.opsForValue().set(key, json)` 将 JSON 字符串写入 Redis。

当前实现没有设置过期时间，因此缓存会一直保留，直到被主动删除、覆盖或 Redis 数据被清除。

如果 JSON 序列化失败，程序会输出错误信息，但仍然可以返回从 MySQL 查询到的笔记。

## 四、测试过程与结果

### 1. 测试接口

使用以下命令查询 ID 为 `2` 的笔记：

```bash
curl -i http://localhost:8080/notes/2
```

测试结果：

- HTTP 状态码为 `200`。
- 响应头中的 `Content-Type` 为 `application/json`。
- 响应体中包含笔记的标题、正文、ID、创建时间、更新时间等信息。

### 2. 检查 Redis 缓存

使用以下命令查看缓存：

```bash
redis-cli GET note:2
```

测试结果：Redis 返回了对应笔记的 JSON 字符串，说明笔记数据已经成功写入 Redis。

为了便于阅读中文内容，也可以使用：

```bash
redis-cli --raw GET note:2
```

### 3. 再次查询

再次请求：

```bash
curl -i http://localhost:8080/notes/2
```

测试结果：接口正常返回笔记数据，Redis 中的缓存仍然存在。
`Images/redis/test.png`

说明查询接口正常，且缓存数据已写入并保留。不过，本次测试没有通过日志或其他手段确认第二次请求实际命中了 Redis，因此暂不将这一点记为已独立验证的结果。

## 五、总结

本次任务为 JotangNote 的笔记查询接口添加了 Redis 缓存。

通过 `StringRedisTemplate` 查询和写入缓存，结合 `ObjectMapper` 完成 Java 对象与 JSON 字符串的转换，实现了“先查 Redis，缓存未命中时再查 MySQL”的基本流程。
