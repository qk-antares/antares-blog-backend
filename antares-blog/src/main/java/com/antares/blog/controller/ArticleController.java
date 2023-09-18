package com.antares.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.antares.blog.model.dto.article.ArticleCreateRequest;
import com.antares.blog.model.dto.article.ArticleQueryRequest;
import com.antares.blog.model.entity.Article;
import com.antares.blog.model.vo.article.ArticleContentVo;
import com.antares.blog.model.vo.article.ArticleVo;
import com.antares.blog.service.ArticleService;
import com.antares.common.model.enums.AppHttpCodeEnum;
import com.antares.common.model.response.R;
import com.antares.common.utils.ThrowUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/blog")
@Validated
public class ArticleController {
    @Resource
    private ArticleService articleService;

    /**
     * 创建文章
     * @param articleCreateRequest
     * @param request
     * @return
     */
    @PostMapping("/article")
    public R<Long> createDraft(@Valid @RequestBody ArticleCreateRequest articleCreateRequest,
                         HttpServletRequest request){
        Long articleId = articleService.createDraft(articleCreateRequest, request);
        return R.ok(articleId);
    }

    /**
     * 获取封面信息（除去content的完整信息，因为content占用了文章信息的大部分），
     * 如果请求打到这里，证明cover不在缓存里，该函数被lua脚本调用
     * @param id
     * @param request 这个参数的作用是校验用户是否点赞和收藏了
     * @return
     */
    @GetMapping("/article/{id}/cover")
    public R<ArticleVo> getArticleCoverById(@PathVariable("id") Long id, HttpServletRequest request){
        ArticleVo articleCover = articleService.getArticleCoverById(id, request);
        return R.ok(articleCover);
    }

    /**
     * 获取文章的基本信息，用在配置文章页面（除去content，包含标签但是不包含viewCount等），保证请求打到这里已经请求过redis了
     * @param id
     * @return
     */
    @GetMapping("/article/{id}/basic")
    public R<ArticleVo> getArticleBasicById(@PathVariable("id") Long id) {
        ArticleVo articleBasic = articleService.getArticleBasicById(id);
        return R.ok(articleBasic);
    }

    /**
     * 更新文章的基本信息，用在保存配置页面
     * @param id
     * @param articleCreateRequest
     * @param request
     * @return
     */
    @PutMapping("/article/{id}/basic")
    public R updateBasicById(@PathVariable("id") Long id,
                             @RequestBody ArticleCreateRequest articleCreateRequest,
                             HttpServletRequest request){
        articleService.updateBasicById(id, articleCreateRequest, request);
        return R.ok();
    }

    /**
     * 根据文章id获取文章的内容，同时将内容缓存起来，用在编辑页面和普通的文章浏览界面。（请求到这里已经经过redis）
     * 对于编辑页面不用做校验，因为前端发现返回的createdBy和当前登录用户的id不一致会跳转，只有提交修改和发布才需要做校验
     * @param id
     * @return
     */
    @GetMapping("/article/{id}/content")
    public R<ArticleContentVo> getArticleContentById(@PathVariable("id") Long id){
        ArticleContentVo articleContent = articleService.getArticleContentById(id);
        return R.ok(articleContent);
    }

    /**
     * 修改文章的内容，用在编辑页面
     * @param id
     * @param articleContentVo
     * @param request
     * @return
     */
    @PutMapping("/article/{id}/content")
    public R updateContentById(@PathVariable("id") Long id,
                               @RequestBody ArticleContentVo articleContentVo,
                               HttpServletRequest request){
        articleService.updateContentById(id, articleContentVo.getContent(), request);
        return R.ok();
    }

    /**
     * 发布文章（save+发布）
     * @param id
     * @param articleContentVo
     * @param request
     * @return
     */
    @PutMapping("/article/{id}/publish")
    public R publishArticle(@PathVariable("id") Long id,
                            @RequestBody(required = false) ArticleContentVo articleContentVo,
                            HttpServletRequest request){
        articleService.publishArticle(id, articleContentVo, request);
        return R.ok();
    }

    @DeleteMapping("/article/{id}/remove")
    public R deleteArticle(@PathVariable("id") Long id, HttpServletRequest request){
        articleService.deleteArticle(id, request);
        return R.ok();
    }

    /**
     * 获取某一用户的文章封面信息（分页查询）
     * @param articleQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/user/article")
    public R<Page<ArticleVo>> getArticlesByUid(@RequestBody ArticleQueryRequest articleQueryRequest, HttpServletRequest request){
        // 限制爬虫
        ThrowUtils.throwIf(articleQueryRequest.getPageSize() > 20, AppHttpCodeEnum.PARAMS_ERROR, "分页大小超出限制");
        Page<ArticleVo> articleVoPage = articleService.getArticlesByUid(articleQueryRequest, request);
        return R.ok(articleVoPage);
    }

    /**
     * 分页获取列表（封装类）
     * @param articleQueryRequest
     * @param request 这个参数的作用是校验用户是否点赞和收藏了
     * @return
     */
    @PostMapping("/list/page/vo")
    public R<Page<ArticleVo>> listArticleVoByPage(@RequestBody ArticleQueryRequest articleQueryRequest,
                                 HttpServletRequest request) {
        // 限制爬虫
        ThrowUtils.throwIf(articleQueryRequest.getPageSize() > 20, AppHttpCodeEnum.PARAMS_ERROR, "分页大小超出限制");
        Page<ArticleVo> articleVoPage = articleService.listArticleVoByPage(articleQueryRequest, request);
        return R.ok(articleVoPage);
    }

    /**
     * 获取热门文章
     * @return
     */
    @GetMapping("/article/hot")
    public R<List<Article>> getHot(){
        List<Article> hots = articleService.getHots();
        return R.ok(hots);
    }

    /**
     * 获取全局置顶的文章，他们应该永远缓存在redis
     * @return
     */
    @GetMapping("/article/global/top")
    public R<List<Article>> getGlobalTop(){
        List<Article> tops = articleService.getGlobalTop();
        return R.ok(tops);
    }

    /**
     * 获取所有动态
     * @param request
     * @return
     */
    @RequestMapping("/article/follow/updates")
    public R<Page<ArticleVo>> getAllUpdates(@RequestBody ArticleQueryRequest articleQueryRequest,
                           HttpServletRequest request){
        Page<ArticleVo> updates = articleService.getUpdates(articleQueryRequest, request);
        return R.ok(updates);
    }

    /**
     * 这是一个测试接口，用来对比lua脚本和直接请求后端的性能差异
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/article/{id}")
    public R<ArticleVo> getArticleById(@PathVariable("id") Long id,
                               HttpServletRequest request){
        ArticleVo articleBasic = articleService.getArticleById(id, request);
        return R.ok(articleBasic);
    }
}
