package org.example.antares.member.model.vo.tag;

import lombok.Data;
import org.example.antares.member.model.entity.UserTag;

import java.util.List;

@Data
public class UserTagCategoryVo {
    private Long id;

    private String name;

    private List<UserTag> tags;
}
