package org.example.antares.blog.controller;

import org.example.antares.blog.model.vo.article.ArticleVo;
import org.example.antares.blog.service.ArticleService;
import org.example.antares.common.model.response.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog")
@Validated
public class ArticleFeignController {
    @Resource
    private ArticleService articleService;

    /**
     * 根据Id列表查询对应的文章，只用于远程调用
     * @param articleIds
     * @return
     */
    @PostMapping("/article/list")
    public List<ArticleVo> getArticlesByIds(@RequestBody List<Long> articleIds, HttpServletRequest request){
        return articleService.getArticlesByIds(articleIds, request);
    }
}
