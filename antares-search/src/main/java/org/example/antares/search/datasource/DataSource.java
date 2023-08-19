package org.example.antares.search.datasource;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 数据源接口（新接入的数据源必须实现）
 */
public interface DataSource<T> {
    /**
     * 搜索
     * @param searchText
     * @param pageNum
     * @param pageSize
     * @return
     */
    Page<T> doSearch(String searchText, int pageNum, int pageSize, List<String> tags);
}
