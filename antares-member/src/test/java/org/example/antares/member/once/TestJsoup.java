package org.example.antares.member.once;

import cn.hutool.http.HttpRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class TestJsoup {
    @Test
    void test() throws IOException {
        //Document document = Jsoup.connect("https://imgs.itxueyuan.com/1608216521885-image.png").get();
        //System.out.println(document);

        String url = "https://imgs.itxueyuan.com/1608216521885-image.png";
        String result = HttpRequest
                .get(url)
                .execute()
                .body();
        System.out.println(result);

    }
}
