package org.example.antares.search.esdao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.antares.search.model.dto.article.ArticleEsDTO;
import org.example.antares.search.model.dto.article.ArticleQueryRequest;
import org.example.antares.search.model.vo.ArticleVo;
import org.example.antares.search.service.ArticleSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.*;

/**
 * 帖子 ES 操作测试
 */
@SpringBootTest
public class PostEsDaoTest {

    @Resource
    private ArticleEsDao articleEsDao;

    @Resource
    private ArticleSearchService articleSearchService;

    @Test
    void test() {
        ArticleQueryRequest articleQueryRequest = new ArticleQueryRequest();
        articleQueryRequest.setKeyword("Java");
        articleQueryRequest.setPageNum(1);
        Page<ArticleVo> page =
                articleSearchService.searchFromEs(articleQueryRequest);
        System.out.println(page.getRecords());
    }

    @Test
    void testSelect() {
//        System.out.println(postEsDao.count());
//        Page<PostEsDTO> PostPage = postEsDao.findAll(
//                PageRequest.of(0, 5, Sort.by("createTime")));
//        List<PostEsDTO> postList = PostPage.getContent();
//        Optional<PostEsDTO> byId = postEsDao.findById(1L);
//        System.out.println(byId);
//        System.out.println(postList);
    }

    @Test
    void testAdd() {
        ArticleEsDTO articleEsDTO = new ArticleEsDTO();
        articleEsDTO.setId(123456L);
        articleEsDTO.setTitle("鱼皮是小黑子");
        articleEsDTO.setSummary("鱼皮的知识星球：https://yupi.icu，直播带大家做项目");
        articleEsDTO.setContent("鱼皮的知识星球：https://yupi.icu，直播带大家做项目");
        ArrayList<String> tags = new ArrayList<>();
        tags.add("java");
        tags.add("python");
        articleEsDTO.setTags(tags);
        articleEsDTO.setCreatedBy("antares");
        articleEsDTO.setCreateTime(new Date());
        articleEsDTO.setUpdateTime(new Date());

        articleEsDao.save(articleEsDTO);
    }

    @Test
    void testDelete(){
        articleEsDao.deleteById(123456L);
    }

    @Test
    void testFindById() {
        Optional<ArticleEsDTO> postEsDTO = articleEsDao.findById(123456L);
        System.out.println(postEsDTO);
    }

    @Test
    void testCount() {
        System.out.println(articleEsDao.count());
    }

    @Test
    void testFindByCreatedBy() {
        List<ArticleEsDTO> postEsDaoTestList = articleEsDao.findByCreatedBy(1L);
        System.out.println(postEsDaoTestList);
    }

    @Test
    void testFindByTitle() {
        List<ArticleEsDTO> articleEsDTOS = articleEsDao.findByTitle("鱼狗");
        System.out.println(articleEsDTOS);
    }
}
