package org.example.antares.member.job;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.model.vo.UserTagVo;
import org.example.antares.common.utils.BeanCopyUtils;
import org.example.antares.member.utils.RedisUtils;
import org.example.antares.member.model.entity.UserTag;
import org.example.antares.member.model.entity.UserTagCategory;
import org.example.antares.member.mapper.UserTagCategoryMapper;
import org.example.antares.member.mapper.UserTagMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.antares.common.constant.RedisConstants.*;

//@Component
@Slf4j
public class StartRunner implements CommandLineRunner {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserTagMapper userTagMapper;
    @Resource
    private UserTagCategoryMapper userTagCategoryMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public void run(String... args) {
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