package org.example.antares.member.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.example.antares.member.job.NettyStarter;
import org.example.antares.member.model.dto.chat.Command;
import org.example.antares.member.model.dto.chat.Response;
import org.example.antares.member.model.entity.ChatMessage;
import org.example.antares.member.model.enums.MessageType;
import org.example.antares.member.model.enums.WebSocketCodeEnum;
import org.example.antares.member.model.vo.chat.MessageVo;
import org.example.antares.member.service.ChatMessageService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class ChatHandler {
    @Resource
    private ChatMessageService chatMessageService;

    public void execute(ChannelHandlerContext ctx, Command command){
        try {
            ChatMessage chatMessage = command.getChatMessage();
            Long uid = chatMessage.getToUid();
            switch (MessageType.match(chatMessage.getType())){
                case PRIVATE:
                    MutablePair<Channel, Long> pair = NettyStarter.USERS.get(uid);
                    //对方不在线，只需要保存消息就好了
                    if(pair == null){
                        chatMessageService.saveMessage(chatMessage);
                    } else {
                        Channel channel = pair.getLeft();
                        //如果是新建的conversation，先保存以获取conversationId，之后再发送消息
                        if(chatMessage.getConversationId().equals(-1L)){
                            Long conversationId = chatMessageService.saveMessage(chatMessage);
                            chatMessage.setConversationId(conversationId);

                            if(channel != null && channel.isActive()){
                                //将message转成vo后发送
                                MessageVo vo = chatMessageService.messageToMessageVo(chatMessage);
                                channel.writeAndFlush(Response.ok(vo));
                            }
                        }
                        //否则先发送后保存
                        else {
                            if(channel != null && channel.isActive()){
                                //将message转成vo后发送
                                MessageVo vo = chatMessageService.messageToMessageVo(chatMessage);
                                channel.writeAndFlush(Response.ok(vo));
                            }
                            chatMessageService.saveMessage(chatMessage);
                        }
                    }
                    break;
                case GROUP:
                    break;
                default:
                    ctx.channel().writeAndFlush(Response.error(WebSocketCodeEnum.NOT_EXIST));
                    break;
            }
        } catch (Exception e) {
            ctx.channel().writeAndFlush(Response.error(WebSocketCodeEnum.INTERNAL_SERVER_ERROR));
        }
    }
}
