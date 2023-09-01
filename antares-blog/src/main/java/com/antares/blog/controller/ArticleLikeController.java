package com.antares.blog.controller;

import com.antares.blog.service.ArticleLikeService;
import com.antares.common.model.response.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/blog")
@Validated
public class ArticleLikeController {
    @Resource
    private ArticleLikeService articleLikeService;

    /**
     * 对文章进行点赞（点赞的消息同时要放进redis里，只计一个数，用户点击查看消息后再到数据库查询）
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/article/{id}/like")
    public R likeBlog(@PathVariable("id") Long id, HttpServletRequest request){
        articleLikeService.likeBlog(id, request);
        return R.ok();
    }
}
