package org.example.antares.member.model.dto.chat;

import lombok.Data;
import org.example.antares.common.utils.PageRequest;

@Data
public class MessageQueryRequest extends PageRequest {
    private Long conversationId;
}
