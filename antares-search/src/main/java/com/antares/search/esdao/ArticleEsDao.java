package com.antares.search.esdao;

import java.util.List;

import com.antares.search.model.dto.article.ArticleEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 帖子 ES 操作
 */
public interface ArticleEsDao extends ElasticsearchRepository<ArticleEsDTO, Long> {
    List<ArticleEsDTO> findByUsername(String username);

    List<ArticleEsDTO> findByTitle(String title);
}