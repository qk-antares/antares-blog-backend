package org.example.antares.search.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Antares
 * @date 2023/5/18 9:49
 * @description 远程调用保留cookie
 */
@Configuration
public class FeignConfig {
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //拿到老的request
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (requestAttributes != null) {
                    //老请求
                    HttpServletRequest request = requestAttributes.getRequest();
                    //2、同步请求头的数据（主要是cookie）
                    //把老请求的cookie值放到新请求上来，进行一个同步
                    String cookie = request.getHeader("Cookie");
                    requestTemplate.header("Cookie", cookie);
                }
            }
        };
    }
}
