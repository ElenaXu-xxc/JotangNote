# RabbitMQ 异步处理实践

## 一、实践目标

本次实践的目标是使用 RabbitMQ 改造 JotangNote 笔记系统的新增、修改和删除接口，将原本由 Controller 直接调用 Service 执行的同步操作，改为通过消息队列进行异步处理。

具体要求如下：

1. 调用接口后，不必等待数据库操作完成再返回。
2. Controller 将操作封装成消息，并发送到 RabbitMQ。
3. 后台消费者从消息队列中获取消息，执行对应的业务操作。
4. 保留原有的用户身份验证、笔记归属权限检查和 Redis 缓存处理逻辑。

## 二、实现思路

### 2.1 原来的同步处理流程

在原来的实现中，Controller 直接调用 NoteService，等待数据库操作完成后，再根据执行结果返回响应。

```text
客户端
  ↓
NoteController
  ↓
NoteService
  ↓
MySQL
  ↓
返回操作结果
```

这种方式下，Controller 需要等待 Service 完成数据库操作，才能返回最终结果。

### 2.2 改造后的异步处理流程

引入 RabbitMQ 后，Controller 不再直接执行新增、修改和删除操作，而是把任务封装为消息发送到消息队列。

```text
客户端
  ↓
NoteController
  ↓
NoteMessageProducer
  ↓
RabbitMQ Exchange
  ↓
RabbitMQ Queue
  ↓
NoteMessageConsumer
  ↓
NoteService
  ↓
MySQL
```

Controller 在调用消息发送方法后返回任务已提交的响应。消费者在后台接收消息，并调用原有的 NoteService 方法完成数据库操作。

需要注意，接口返回“任务已提交”不代表数据库操作已经成功。数据库操作的最终结果需要通过后续查询等方式确认。

## 三、核心代码与实现

### 3.1 添加 RabbitMQ 依赖

在 `pom.xml` 中添加 Spring AMQP 依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

该依赖提供 Spring Boot 与 RabbitMQ 的集成功能，包括消息发送、消息监听和相关自动配置。

### 3.2 配置 RabbitMQ 连接

在 `application.properties` 中配置连接信息：

```properties
spring.rabbitmq.host=127.0.0.1
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

各项配置的含义如下：

- `host`：RabbitMQ 服务所在的主机地址。本次实践中，Spring Boot 和 RabbitMQ 都运行在同一个 WSL 环境中，因此使用 `127.0.0.1`。
- `port`：RabbitMQ 的 AMQP 通信端口，默认是 `5672`。
- `username` 和 `password`：连接 RabbitMQ 使用的账号和密码。

在本次环境中，使用 `127.0.0.1` 替代 `localhost` 后，解决了主机名解析异常，Spring Boot 能够正常启动。

### 3.3 配置队列、交换机和绑定关系

在 `RabbitMQConfig` 中定义消息队列、交换机和绑定关系。

主要配置包括：

- `NOTE_QUEUE`：队列名称 `note.queue`。
- `NOTE_EXCHANGE`：交换机名称 `note.exchange`。
- `NOTE_ROUTING_KEY`：路由键 `note.routingKey`。
- `noteQueue()`：声明持久化队列。
- `noteExchange()`：声明 Direct 类型的交换机。
- `noteBinding()`：将队列绑定到交换机，并指定路由键。
- `messageConverter()`：配置 JSON 消息转换器，使消息对象能够转换为 JSON 格式进行传输。

通过交换机、路由键和绑定关系，Producer 发送的消息可以被路由到对应的队列。

### 3.4 定义消息对象 NoteMessage

`NoteMessage` 用于描述需要执行的笔记操作。

主要字段如下：

| 字段 | 含义 |
|---|---|
| `action` | 操作类型，例如 `CREATE`、`UPDATE`、`DELETE` |
| `note` | 笔记对象，保存标题、正文等信息 |
| `noteId` | 需要修改或删除的笔记 ID |
| `userId` | 当前登录用户的 ID |

使用统一的消息对象，可以让消费者根据 `action` 判断应该执行哪一种业务操作。

### 3.5 使用 NoteMessageProducer 发送消息

`NoteMessageProducer` 负责把消息发送到 RabbitMQ。

核心逻辑为：

```java
public void send(NoteMessage message) {
    rabbitTemplate.convertAndSend(
            RabbitMQConfig.NOTE_EXCHANGE,
            RabbitMQConfig.NOTE_ROUTING_KEY,
            message
    );
}
```

代码说明：

- `rabbitTemplate` 是 Spring 提供的消息操作工具。
- `convertAndSend()` 将消息发送到指定交换机，并根据路由键进行路由。
- `message` 是需要传递给消费者的任务对象。

通过 Producer，Controller 不需要直接操作 RabbitMQ 的底层连接。

### 3.6 使用 NoteMessageConsumer 消费消息

`NoteMessageConsumer` 使用 `@RabbitListener` 监听 `note.queue`。

收到消息后，根据操作类型执行相应逻辑：

- `CREATE`：获取笔记对象，设置用户 ID，然后调用 `noteService.createNote(note)`。
- `UPDATE`：设置笔记 ID 和用户 ID，然后调用 `noteService.updateNote(note)`。
- `DELETE`：使用笔记 ID 和用户 ID 调用 `noteService.deleteNote(noteId, userId)`。

其中，`@RabbitListener` 用于声明消息监听方法。消费者收到消息后，会根据 `action` 选择对应的操作。

消费者复用了原有的 NoteService，因此不需要在 Consumer 中重新编写数据库操作 SQL。

### 3.7 修改 NoteController

改造前，Controller 直接调用 NoteService，例如：

```java
int result = noteService.createNote(note);
```

然后根据 `result` 判断数据库操作是否成功。

改造后，Controller 负责完成以下工作：

1. 从 Session 中获取当前登录用户 ID。
2. 将用户 ID 设置到笔记对象或消息对象中。
3. 创建 `NoteMessage`，设置操作类型和相关数据。
4. 调用 `noteMessageProducer.send(message)` 发送消息。
5. 返回“笔记创建任务已提交”“笔记修改任务已提交”或“笔记删除任务已提交”。

由于 Controller 不再等待消费者完成数据库操作，因此不能在 Controller 中直接根据数据库影响行数判断最终操作结果。

### 3.8 保留原有权限与缓存逻辑

本次改造并不是重新实现笔记业务，而是调整业务操作的执行方式。

- 登录用户 ID 仍然从服务器端 Session 获取，不能信任客户端提交的 `userId`。
- 修改和删除操作仍然通过原有 Service、Mapper 执行数据库操作。
- 数据库更新和软删除仍应保留笔记 ID、用户 ID 等归属条件。
- 原有 Service 中的 Redis 缓存清理逻辑仍由后台消费者调用 Service 时执行。

因此，消息队列负责传递任务，NoteService 负责执行业务，Mapper 负责数据库访问。

## 四、测试过程与结果

### 4.1 编译测试

执行：

```bash
./mvnw -DskipTests compile
```

该命令跳过测试执行，编译项目中的 Java 代码。

**测试结果：** 编译成功，说明当前代码通过了编译检查。但编译成功本身不能证明 RabbitMQ 连接和消息处理一定正常。

### 4.2 RabbitMQ 连接测试

启动 RabbitMQ 服务，并运行 Spring Boot 项目。

本次实践中，最初出现了：

```text
java.net.UnknownHostException: localhost
```

随后将 RabbitMQ 主机配置修改为：

```properties
spring.rabbitmq.host=127.0.0.1
```

修改后，Spring Boot 成功启动，RabbitMQ 消息监听组件能够继续进行运行测试。

### 4.3 异步新增笔记测试

请求接口：

```bash
curl -i -b cookies.txt \
  -X POST http://localhost:8080/notes \
  -H "Content-Type: application/json" \
  -d '{"title":"RabbitMQ异步测试","content":"测试笔记新增任务是否通过消息队列执行"}'
```

预期接口返回：

```text
笔记创建任务已提交
```

随后查询 MySQL，确认新增笔记已经保存。
`Images/rabbitmq-create-test.png`

### 4.4 异步修改笔记测试

使用 PUT 请求修改测试笔记的标题和正文。

接口返回：

```text
笔记修改任务已提交
```

等待后台消费者处理后，再次调用查询接口，确认标题和正文已经更新。
`Images/rabbitmq-update-test.png`

### 4.5 异步删除笔记测试

使用 DELETE 请求删除测试笔记。

接口返回：

```text
笔记删除任务已提交
```

随后查询 MySQL 中对应的笔记记录，检查 `is_deleted` 字段。

预期结果：

```text
is_deleted = 1
```

该字段变为 `1` 表示笔记被软删除，数据库记录仍然保留。
`Images/rabbitmq-delete-test.png`

## 五、实践总结

本次实践完成了 JotangNote 笔记系统的 RabbitMQ 异步处理改造。

主要收获如下：

1. 理解了同步处理与异步处理的区别。
2. 学会了使用 Spring AMQP 配置队列、交换机和绑定关系。
3. 学会了使用 `RabbitTemplate` 发送消息。
4. 学会了使用 `@RabbitListener` 接收消息并执行后台任务。
5. 将 Controller 的请求处理与实际业务执行分离。
6. 理解了通过复用 Service，可以在调整执行方式的同时保留原有业务逻辑。

通过新增、修改和删除的实际测试，验证了三种操作的基本异步处理流程。
