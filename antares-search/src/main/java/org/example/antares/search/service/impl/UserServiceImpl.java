package org.example.antares.search.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.search.model.entity.User;
import org.example.antares.search.service.UserService;
import org.example.antares.search.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【user】的数据库操作Service实现
* @createDate 2023-05-21 13:36:25
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




