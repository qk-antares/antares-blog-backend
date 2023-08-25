package org.example.antares.oj.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.antares.common.constant.CommonConstant;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.SqlUtils;
import org.example.antares.oj.feign.UserFeignService;
import org.example.antares.oj.model.dto.question.QuestionQueryRequest;
import org.example.antares.oj.model.entity.Question;
import org.example.antares.oj.model.vo.QuestionVo;
import org.example.antares.oj.service.QuestionService;
import org.example.antares.oj.mapper.QuestionMapper;
import org.example.antares.oj.utils.UserUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2023-08-24 10:36:35
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService {
    @Resource
    private UserFeignService userFeignService;

    @Override
    public QuestionVo getQuestionVo(Question question) {
        QuestionVo questionVO = QuestionVo.objToVo(question);
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        UserInfoVo userInfoVo = UserUtils.getCurrentUser(userFeignService.info(userId));
        questionVO.setUserInfoVo(userInfoVo);
        return questionVO;
    }

    @Override
    public Wrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<QuestionVo> getQuestionVoPage(Page<Question> questionPage) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVo> questionVoPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVoPage;
        }
        // 1. 关联查询用户信息
//        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
//        Map<Long, List<UserInfoVo>> userIdUserListMap = userService.listByIds(userIdSet).stream()
//                .collect(Collectors.groupingBy(User::getId));
//        // 填充信息
//        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
//            QuestionVO questionVO = QuestionVO.objToVo(question);
//            Long userId = question.getUserId();
//            User user = null;
//            if (userIdUserListMap.containsKey(userId)) {
//                user = userIdUserListMap.get(userId).get(0);
//            }
//            questionVO.setUserVO(userService.getUserVO(user));
//            return questionVO;
//        }).collect(Collectors.toList());
//        questionVOPage.setRecords(questionVOList);
        return questionVoPage;
    }
}




