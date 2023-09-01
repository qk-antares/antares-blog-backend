package com.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.antares.blog.model.dto.comment.PostCommentRequest;
import com.antares.blog.model.entity.ArticleComment;
import com.antares.blog.model.vo.comment.ChildrenCommentVo;
import com.antares.blog.model.vo.comment.RootCommentVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**R
* @description 针对表【article_comment】的数据库操作Service
* @createDate 2023-04-20 21:31:59
*/
public interface ArticleCommentService extends IService<ArticleComment> {
    void publishComment(PostCommentRequest postCommentRequest, HttpServletRequest request);

    List<RootCommentVo> getRootCommentsOfArticle(Long id);

    void likeComment(Long id);

    List<ChildrenCommentVo> getChildrenOfRoot(Long id);
}
