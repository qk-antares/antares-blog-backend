package org.example.antares.common.utils;

import lombok.Data;
import org.example.antares.common.constant.CommonConstant;

/**
 * 分页请求
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int pageNum = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_DESC;
}
