package org.example.antares.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.common.model.vo.UserTagVo;
import org.example.antares.member.model.dto.tag.UserTagAddRequest;
import org.example.antares.member.model.entity.UserTag;
import org.example.antares.member.model.vo.tag.UserTagCategoryVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【user_tag】的数据库操作Service
* @createDate 2023-03-05 22:05:53
*/
public interface UserTagService extends IService<UserTag> {

    List<UserTagCategoryVo> getAllTags();

    UserTagVo addATag(UserTagAddRequest userTagAddRequest, HttpServletRequest request);

    public List<UserTagVo> idsToTags(String idsJSON);
    public List<UserTagVo> idsToTags(List<Long> tagIds);
}
