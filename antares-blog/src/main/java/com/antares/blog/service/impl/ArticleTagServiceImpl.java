package com.antares.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.antares.blog.mapper.ArticleTagMapper;
import com.antares.blog.model.dto.tag.ArticleTagAddRequest;
import com.antares.blog.model.entity.ArticleTag;
import com.antares.blog.model.entity.ArticleTagCategory;
import com.antares.blog.model.vo.tag.ArticleTagCategoryVo;
import com.antares.blog.model.vo.tag.ArticleTagVo;
import com.antares.blog.service.ArticleTagService;
import com.antares.blog.utils.RedisUtils;
import com.antares.common.exception.BusinessException;
import com.antares.common.model.enums.AppHttpCodeEnum;
import com.antares.common.model.vo.UserInfoVo;
import com.antares.common.utils.BeanCopyUtils;
import com.antares.common.utils.ObjectMapperUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.antares.common.constant.RedisConstants.ARTICLE_TAGS_CATEGORY;
import static com.antares.common.constant.RedisConstants.ARTICLE_TAGS_PREFIX;
import static com.antares.common.constant.SystemConstants.TAG_COLORS;

/**
* @author Antares
* @description 针对表【article_tag(文章标签表)】的数据库操作Service实现
* @createDate 2023-03-24 20:40:13
*/
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag>
    implements ArticleTagService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public List<ArticleTagCategoryVo> getAllTags() {
        //获取大类（这个信息是常驻Redis的...，后期可以用）
        List<ArticleTagCategory> categories = redisUtils.readList(ARTICLE_TAGS_CATEGORY, ArticleTagCategory.class);

        //获取标签
        return categories.stream().map(category -> {
            ArticleTagCategoryVo vo = BeanCopyUtils.copyBean(category, ArticleTagCategoryVo.class);
            List<ArticleTagVo> tags = redisUtils.readList(ARTICLE_TAGS_PREFIX + vo.getId(), ArticleTagVo.class);
            vo.setTags(tags);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ArticleTagVo addATag(ArticleTagAddRequest articleTagAddRequest, HttpServletRequest request) {
        UserInfoVo currentUser = redisUtils.getCurrentUserWithValidation(request);
        //获取这个分类下的所有标签
        List<ArticleTag> tags = redisUtils.readList(ARTICLE_TAGS_PREFIX + articleTagAddRequest.getParentId(), ArticleTag.class);

        //判断这个标签是否出现过
        for (ArticleTag tag : tags) {
            if(tag.getName().equals(articleTagAddRequest.getName())){
                throw new BusinessException(AppHttpCodeEnum.USER_TAG_EXIST);
            }
        }

        //标签不存在则将其插入到mysql和redis中（随机生成一个颜色）
        ArticleTag articleTag = BeanCopyUtils.copyBean(articleTagAddRequest, ArticleTag.class);
        articleTag.setColor(TAG_COLORS[new Random().nextInt(TAG_COLORS.length)]);
        articleTag.setCreatedBy(currentUser.getUid());
        save(articleTag);

        //重新设置vo的id
        ArticleTagVo articleTagVo = BeanCopyUtils.copyBean(articleTag, ArticleTagVo.class);
        stringRedisTemplate.opsForList().rightPush(ARTICLE_TAGS_PREFIX + articleTagAddRequest.getParentId(), ObjectMapperUtils.writeValueAsString(articleTagVo));
        return articleTagVo;
    }
}