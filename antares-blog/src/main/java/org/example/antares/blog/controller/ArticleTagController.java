package org.example.antares.blog.controller;

import org.example.antares.blog.model.dto.tag.ArticleTagAddRequest;
import org.example.antares.blog.service.ArticleTagService;
import org.example.antares.common.model.response.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/blog/article/tags")
public class ArticleTagController {
    @Resource
    private ArticleTagService articleTagService;

    /**
     * 获取所有的文章标签
     * @return
     */
    @GetMapping
    public R getAllTags(){
        return articleTagService.getAllTags();
    }

    /**
     * 添加一个标签
     * @param articleTagAddRequest
     * @param request
     * @return
     */
    @PutMapping
    public R addATag(@RequestBody ArticleTagAddRequest articleTagAddRequest, HttpServletRequest request){
        return articleTagService.addATag(articleTagAddRequest, request);
    }
}
