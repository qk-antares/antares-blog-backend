package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.blog.model.dto.comment.PostCommentRequest;
import org.example.antares.blog.model.entity.ArticleComment;
import org.example.antares.blog.model.vo.comment.ChildrenCommentVo;
import org.example.antares.blog.model.vo.comment.RootCommentVo;
import org.example.antares.common.model.response.R;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**R
* @description 针对表【article_comment】的数据库操作Service
* @createDate 2023-04-20 21:31:59
*/
public interface ArticleCommentService extends IService<ArticleComment> {
    R publishComment(PostCommentRequest postCommentRequest, HttpServletRequest request);

    List<RootCommentVo> getRootCommentsOfArticle(Long id);

    R likeComment(Long id);

    List<ChildrenCommentVo> getChildrenOfRoot(Long id);
}
