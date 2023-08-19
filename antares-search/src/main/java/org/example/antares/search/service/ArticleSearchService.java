package org.example.antares.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.search.model.dto.article.ArticleQueryRequest;
import org.example.antares.search.model.vo.ArticleVo;

public interface ArticleSearchService {
    /**
     * 从 ES 查询
     * @param articleQueryRequest
     * @return
     */
    Page<ArticleVo> searchFromEs(ArticleQueryRequest articleQueryRequest);
}
