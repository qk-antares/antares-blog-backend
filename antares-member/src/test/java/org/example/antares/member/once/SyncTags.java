package org.example.antares.member.once;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.antares.common.model.vo.UserTagVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.example.antares.member.mapper.UserTagCategoryMapper;
import org.example.antares.member.mapper.UserTagMapper;
import org.example.antares.member.model.entity.UserTag;
import org.example.antares.member.model.entity.UserTagCategory;
import org.example.antares.member.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.antares.common.constant.RedisConstants.USER_TAGS_CATEGORY;
import static org.example.antares.common.constant.RedisConstants.USER_TAGS_PREFIX;

@SpringBootTest
public class SyncTags {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserTagMapper userTagMapper;
    @Resource
    private UserTagCategoryMapper userTagCategoryMapper;
    @Resource
    private RedisUtils redisUtils;

    @Test
    void sync(){
        //首先查询所有的类别
        List<UserTagCategory> userTagCategories = userTagCategoryMapper.selectList(null);
        if(CollectionUtils.isNotEmpty(userTagCategories)){
            stringRedisTemplate.delete(USER_TAGS_CATEGORY);
            redisUtils.rightPushAllAsString(USER_TAGS_CATEGORY, userTagCategories);
        }

        //根据类别查询标签
        userTagCategories.stream().forEach(userTagCategory -> {
            LambdaQueryWrapper<UserTag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserTag::getParentId, userTagCategory.getId());
            List<UserTagVo> userTags = userTagMapper.selectList(wrapper).stream()
                    .map(userTag -> BeanCopyUtils.copyBean(userTag, UserTagVo.class))
                    .collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(userTags)){
                stringRedisTemplate.delete(USER_TAGS_PREFIX + userTagCategory.getId());
                redisUtils.rightPushAllAsString(USER_TAGS_PREFIX + userTagCategory.getId(), userTags);
            }
        });
    }
}
