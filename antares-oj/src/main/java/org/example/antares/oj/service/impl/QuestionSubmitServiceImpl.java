package org.example.antares.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.antares.common.constant.CommonConstant;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.SqlUtils;
import org.example.antares.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import org.example.antares.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import org.example.antares.oj.model.entity.QuestionSubmit;
import org.example.antares.oj.model.enums.QuestionSubmitStatusEnum;
import org.example.antares.oj.model.vo.QuestionSubmitVo;
import org.example.antares.oj.service.QuestionSubmitService;
import org.example.antares.oj.mapper.QuestionSubmitMapper;
import org.example.antares.oj.utils.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author Antares
* @description 针对表【question_submit(题目提交)】的数据库操作Service实现
* @createDate 2023-08-24 10:36:35
*/
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService{

    @Override
    public Long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, UserInfoVo currentUser) {

        return null;
    }

    @Override
    public Wrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "question_id", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("is_delete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<QuestionSubmitVo> getQuestionSubmitVoPage(Page<QuestionSubmit> questionSubmitPage, UserInfoVo currentUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVo> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVo> questionSubmitVoList = questionSubmitList.stream()
                .map(questionSubmit -> questionSubmitToVo(questionSubmit, currentUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVoList);
        return questionSubmitVOPage;
    }

    private QuestionSubmitVo questionSubmitToVo(QuestionSubmit questionSubmit, UserInfoVo currentUser) {
        QuestionSubmitVo questionSubmitVo = QuestionSubmitVo.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = currentUser.getUid();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !UserUtils.isAdmin(currentUser)) {
            questionSubmitVo.setCode(null);
        }
        return questionSubmitVo;
    }
}




