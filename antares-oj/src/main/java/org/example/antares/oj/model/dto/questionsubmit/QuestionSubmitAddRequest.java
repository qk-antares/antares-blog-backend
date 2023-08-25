package org.example.antares.oj.model.dto.questionsubmit;

import lombok.Data;

import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 创建请求
 */
@Data
public class QuestionSubmitAddRequest implements Serializable {

    /**
     * 编程语言
     */
    private String language;

    /**
     * 用户代码
     */
    private String code;

    /**
     * 题目 id
     */
    @Min(1)
    private Long questionId;

    private static final long serialVersionUID = 1L;
}