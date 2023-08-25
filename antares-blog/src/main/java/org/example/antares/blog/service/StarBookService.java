package org.example.antares.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.antares.blog.model.dto.star.StarBookQueryRequest;
import org.example.antares.blog.model.entity.StarBook;
import org.example.antares.blog.model.vo.article.ArticleStarVo;
import org.example.antares.blog.model.vo.article.ArticleVo;
import org.example.antares.blog.model.vo.star.StarBookBoolVo;
import org.example.antares.common.model.response.R;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Antares
* @description 针对表【star_book】的数据库操作Service
* @createDate 2023-04-20 21:31:59
*/
public interface StarBookService extends IService<StarBook> {

    List<StarBookBoolVo> getStarBooks(Long articleId, HttpServletRequest request);

    Long createStarBook(String name, HttpServletRequest request);

    Page<ArticleVo> getArticlesInStarBook(StarBookQueryRequest starBookQueryRequest);

    List<StarBookBoolVo> getStarBooksByUid(Long uid);
}
