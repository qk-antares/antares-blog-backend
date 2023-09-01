package com.antares.blog.model.dto.comment;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PostCommentRequest {
    @NotBlank
    private String content;

    private Long articleId;
    private Long rootId;

    //以下的属性只有子回复有
    private Long toUid;
    private Long toCommentId;
}
