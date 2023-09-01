package com.antares.blog.aspect;

import com.antares.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Component
@Aspect
@Slf4j
public class LoginAspect {
    @Resource
    private HttpServletRequest request;
    @Resource
    private RedisUtils redisUtils;

    @Pointcut("@annotation(com.antares.blog.annotation.LoginRequired)")
    public void requiresLogin(){}

    @Before("requiresLogin()")
    public void before(JoinPoint joinPoint) throws Throwable {
        redisUtils.getCurrentUserWithValidation(request);
    }
}
