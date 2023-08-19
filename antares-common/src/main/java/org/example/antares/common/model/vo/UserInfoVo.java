package org.example.antares.common.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVo {
    private Long uid;
    private String username;
    private List<UserTagVo> tags;
    private String signature;
    private String email;
    private String phone;
    private Integer sex;
    private String avatar;
    private Integer follow;
    private Integer fans;
    private Integer topic;

    private Boolean isFollow = false;
}