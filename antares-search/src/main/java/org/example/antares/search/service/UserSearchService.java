package org.example.antares.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.search.model.dto.article.ArticleQueryRequest;
import org.example.antares.search.model.dto.user.UserQueryRequest;
import org.example.antares.search.model.vo.ArticleVo;

public interface UserSearchService {
    /**
     * 从 ES 查询
     * @param userQueryRequest
     * @return
     */
    Page<UserInfoVo> searchFromEs(UserQueryRequest userQueryRequest);
}