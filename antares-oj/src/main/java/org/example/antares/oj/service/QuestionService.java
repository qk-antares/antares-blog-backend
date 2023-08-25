package org.example.antares.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.oj.model.dto.question.QuestionQueryRequest;
import org.example.antares.oj.model.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.oj.model.vo.QuestionVo;

import javax.servlet.http.HttpServletRequest;

/**
* @author Antares
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2023-08-24 10:36:35
*/
public interface QuestionService extends IService<Question> {

    QuestionVo getQuestionVo(Question question);

    Wrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    Page<QuestionVo> getQuestionVoPage(Page<Question> questionPage);
}
