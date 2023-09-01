package com.antares.search.service;

import com.antares.search.model.dto.user.UserQueryRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.antares.common.model.vo.UserInfoVo;

public interface UserSearchService {
    /**
     * 从 ES 查询
     * @param userQueryRequest
     * @return
     */
    Page<UserInfoVo> searchFromEs(UserQueryRequest userQueryRequest);
}
