# Java 学习笔记 启蒙篇Part1

## 一、学习概述

目前已经接触到的主要内容：

- Java
- JDK
- Maven
- `pom.xml`
- Maven Wrapper
- `mvnw`
- Spring Initializr
- Spring Boot
- `HelloApplication`
- Java 项目基本结构

---

## 二、Java、JDK 与 Maven

### 2.1 Java

Java 是一种编程语言。

一个最基本的 Java 程序：

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

其中：

```java
public static void main(String[] args)
```

是 Java 程序的入口。

程序运行时，会从 `main` 方法开始执行。

---

### 2.2 JDK

JDK 是 **Java Development Kit**，即 Java 开发工具包。

进行 Java 开发时，需要 JDK 提供编译和运行 Java 程序所需要的环境。

---

### 2.3 Maven

Maven 是 Java 项目中常用的项目构建和依赖管理工具。

它可以帮助完成：

- 管理项目依赖
- 编译 Java 代码
- 运行测试
- 打包项目
- 管理项目构建流程

---

## 三、pom.xml

Maven 项目中通常存在：

```
pom.xml
```

这个文件是 Maven 项目的核心配置文件之一。

它可以描述：

- 项目的基本信息
- 项目依赖
- 构建配置
- 插件信息

---

## 四、Maven Wrapper

在 Spring Boot 项目中会看到：

```
mvnw
mvnw.cmd
.mvn/
```

其中：

`mvnw` 是 **Maven Wrapper**。

它的作用：

> 让项目使用自己指定的 Maven 环境，而不完全依赖电脑安装的 Maven。

---

### 4.1 使用方式

Linux / WSL 环境：

```bash
./mvnw
```

例如：

```bash
./mvnw clean
```

或者：

```bash
./mvnw package
```

---

### 4.2 `mvn` 与 `mvnw`

两者都可以执行 Maven 命令，但是来源不同：

```
mvn
↓
电脑安装的 Maven
```

```
./mvnw
↓
项目提供的 Maven Wrapper
```

因此：

`mvnw` 不是新的编程语言，而是 Maven 的一种运行方式。

---

## 五、Spring Initializr

Spring Initializr 是一个帮助快速创建 Spring Boot 项目的工具。

它可以自动生成：

- 项目目录结构
- Maven 配置
- 启动类
- 基础代码

---

## 六、Spring Boot

Spring Boot 是基于 Spring 生态的 Java 后端开发框架。

---

## 七、HelloApplication

使用 Spring Initializr 创建项目后，会生成类似：

```
HelloApplication.java
```

的启动类。

示例：

```java
@SpringBootApplication
public class HelloApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            HelloApplication.class,
            args
        );
    }

}
```

运行流程：

```
运行 Java 程序
        ↓
进入 main()
        ↓
执行 SpringApplication.run()
        ↓
启动 Spring Boot
        ↓
运行后端应用
```

---

## 八、Spring Boot 项目结构

一个基础 Spring Boot 项目：

```
hello/
├── .mvn/
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src/
    ├── main/
    │   └── java/
    │       └── HelloApplication.java
    └── test/
```

各部分作用：

| 文件/目录 | 作用 |
|---|---|
| `.mvn/` | Maven Wrapper 配置 |
| `mvnw` | Linux/macOS 使用的 Maven Wrapper |
| `mvnw.cmd` | Windows 使用的 Maven Wrapper |
| `pom.xml` | Maven 项目配置 |
| `src/main/java` | Java 源代码 |
| `src/test` | 测试代码 |
| `HelloApplication.java` | Spring Boot 启动类 |

---

## 九、目前对 Java 后端项目的理解

目前可以将整个体系理解为：

```
Java
↓
编写程序

JDK
↓
提供 Java 开发环境

Maven
↓
管理项目和依赖

pom.xml
↓
描述项目配置

Spring Initializr
↓
创建 Spring Boot 项目

Spring Boot
↓
开发后端应用

HelloApplication
↓
项目启动入口
```

---


# Java 学习笔记 启蒙篇part2

## 一、MySQL

1. 本质上，Excel是一个给人操作的表格文件，而MySQL是一个专门给程序读写的大型数据管理系统。MySQL理论上单表可以达到千万级、亿万级数据，单个数据库可以达到TB级甚至更大。如果单个MySQL不够了，会采用分库、分表和集群。

2.  * 索引 查找时可以像翻目录一样，不用一个一个找。
    * 数据结构优化（如 B+Tree） 可以一次排除大量数据。
    * 缓存 MySQL有自己的缓存，默认主要缓存数据页和索引页。
    * 事务和优化机制 保证修改可靠。

3. * JDBC *Java和数据库沟通的桥梁*
   * 实际开发中通常使用Spring Boot+MyBatis+MySQL，
    MyBaits自动完成：
    Java方法调用
    ↓
    生成SQL
    ↓
    发送给MySQL
    ↓   
    获得结果
    ↓   
    封装成User对象

---

## 二、Redis

1. 为什么 Redis 快（原理）
   
    MySQL 是磁盘数据库，虽然有缓存、索引等优化，但是最终数据主要存储在磁盘。
    Redis 是内存数据库。
    * 数据存在内存中：从内存和从磁盘中读取数据的速度差距非常大
    * Redis 数据结构简单：MySQL 存数据需要表结构、索引、事务、锁和SQL解析，而 Redis 直接 `user:1 → {"name":"Tom","age":20}` 。
    * Redis单线程模型：它避免了大量 线程切换 和 锁竞争。

2. MySQL 数据如何进入 Redis（缓存设计）
    
    用户请求
    ↓
    Spring Boot
    ↓
    Redis
    ↓
    (有数据)
    直接返回
    ↓
    (没有数据)
    查询MySQL
    ↓
    放入Redis
    * Redis用 JSON 字符串存储
    * 用 Hash 结构存储
    * 只存热点数据

3. Java 如何操作 Redis（工程实践）
    
    和MySQL类似，Java不能直接操作Redis，需要客户端。
    Spring Boot中最常用 Spring Data Redis。

---

## 三、RabbitMQ

1. 什么项目会引入 MQ？
  
    * 异步处理耗时任务（引导中给的例子）
    **减少用户等待**

    * 削峰填谷
    **防止流量冲垮系统**
    大量请求同时涌现时，服务器无法同时解决， RabbitMQ 可以控制请求流速，让请求排队，使数据库只承受它能承受的速度。可达成*系统不崩溃，请求最终正常完成*成就！

    * 服务之间解耦
    **降低模块之间依赖**
    如果是流水线式的服务流程，一个服务挂了，整个流程就会失败。将模式改为让生产消息的服务（比如订单服务）不再直接调用处理消息的服务（比如库存、短信、积分），双方只通过 MQ 交换消息，从而降低系统之间的依赖。

2. Java 如何操作 RabbitMQ？
    
    * 第一步：引入依赖

    Spring Boot：

    ```XML
    <dependency>
        <groupId>
            org.springframework.boot
        </groupId>

        <artifactId>
            spring-boot-starter-amqp
        </artifactId>
    </dependency>

    ```
    AMQP = Advanced Message Queuing Protocol,是RabbitMQ 使用的通信协议。

    * 第二步：发送信息

    ```java
    @Autowired
    RabbitTemplate rabbitTemplate;

    public void register(){
        //保存用户
        //发送消息
        rabbitTemplate.convertAndSend(
            "user.queue",
            "用户注册成功"
        );
    }

    ```
    * 第三步：接收信息

    ```java
    @Component
    public class UserConsumer {

    @RabbitListener(
    queues="user.queue"
    )
    public void receive(String message){

        System.out.println(message);

    }
    }

    ```
    当 RabbitMQ 有消息，自动调用
    
    ```java
    receive()
    ```
---

## 四、AI Agent

1. AI Agent 是如何让大模型除了文本回答，还能进行更多操作的？
   
    Agent = 大模型 + 工具（Tool）+ 规划能力 + 执行循环，
    AI Agent有权限调用工具直接执行任务。

2. 长期使用 Agent 会产生大量信息，如何存储？

    * 长期记忆
    把重要的，提炼后的信息存数据库。

    * 向量数据库
    普通数据库适合精准查询，但是AI需要找相似意思的信息。

---

# Java 学习笔记 启蒙篇part3

## 一、 HTTP 请求 & 响应

1. 一个标准的 HTTP 请求和响应, 都有哪些部分?

    * HTTP请求
    包括： 请求行（Request Line） 请求头（Headers） 空行 请求体（Body）
    
    例如：
    ```http
    POST /api/notes HTTP/1.1

    Host: localhost:8080
    Content-Type: application/json
    Authorization: Bearer xxx

    {
        "title":"我的第一篇笔记",
        "content":"学习HTTP"
    }
    ```

    ① 请求行（Request Line）

        ```http
        POST /api/notes HTTP/1.1
        ```
     包含三个信息：请求方法  URL  HTTP版本
        
        请求方法：（表示我对服务器执行什么操作）
         GET
         POST
         PUT
         DELETE
       
        URL：（表示我要访问服务器中的哪个资源）
         例如：
         ```text
         /api/notes
         ```

        HTTP版本：（表示当前使用的 HTTP 协议版本）
         例如：
         ```text
         HTTP/1.1
         ```

    
    ② 请求头（Headers）
        
        ```http
        Host: localhost:8080
        Content-Type: application/json
        Authorization: Bearer token
        ```
     用于携带额外信息。

        常见请求头：
        | Header | 作用 |
        | --- | --- |
        | Host | 服务器地址 |
        | Content-Type | 请求数据格式 |
        | Authorization | 身份认证 |
        | Cookie | 用户状态信息 |
        | User-Agent | 客户端信息 |
    
    
    ③ 请求体（Body）

        ```json
        {
            "title":"Redis学习",
            "content":"Redis很快"
        }
        ```
    请求体用于携带提交的数据。

        通常：
        - GET 请求没有 Body
        - POST / PUT 请求经常包含 Body


    * HTTP响应
    包括：状态行（Status Line） 响应头（Headers） 空行  响应体（Body）

    例如：
    ```http
    HTTP/1.1 200 OK

    Content-Type: application/json

    {
        "id":1001,
        "title":"Redis学习"
    }
    ```

     ① 状态行

        ```http
        HTTP/1.1 200 OK
        ```
    包含三个信息：HTTP版本  状态码  状态描述


     ② 响应头（Headers）

    ```http
    Content-Type: application/json
    Set-Cookie: xxx
    ```
    用于告诉客户端：
    - 返回的数据类型
    - Cookie 信息
    - 缓存策略等

    ③ 响应体（Body）

        ```json
        {
            "id":1,
            "title":"hello"
        }
        ```
    服务器真正返回的数据。


2. HTTP 规定了很多种方法, 最常用的是 GET 和 POST. 如果在 JotangNote 项目中, 用户需要向服务器提交一篇刚刚写好的长篇笔记, 你应该用哪种动作? 为什么?

    > 使用POST。
    用 GET 表示获取资源，用 POST 表示提交数据，让服务器处理。


3. HTTP 状态码是什么? 都有哪些常见的状态码? 你在开发 JotangNote 时, 可能在什么情况下用到哪些状态码?

    > 状态码：服务器通过数字告诉客户端请求处理结果。
    格式：```text
         三位数字
         ```
    分类：| 范围 | 含义 |
        | --- | --- |
        | 1xx | 处理中 |
        | 2xx | 成功 |
        | 3xx | 重定向 |
        | 4xx | 客户端错误 |
        | 5xx | 服务器错误 |

    > 常见状态码
    200 OK ：请求成功
    201 Created ：创建成功
    400 Bad Request ：请求格式错误
    401 Unauthorized ：未登录
    403 Forbidden ：没有权限
    404 Not Found ：资源不存在
    500 Internal Server Error ：服务器内部错误

    > 可能出现的场景码
    获取笔记成功：200
    创建笔记成功：201
    修改笔记成功：200
    删除笔记：204
    没有登录：401
    没有权限：403
    笔记不存在：404
    数据库异常：500

---

## 二、接口/API

1. 假设在我们的 JotangNote 项目中, 你需要提供一个"获取某篇笔记详细内容"的功能. 如果让你来设计这个接口, 你要考虑什么内容?

    1. **接口地址（URL）**
    2. **请求方法（GET / POST 等）**
    3. **请求参数**
    4. **返回数据格式**
    5. **错误处理方式**

---

## 三、JSON

1. JSON 格式的数据长什么样?

    >JSON本质上是一种用字符串表示结构化的格式

    例如：一个用户对象：

    ```json
    {
        "id": 1001,
        "name": "Tom",
        "age": 20
    }
    ```
    其中：
    - `{}` 表示一个对象；
    - `"key": value` 表示键值对；
    - 多个字段之间使用 `,` 分隔。

    > JSON 常见数据类型

    * 对象（Object）
    使用 `{}` 表示：

    ```json
    {
        "name": "Tom",
        "age": 20
    }
    ```
    * 数组（Array）
    使用 `[]` 表示：
    ```json
    [
        "Java",
        "C++",
        "Python"
    ]
    ```

    * 字符串
    必须使用双引号：
    ```json
    {
        "name": "Tom"
    }
    ```

    * 数字
    ```json
    {
        "age": 20
    }
    ```

    * 布尔值
    ```json
    {
        "isStudent": true
    }
    ```
    * 空值
    ```json
    {
        "email": null
    }
    ```
    
2. 如何使用你所选择的编程语言来将对象转化成 JSON, 或者将 JSON 转化成一个对象？

    > Java 对象转化为 JSON
    
    ```java
    public class User {

        private int id;
        private String name;

    }
    
    User user = new User(1, "Tom");
    
    ObjectMapper mapper = new ObjectMapper();

    String json = mapper.writeValueAsString(user);
    ```

    结果：

    ```json
    {
        "id":1,
        "name":"Tom"
    }
    ```

    > JSON 转 Java 对象
    
    ```json
    {
        "id":1,
        "name":"Tom"
    }
    ```

    代码：

    ```java
    User user = mapper.readValue(json, User.class);
    ```

    得到：

    ```java
    User对象
    ```
---

## 四、HTTPS

1. HTTPS 的加密过程是怎么样的?

    简单流程：

    ```
    客户端                    服务器

    |                         |
    |------ 请求连接 --------> |
    |                         |
    |<----- 返回证书 -------- |
    |                         |
    |--- 验证证书 ------------|
    |                         |
    |--- 协商加密密钥 -------->|
    |                         |
    |====== 加密通信 =========|
    ```

    
    ①证书里面包含：
    - 服务器身份信息
    - 服务器公钥
    - 证书颁发机构（CA）

    ②协商加密密钥
    HTTPS 使用：非对称加密 + 对称加密

        非对称加密
        - 公钥 ：可以公开，大家都可以获得。
        - 私钥 ：只有服务器拥有，用于安全交换信息。

        对称加密
        通信双方最终使用 同一个密钥 进行数据加密。

---

# Java 实操记录

## 启蒙篇 Part1

1. ![Hello World From Terminal!](Notes/Images/1.png)

2. ![Hello World From Web Application!](Notes/Images/2.png)

---

## 

