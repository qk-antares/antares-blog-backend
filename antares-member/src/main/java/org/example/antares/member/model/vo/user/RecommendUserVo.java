package org.example.antares.member.model.vo.user;

import lombok.Data;
import org.example.antares.common.model.vo.UserInfoVo;

@Data
public class RecommendUserVo {
    private UserInfoVo userInfo;
    private Double score;
}
