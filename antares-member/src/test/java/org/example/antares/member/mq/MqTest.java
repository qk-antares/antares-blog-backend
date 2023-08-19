package org.example.antares.member.mq;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class MqTest {
    @Resource
    private AmqpAdmin amqpAdmin;


}
