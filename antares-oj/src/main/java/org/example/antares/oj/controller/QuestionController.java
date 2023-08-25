package org.example.antares.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.ThrowUtils;
import org.example.antares.oj.annotation.AuthCheck;
import org.example.antares.oj.constant.UserConstant;
import org.example.antares.oj.feign.UserFeignService;
import org.example.antares.oj.model.dto.question.*;
import org.example.antares.oj.model.entity.Question;
import org.example.antares.oj.model.vo.QuestionVo;
import org.example.antares.oj.service.QuestionService;
import org.example.antares.oj.utils.UserUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Antares
 * @date 2023/8/25 9:27
 * @description 题目接口
 */
@RestController
@RequestMapping("/question")
@Slf4j
@Validated
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignService userFeignService;

    /**
     * 创建
     * @param questionAddRequest
     * @return
     */
    @PostMapping
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public R<Long> addQuestion(@RequestBody @NotNull @Valid QuestionAddRequest questionAddRequest) {
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        UserInfoVo currentUser = UserUtils.getCurrentUser(userFeignService.getCurrentUser());
        question.setUserId(currentUser.getUid());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, AppHttpCodeEnum.INTERNAL_SERVER_ERROR);
        return R.ok(question.getId());
    }

    /**
     * 更新（仅管理员）
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public R<Boolean> updateQuestion(@RequestBody @NotNull @Valid QuestionUpdateRequest questionUpdateRequest) {
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, AppHttpCodeEnum.NOT_EXIST);
        boolean result = questionService.updateById(question);
        return R.ok(result);
    }


    /**
     * 删除
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public R<Boolean> deleteQuestion(@PathVariable("id") @Min(1) Long id) {
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, AppHttpCodeEnum.NOT_EXIST);
        boolean result = questionService.removeById(id);
        return R.ok(result);
    }

    /**
     * 根据 id 获取
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/{id}")
    public R<Question> getQuestionById(@PathVariable("id") @Min(1) Long id, HttpServletRequest request) {
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
        }
        UserInfoVo currentUser = UserUtils.getCurrentUser(userFeignService.getCurrentUser());
        // 不是本人或管理员，不能直接获取所有信息
        if (!question.getUserId().equals(currentUser.getUid()) && !UserUtils.isAdmin(currentUser)) {
            throw new BusinessException(AppHttpCodeEnum.NO_AUTH);
        }
        return R.ok(question);
    }

    /**
     * 根据 id 获取（脱敏）
     * @param id
     * @return
     */
    @GetMapping("/{id}/vo")
    public R<QuestionVo> getQuestionVOById(@PathVariable("id") @Min(1) Long id, HttpServletRequest request) {
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(AppHttpCodeEnum.NOT_EXIST);
        }
        QuestionVo vo = questionService.getQuestionVo(question);
        return R.ok(vo);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public R<Page<QuestionVo>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long pageNum = questionQueryRequest.getPageNum();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, AppHttpCodeEnum.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(pageNum, size),
                questionService.getQueryWrapper(questionQueryRequest));
        Page<QuestionVo> page = questionService.getQuestionVoPage(questionPage);
        return R.ok(page);
    }

    /**
     * 分页获取当前用户创建的资源列表
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public R<Page<QuestionVo>> listMyQuestionVOByPage(@RequestBody @NotNull QuestionQueryRequest questionQueryRequest) {
        UserInfoVo currentUser = UserUtils.getCurrentUser(userFeignService.getCurrentUser());
        questionQueryRequest.setUserId(currentUser.getUid());
        long pageNum = questionQueryRequest.getPageNum();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, AppHttpCodeEnum.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(pageNum, size),
                questionService.getQueryWrapper(questionQueryRequest));
        Page<QuestionVo> page = questionService.getQuestionVoPage(questionPage);
        return R.ok(page);
    }

    /**
     * 分页获取题目列表（仅管理员）
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public R<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long pageNum = questionQueryRequest.getPageNum();
        long size = questionQueryRequest.getPageSize();
        Page<Question> page = questionService.page(new Page<>(pageNum, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return R.ok(page);
    }

    // endregion

    /**
     * 编辑（用户）
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public R editQuestion(@RequestBody @NotNull @Valid QuestionEditRequest questionEditRequest,
                                   HttpServletRequest request) {
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);

        // 判断是否存在
        long id = questionEditRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, AppHttpCodeEnum.NOT_EXIST);

        UserInfoVo currentUser = UserUtils.getCurrentUser(userFeignService.getCurrentUser());

        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(currentUser.getUid()) && !UserUtils.isAdmin(currentUser)) {
            throw new BusinessException(AppHttpCodeEnum.NO_AUTH);
        }
        questionService.updateById(question);
        return R.ok();
    }
}
