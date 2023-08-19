package org.example.antares.member.controller;

import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserTagVo;
import org.example.antares.member.model.dto.tag.UserTagAddRequest;
import org.example.antares.member.model.vo.tag.UserTagCategoryVo;
import org.example.antares.member.service.UserTagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/member/tags")
public class UserTagController {
    @Resource
    private UserTagService userTagService;

    /**
     * 获取所有标签
     * @return
     */
    @GetMapping
    public R getAllTags(){
        List<UserTagCategoryVo> allTags = userTagService.getAllTags();
        return R.ok(allTags);
    }

    /**
     * 添加一个标签
     * @param userTagAddRequest
     * @param request
     * @return
     */
    @PutMapping
    public R addATag(@RequestBody UserTagAddRequest userTagAddRequest, HttpServletRequest request){
        UserTagVo userTagVo = userTagService.addATag(userTagAddRequest, request);
        return R.ok(userTagVo);
    }
}
