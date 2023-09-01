package com.antares.search.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.antares.search.esdao.ArticleEsDao;
import com.antares.search.esdao.UserEsDao;
import com.antares.search.model.dto.article.ArticleEsDTO;
import com.antares.search.model.dto.user.UserEsDTO;
import com.antares.search.model.entity.ArticleTag;
import com.antares.search.model.entity.ArticleTagRelation;
import com.antares.search.model.entity.User;
import com.antares.search.model.entity.UserTag;
import com.antares.search.service.*;
import lombok.extern.slf4j.Slf4j;
import com.antares.common.utils.BeanCopyUtils;
import com.antares.search.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全量同步帖子到 es
 */
@SpringBootTest
@Slf4j
public class FullSyncPostToEs {
    @Resource
    private ArticleService articleService;
    @Resource
    private ArticleTagService articleTagService;
    @Resource
    private ArticleTagRelationService articleTagRelationService;
    @Resource
    private ArticleEsDao articleEsDao;
    @Resource
    private UserEsDao userEsDao;
    @Resource
    private UserService userService;
    @Resource
    private UserTagService userTagService;

    @Test
    public void fullSyncArticle() {
        Map<Long, ArticleTag> tagsMap = articleTagService.list().stream()
                .collect(Collectors.toMap(ArticleTag::getId, articleTag -> articleTag));
        List<ArticleEsDTO> dtos = articleService.list().stream().map(article -> {
            ArticleEsDTO articleEsDTO = BeanCopyUtils.copyBean(article, ArticleEsDTO.class);
            //查询该文章涉及的标签
            List<Long> tagIds = articleTagRelationService.lambdaQuery().select(ArticleTagRelation::getTagId)
                    .eq(ArticleTagRelation::getArticleId, article.getId()).list()
                    .stream().map(ArticleTagRelation::getTagId).collect(Collectors.toList());
            ArrayList<String> tags = new ArrayList<>();
            for (Long tagId : tagIds) {
                tags.add(tagsMap.get(tagId).getName());
            }
            articleEsDTO.setTags(tags);

            //查询文章的作者信息
            User user = userService.lambdaQuery().select(User::getUsername).eq(User::getUid, article.getCreatedBy()).one();
            articleEsDTO.setUsername(user.getUsername());

            return articleEsDTO;
        }).collect(Collectors.toList());

        final int pageSize = 500;
        int total = dtos.size();
        log.info("FullSyncPostToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            articleEsDao.saveAll(dtos.subList(i, end));
        }
        log.info("FullSyncPostToEs end, total {}", total);
    }

    @Test
    public void fullSyncUser() {
        Map<Long, UserTag> tagsMap = userTagService.list().stream()
                .collect(Collectors.toMap(UserTag::getId, userTag -> userTag));
        List<UserEsDTO> dtos = userService.list().stream().map(user -> {
            UserEsDTO userEsDTO = BeanCopyUtils.copyBean(user, UserEsDTO.class);
            List<Long> tagIds = JSON.parseObject(user.getTags(), new TypeReference<List<Long>>(){});
            ArrayList<String> tags = new ArrayList<>();
            for (Long tagId : tagIds) {
                tags.add(tagsMap.get(tagId).getName());
            }
            userEsDTO.setTags(tags);

            return userEsDTO;
        }).collect(Collectors.toList());

        final int pageSize = 500;
        int total = dtos.size();
        log.info("FullSyncPostToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            userEsDao.saveAll(dtos.subList(i, end));
        }
        log.info("FullSyncPostToEs end, total {}", total);
    }

    @Test
    public void updateSignature(){
        List<User> list = userService.lambdaQuery().select(User::getUid, User::getSignature).list();
        List<UserEsDTO> dtos = list.stream().map(user -> BeanCopyUtils.copyBean(user, UserEsDTO.class)).collect(Collectors.toList());

        final int pageSize = 500;
        int total = dtos.size();
        log.info("FullSyncPostToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            userEsDao.saveAll(dtos.subList(i, end));
        }
        log.info("FullSyncPostToEs end, total {}", total);
    }
}
