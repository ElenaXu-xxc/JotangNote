package com.jotangnote.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTE_QUEUE = "note.queue"; // NOTE_QUEUE	:队列名称，后台消费者从这里取消息
    public static final String NOTE_EXCHANGE = "note.exchange"; // NOTE_EXCHANGE:交换机名称，生产者发消息到这里，交换机再转发到队列
    public static final String NOTE_ROUTING_KEY = "note.routingKey"; // NOTE_ROUTING_KEY:路由键，生产者发消息时指定的标签，交换机根据这个标签把消息转发到对应队列

    @Bean // 让 Spring 创建并管理这个转换器对象。
    public Queue noteQueue() {
        return new Queue(NOTE_QUEUE, true); // true:持久化队列，重启 RabbitMQ 后仍然存在
    }

    @Bean
    public DirectExchange noteExchange() {
        return new DirectExchange(NOTE_EXCHANGE, true, false);
    }

    @Bean
    public Binding noteBinding() {
        return BindingBuilder // BindingBuilder:绑定器，负责把队列和交换机绑定起来
                .bind(noteQueue())
                .to(noteExchange())
                .with(NOTE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() { // MessageConverter:消息转换器，负责把 Java 对象转换为 JSON 字符串，或者把 JSON 字符串转换为 Java 对象
        return new JacksonJsonMessageConverter(); // JacksonJsonMessageConverter:使用 Jackson 库实现的 JSON 转换器
    }
}