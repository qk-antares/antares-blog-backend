package org.example.antares.member.model.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.antares.member.model.entity.ChatMessage;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Command {
    private Integer code;
    private Long uid;
    private Long conversationId;
    private ChatMessage chatMessage;
}
