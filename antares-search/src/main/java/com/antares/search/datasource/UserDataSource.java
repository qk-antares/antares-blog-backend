package com.antares.search.datasource;

import com.antares.search.model.dto.user.UserQueryRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.antares.common.model.vo.UserInfoVo;
import com.antares.search.service.UserSearchService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserDataSource implements DataSource<UserInfoVo> {
    @Resource
    private UserSearchService userSearchService;

    @Override
    public Page<UserInfoVo> doSearch(String searchText, int pageNum, int pageSize, List<String> tags) {
        UserQueryRequest userQueryRequest = new UserQueryRequest();
        userQueryRequest.setKeyword(searchText);
        userQueryRequest.setPageNum(pageNum);
        userQueryRequest.setPageSize(pageSize);
        userQueryRequest.setTags(tags);
        //只搜索出对应的id
        return userSearchService.searchFromEs(userQueryRequest);
    }
}
