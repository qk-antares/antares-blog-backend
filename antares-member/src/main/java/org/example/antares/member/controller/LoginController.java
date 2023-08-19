package org.example.antares.member.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.constant.RedisConstants;
import org.example.antares.common.model.response.R;
import org.example.antares.common.utils.HttpUtils;
import org.example.antares.member.model.dto.user.AccountLoginRequest;
import org.example.antares.member.model.dto.user.PhoneLoginRequest;
import org.example.antares.member.model.dto.user.UserRegisterRequest;
import org.example.antares.member.service.UserService;
import org.example.antares.member.utils.RedisUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

import static org.example.antares.common.constant.SystemConstants.*;

@Slf4j
@RestController
@RequestMapping("/member")
@Validated
public class LoginController {
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     * @param phone 手机号
     * @return
     */
    @GetMapping(value = "/sms/sendCode")
    public R sendCode(@Pattern(regexp = "^[1]([3-9])[0-9]{9}$", message = "手机号格式不正确") @RequestParam("phone") String phone) {
        //如果有错误回到注册页面
        return userService.sendCode(phone, PHONE_CODE);
    }

    /**
     * 发送邮箱验证码
     * @param email
     * @return
     */
    @GetMapping(value = "/email/sendCode")
    public R sendMail(@Email @RequestParam("email") String email) {
        return userService.sendCode(email, MAIL_CODE);
    }

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping(value = "/register")
    public R register(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        return userService.register(userRegisterRequest);
    }

    /**
     * 用户登录
     * @param accountLoginRequest
     * @param response
     * @return
     */
    @PostMapping(value = "/login")
    public R login(@RequestBody AccountLoginRequest accountLoginRequest, HttpServletResponse response) {
        return userService.login(accountLoginRequest, response);
    }

    /**
     * 短信登录
     * @param phoneLoginRequest
     * @param response
     * @return
     */
    @PostMapping(value = "/loginByPhone")
    public R loginByPhone(@Valid @RequestBody PhoneLoginRequest phoneLoginRequest, HttpServletResponse response) {
        return userService.loginByPhone(phoneLoginRequest, response);
    }


    /**
     * 登出，移除redis中的缓存
     * @param request
     * @return
     */
    @PostMapping(value = "/logout")
    public R logout(HttpServletRequest request) {
        String token = HttpUtils.getToken(request);
        stringRedisTemplate.delete(RedisConstants.USER_SESSION_PREFIX + token);
        return R.ok();
    }
}