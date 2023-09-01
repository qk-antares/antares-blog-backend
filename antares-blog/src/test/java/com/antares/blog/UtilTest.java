package com.antares.blog;

import cn.hutool.core.collection.CollectionUtil;
import com.antares.blog.model.entity.Article;
import org.example.antares.common.utils.BeanCopyUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SpringBootTest
public class UtilTest {
    @Test
    public void testCollectionUtil(){
        Set<Integer> set1 = new HashSet<>();
        set1.addAll(Arrays.asList(1,2,3));

        Set<Integer> set2 = new HashSet<>();
        set2.addAll(Arrays.asList(2,3,4));

        Set <Integer> result = (Set<Integer>) CollectionUtil.addAll(set1, set2);
        System.out.println(result);
    }

    @Test
    void testCopyBean(){
        Article article = new Article();
        article.setId(13340L);
        article.setTitle("1546564");
        article.setSummary("4545456");
        article.setContent("4565654565");

        Article result = new Article();

        BeanUtils.copyProperties(article, result, "title");
        System.out.println(result);
    }

}
