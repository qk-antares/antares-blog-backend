package org.example.antares.member.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.common.model.response.R;
import org.example.antares.common.utils.PageRequest;
import org.example.antares.member.model.vo.chat.ConversationVo;
import org.example.antares.member.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/member/conversation")
public class ConversationController {
    @Resource
    private ConversationService conversationService;

    /**
     * 分页获取列表（封装类）
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public R<Page<ConversationVo>> listConversationVoByPage(@RequestBody PageRequest pageRequest,
                                 HttpServletRequest request) {
        Page<ConversationVo> page = conversationService.listConversationVoByPage(pageRequest, request);
        return R.ok(page);
    }

    /**
     * 根据目标用户id获取conversationVo，如果没有就返回一个id为-1的
     * @param targetUid
     * @param request
     * @return
     */
    @GetMapping("/{targetUid}")
    public R<ConversationVo> getConversationByTargetUid(@PathVariable("targetUid") Long targetUid,
                                      HttpServletRequest request) {
        ConversationVo vo = conversationService.getConversationByTargetUid(targetUid, request);
        return R.ok(vo);
    }
}
