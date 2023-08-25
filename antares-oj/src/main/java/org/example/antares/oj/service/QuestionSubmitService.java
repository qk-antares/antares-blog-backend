package org.example.antares.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import org.example.antares.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import org.example.antares.oj.model.entity.QuestionSubmit;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.oj.model.vo.QuestionSubmitVo;

/**
* @author Antares
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2023-08-24 10:36:35
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {

    Long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, UserInfoVo currentUser);

    Wrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);

    Page<QuestionSubmitVo> getQuestionSubmitVoPage(Page<QuestionSubmit> questionSubmitPage, UserInfoVo currentUser);
}
