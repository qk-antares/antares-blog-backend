package org.example.antares.member.once;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.example.antares.common.utils.CrawlerUtils;
import org.example.antares.common.utils.HttpUtils;
import org.example.antares.member.mapper.UserTagMapper;
import org.example.antares.member.model.entity.User;
import org.example.antares.member.model.entity.UserTag;
import org.example.antares.member.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootTest
public class UpdateUserInfoTest {
    @Resource
    private UserService userService;
    @Resource
    private UserTagMapper userTagMapper;

    /**
     * 利用爬虫批量更新用户的头像，uid从12到1011（1000个）
     */
    @Test
    void updateAvatar() throws IOException {
        ArrayList<User> users = new ArrayList<>();
        String[] keywords = {"二次元头像", "卡通头像", "搞怪头像",
                "动漫头像", "动漫女头", "卡哇伊头像",
                "甜美头像", "风景头像", "大叔头像",
                "呆萌头像"};

        int i = 0;
        int keywordIndex = 0;
        while (i < 1000){
            keywordIndex = i / 100;
            List<String> pictures = CrawlerUtils.fetchPicturesByKeyword(keywords[keywordIndex], (i + 1)-100*keywordIndex, "medium");
            int size = pictures.size();
            for (int j = 0; j < size && i < 1000; j++,i++) {
                User user = new User();
                user.setUid(12L + i);
                user.setAvatar(pictures.get(j));
                users.add(user);
            }
        }

        userService.updateBatchById(users);
    }

    @Test
    void updateUserSignature(){
        String url = "https://v1.alapi.cn/api/mingyan?typeid" + RandomUtils.nextInt(1, 46);
        String result = HttpRequest
                .get(url)
                .execute()
                .body();
        // 2. json 转对象
        Map<String, Object> map = JSONUtil.toBean(result, Map.class);
        JSONObject data = (JSONObject) map.get("data");
        String content = (String) data.get("content");


        //List<User> userList = userService.lambdaQuery().select(User::getUid).list();
        //for (User user : userList) {
        //    try {
        //
        //    } catch (Exception e) {
        //
        //    }
        //}
        //userService.updateBatchById(userList);
    }

    public String removePTags(String input) {
        // 定义正则表达式匹配模式
        String pattern = "^<p>(.*)</p>$";

        // 编译正则表达式
        Pattern regex = Pattern.compile(pattern);

        // 匹配输入字符串
        Matcher matcher = regex.matcher(input);

        // 检查是否匹配成功
        if (matcher.matches()) {
            // 获取匹配结果中的捕获组内容（即<p>标签中的内容）
            String content = matcher.group(1);
            return content;
        } else {
            // 如果不匹配，则返回原始字符串
            return input;
        }
    }

    @Test
    public void changeTags(){
        List<UserTag> userTags = userTagMapper.selectList(null);
        List<User> users = userService.lambdaQuery().select(User::getUid).list();
        int len = userTags.size();

        long start = System.currentTimeMillis();
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (User user : users) {
            //代表该用户的标签数
            int n = RandomUtils.nextInt(0, 11);

            if(n > 0){
                //打乱userTags
                for (int k = 0; k < len; k++) {
                    int index = RandomUtils.nextInt(0,len);
                    UserTag tmp = userTags.get(index);
                    userTags.set(index, userTags.get(k));
                    userTags.set(k, tmp);
                }
                user.setTags(JSON.toJSONString(userTags.subList(0, n)
                        .stream().map(UserTag::getId).collect(Collectors.toList())));
            }
        }

        userService.updateBatchById(users);

        long end = System.currentTimeMillis();
        System.out.println("耗时" + (end-start));
    }
}
