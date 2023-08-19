package org.example.antares.blog;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.UUID;

@SpringBootTest
public class RabbitTest {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Test
    void test(){
        // 发送点赞消息到队列，最后一个参数是消息的唯一ID
        rabbitTemplate.convertAndSend("exchange.direct", "like",
                new Long[]{1L, 2L},
                new CorrelationData(UUID.randomUUID().toString()));

        System.out.println("heello");
    }
}
