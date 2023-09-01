package com.antares.blog.model.vo.star;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArticleLikeStarVo {
    private Boolean isLiked;

    private Boolean isStared;
}
