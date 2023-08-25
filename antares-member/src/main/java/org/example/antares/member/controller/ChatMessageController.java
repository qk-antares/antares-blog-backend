package org.example.antares.member.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.common.model.response.R;
import org.example.antares.member.model.dto.chat.MessageQueryRequest;
import org.example.antares.member.model.vo.chat.MessageVo;
import org.example.antares.member.service.ChatMessageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/member/message")
public class ChatMessageController {
    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 分页获取列表（封装类）
     * @param messageQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public R<Page<MessageVo>> listMessageVoByPage(@RequestBody MessageQueryRequest messageQueryRequest,
                                      HttpServletRequest request) {
        Page<MessageVo> page = chatMessageService.listMessageVoByPage(messageQueryRequest, request);
        return R.ok(page);
    }
}
