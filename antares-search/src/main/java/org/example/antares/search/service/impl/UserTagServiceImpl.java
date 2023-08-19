package org.example.antares.search.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.search.model.entity.UserTag;
import org.example.antares.search.service.UserTagService;
import org.example.antares.search.mapper.UserTagMapper;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【user_tag】的数据库操作Service实现
* @createDate 2023-05-21 13:36:25
*/
@Service
public class UserTagServiceImpl extends ServiceImpl<UserTagMapper, UserTag>
    implements UserTagService{

}




