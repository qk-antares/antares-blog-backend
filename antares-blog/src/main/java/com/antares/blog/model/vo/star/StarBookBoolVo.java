package com.antares.blog.model.vo.star;

import lombok.Data;

@Data
public class StarBookBoolVo {
    private Long id;
    private String name;
    private Integer count;
    private Boolean isContain=false;
}
