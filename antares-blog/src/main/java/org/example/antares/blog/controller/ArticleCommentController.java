package org.example.antares.blog.controller;

import org.example.antares.blog.model.dto.comment.PostCommentRequest;
import org.example.antares.blog.model.vo.comment.ChildrenCommentVo;
import org.example.antares.blog.model.vo.comment.RootCommentVo;
import org.example.antares.blog.service.ArticleCommentService;
import org.example.antares.common.model.response.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/blog/comment")
@Validated
public class ArticleCommentController {
    @Resource
    private ArticleCommentService articleCommentService;

    /**
     * 获取某个文章的根评论
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R getRootCommentsOfArticle(@PathVariable("id") Long id){
        List<RootCommentVo> vos = articleCommentService.getRootCommentsOfArticle(id);
        return R.ok(vos);
    }

    /**
     * 获取某个根评论下的子评论，后期可以优化成分页，就像B站那样
     * @param id
     * @return
     */
    @GetMapping("/children/{id}")
    public R getChildrenOfRoot(@PathVariable("id") Long id){
        List<ChildrenCommentVo> vos = articleCommentService.getChildrenOfRoot(id);
        return R.ok(vos);
    }

    //Todo: 功能待实现
    /**
     * 点赞评论
     * @param id: 评论id
     * @return
     */
    @PostMapping("/like/{id}")
    public R likeComment(@PathVariable("id") Long id){
        return articleCommentService.likeComment(id);
    }

    /**
     * 发表评论
     * @param postCommentRequest
     * @param request
     * @return
     */
    @PostMapping
    public R publishComment(@Valid @RequestBody PostCommentRequest postCommentRequest, HttpServletRequest request){
        return articleCommentService.publishComment(postCommentRequest, request);
    }
}
