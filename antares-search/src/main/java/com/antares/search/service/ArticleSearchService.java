package com.antares.search.service;

import com.antares.search.model.dto.article.ArticleQueryRequest;
import com.antares.search.model.vo.ArticleVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ArticleSearchService {
    /**
     * 从 ES 查询
     * @param articleQueryRequest
     * @return
     */
    Page<ArticleVo> searchFromEs(ArticleQueryRequest articleQueryRequest);
}
