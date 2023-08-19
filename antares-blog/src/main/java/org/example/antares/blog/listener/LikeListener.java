package org.example.antares.blog.listener;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.blog.service.ArticleLikeService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;

@RabbitListener(queues = {"antares.article.like"})
@Service
@Slf4j
public class LikeListener {
    @Resource
    private ArticleLikeService articleLikeService;

    /**
     * uid点赞了articleId
     * @param message
     * @param ids
     * @param channel
     */
    @RabbitHandler
    @Transactional
    public void handleClearMessage(Message message, Long[] ids, Channel channel){
        articleLikeService.likeBlog(ids[0], ids[1], ids[2]);

        //手动ack（false代表不批量接收）
        try {
            long deliveryTag = message.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("RabbitMQ连接接异常！", e);
        }
    }
}