package org.example.antares.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.blog.model.dto.star.StarBookQueryRequest;
import org.example.antares.blog.model.vo.article.ArticleStarVo;
import org.example.antares.blog.model.vo.article.ArticleVo;
import org.example.antares.blog.model.vo.star.StarBookBoolVo;
import org.example.antares.blog.service.StarBookService;
import org.example.antares.common.model.response.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/blog/starBook")
@Validated
public class StarBookController {
    @Resource
    private StarBookService starBookService;

    /**
     * 获取当前用户的所有收藏夹
     * @param request
     * @return
     */
    @GetMapping("/{articleId}")
    public R getStarBooks(@PathVariable("articleId")Long articleId, HttpServletRequest request){
        List<StarBookBoolVo> vos = starBookService.getStarBooks(articleId, request);
        return R.ok(vos);
    }

    /**
     * 获取某用户的所有收藏夹
     * @return
     */
    @GetMapping("/of/{uid}")
    public R getStarBooksByUid(@PathVariable("uid")Long uid){
        List<StarBookBoolVo> vos = starBookService.getStarBooksByUid(uid);
        return R.ok(vos);
    }

    /**
     * 创建收藏夹
     * @param name
     * @param request
     * @return
     */
    @PostMapping
    public R createStarBook(@RequestParam("name") String name, HttpServletRequest request){
        return starBookService.createStarBook(name, request);
    }

    /**
     * 获取某收藏夹下的文章
     * @param starBookQueryRequest
     * @return
     */
    @PostMapping("/articles")
    public R getArticlesInStarBook(@RequestBody StarBookQueryRequest starBookQueryRequest){
        Page<ArticleVo> vos = starBookService.getArticlesInStarBook(starBookQueryRequest);
        return R.ok(vos);
    }
}
