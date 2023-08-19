package org.example.antares.blog.service.impl;

import com.alibaba.nacos.shaded.org.checkerframework.checker.units.qual.A;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.blog.mapper.ArticleStarMapper;
import org.example.antares.blog.model.dto.star.StarBookQueryRequest;
import org.example.antares.blog.model.entity.Article;
import org.example.antares.blog.model.entity.ArticleStar;
import org.example.antares.blog.model.entity.StarBook;
import org.example.antares.blog.model.vo.article.ArticleStarVo;
import org.example.antares.blog.model.vo.article.ArticleVo;
import org.example.antares.blog.model.vo.star.StarBookBoolVo;
import org.example.antares.blog.service.ArticleService;
import org.example.antares.blog.service.StarBookService;
import org.example.antares.blog.mapper.StarBookMapper;
import org.example.antares.blog.utils.RedisUtils;
import org.example.antares.common.constant.CommonConstant;
import org.example.antares.common.constant.SystemConstants;
import org.example.antares.common.model.response.R;
import org.example.antares.common.model.vo.UserInfoVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.example.antares.common.utils.SqlUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
* @author Antares
* @description 针对表【star_book】的数据库操作Service实现
* @createDate 2023-04-20 21:31:59
*/
@Service
@Slf4j
public class StarBookServiceImpl extends ServiceImpl<StarBookMapper, StarBook>
    implements StarBookService{
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ArticleStarMapper articleStarMapper;
    @Resource
    private ArticleService articleService;

    @Override
    public List<StarBookBoolVo> getStarBooks(Long articleId, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        //首先查询出所有的收藏夹
        List<StarBookBoolVo> vos = lambdaQuery()
                .select(StarBook::getId, StarBook::getName, StarBook::getCount)
                .eq(StarBook::getCreateBy, currentUser.getUid()).list()
                .stream().map(starBook -> BeanCopyUtils.copyBean(starBook, StarBookBoolVo.class))
                .collect(Collectors.toList());

        if(articleId.equals(-1L)){
            return vos;
        }

        List<Long> bookIds = vos.stream().map(StarBookBoolVo::getId).collect(Collectors.toList());
        if(bookIds.isEmpty()){
            return new ArrayList<>();
        }
        //查询收藏记录
        Set<Long> containBookIds = articleStarMapper.selectList(new LambdaQueryWrapper<ArticleStar>().select(ArticleStar::getBookId)
                        .eq(ArticleStar::getArticleId, articleId).in(ArticleStar::getBookId, bookIds))
                .stream().map(ArticleStar::getBookId).collect(Collectors.toSet());

        for (StarBookBoolVo vo : vos) {
            if (containBookIds.contains(vo.getId())){
                vo.setIsContain(true);
            }
        }
        return vos;
    }

    @Override
    public R createStarBook(String name, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        StarBook starBook = new StarBook();
        starBook.setName(name);
        starBook.setCreateBy(currentUser.getUid());
        save(starBook);
        return R.ok(starBook.getId());
    }

    @Override
    public Page<ArticleVo> getArticlesInStarBook(StarBookQueryRequest starBookQueryRequest) {
        int pageNum = starBookQueryRequest.getPageNum();
        int pageSize = starBookQueryRequest.getPageSize();
        Long bookId = starBookQueryRequest.getBookId();

        //数据库中的数据除了访问量其他数据都可以确保是最新的
        //1. 构造查询条件
        QueryWrapper<ArticleStar> queryWrapper = new QueryWrapper<ArticleStar>()
                .select("article_id")
                .eq("book_id", bookId)
                .orderBy(true, false, "create_time");

        //2. 查询数据库中的信息
        long start = System.currentTimeMillis();
        Page<ArticleStar> articleLikePage = articleStarMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        long end = System.currentTimeMillis();
        log.info("分页耗时：{}", end - start);

        //3. 转换为vos
        List<Long> articleIds = articleLikePage.getRecords().stream().map(ArticleStar::getArticleId).collect(Collectors.toList());
        List<ArticleVo> vos = articleService.getArticlesByIds(articleIds, null);
        Page<ArticleVo> articleVoPage = new Page<>(pageNum, pageSize, articleLikePage.getTotal());
        articleVoPage.setRecords(vos);
        return articleVoPage;
    }

    @Override
    public List<StarBookBoolVo> getStarBooksByUid(Long uid) {
        return lambdaQuery()
                .select(StarBook::getId, StarBook::getName, StarBook::getCount)
                .eq(StarBook::getCreateBy, uid).list()
                .stream().map(starBook -> BeanCopyUtils.copyBean(starBook, StarBookBoolVo.class))
                .collect(Collectors.toList());
    }
}




