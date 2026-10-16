package com.jotangnote.message;

import org.springframework.amqp.rabbit.core.RabbitTemplate; // RabbitTemplate：Spring 提供的 RabbitMQ 操作工具。
import org.springframework.stereotype.Component;
import com.jotangnote.config.RabbitMQConfig;

@Component // 让 Spring 自动创建并管理这个类的对象。
public class NoteMessageProducer { // 通过构造方法接收 Spring 创建的 RabbitTemplate。

    private final RabbitTemplate rabbitTemplate;

    public NoteMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(NoteMessage message) { // send(...)：负责发送消息。
        rabbitTemplate.convertAndSend( // convertAndSend(...)：将消息转换为可传输的形式，并发送给指定交换机和路由键。
                RabbitMQConfig.NOTE_EXCHANGE,
                RabbitMQConfig.NOTE_ROUTING_KEY,
                message
        );
    }
}
