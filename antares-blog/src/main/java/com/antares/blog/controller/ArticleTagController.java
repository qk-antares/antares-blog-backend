package com.antares.blog.controller;

import com.antares.blog.model.dto.tag.ArticleTagAddRequest;
import com.antares.blog.model.vo.tag.ArticleTagCategoryVo;
import com.antares.blog.model.vo.tag.ArticleTagVo;
import com.antares.blog.service.ArticleTagService;
import com.antares.common.model.response.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    public R<List<ArticleTagCategoryVo>> getAllTags(){
        List<ArticleTagCategoryVo> allTags = articleTagService.getAllTags();
        return R.ok(allTags);
    }

    /**
     * 添加一个标签
     * @param articleTagAddRequest
     * @param request
     * @return
     */
    @PutMapping
    public R<ArticleTagVo> addATag(@RequestBody ArticleTagAddRequest articleTagAddRequest, HttpServletRequest request){
        ArticleTagVo articleTagVo = articleTagService.addATag(articleTagAddRequest, request);
        return R.ok(articleTagVo);
    }
}
