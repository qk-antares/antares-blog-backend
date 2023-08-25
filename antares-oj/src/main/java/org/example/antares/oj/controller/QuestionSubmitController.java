package org.example.antares.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.oj.feign.UserFeignService;
import org.example.antares.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import org.example.antares.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import org.example.antares.oj.model.entity.QuestionSubmit;
import org.example.antares.oj.model.vo.QuestionSubmitVo;
import org.example.antares.oj.service.QuestionSubmitService;
import org.example.antares.oj.utils.UserUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author Antares
 * @date 2023/8/24 17:07
 * @description 题目提交接口
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
@Validated
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;
    @Resource
    private UserFeignService userFeignService;

    /**
     * 提交题目
     * @param questionSubmitAddRequest
     * @return 提交记录的 id
     */
    @PostMapping
    public R<Long> doQuestionSubmit(@RequestBody @NotNull @Valid QuestionSubmitAddRequest questionSubmitAddRequest) {
        // 登录才能点赞
        final UserInfoVo currentUser = UserUtils.getCurrentUser(userFeignService.getCurrentUser());
        Long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, currentUser);
        return R.ok(questionSubmitId);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     * @param questionSubmitQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public R<Page<QuestionSubmitVo>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        long pageNum = questionSubmitQueryRequest.getPageNum();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(pageNum, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final UserInfoVo currentUser = UserUtils.getCurrentUser(userFeignService.getCurrentUser());
        // 返回脱敏信息
        Page<QuestionSubmitVo> page = questionSubmitService.getQuestionSubmitVoPage(questionSubmitPage, currentUser);
        return R.ok(page);
    }


}
