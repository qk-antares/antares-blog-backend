package org.example.antares.blog.controller;

import org.example.antares.blog.annotation.LoginRequired;
import org.example.antares.blog.service.ArticleStarService;
import org.example.antares.common.model.response.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog")
@Validated
public class ArticleStarController {
    @Resource
    private ArticleStarService articleStarService;

    /**
     * 收藏文章（当传来的bookIds为空时代表取消收藏）
     * @param id
     * @param bookIds
     * @param request
     * @return
     */
    @PostMapping("/article/{id}/star")
    public R<Integer> starBlog(@PathVariable("id") Long id,
                      @RequestBody List<Long> bookIds,
                      HttpServletRequest request){
        Integer result = articleStarService.starBlog(id, bookIds, request);
        return R.ok(result);
    }
}
