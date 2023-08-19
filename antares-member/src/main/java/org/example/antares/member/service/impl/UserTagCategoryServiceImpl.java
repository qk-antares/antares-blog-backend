package org.example.antares.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.member.model.entity.UserTagCategory;
import org.example.antares.member.service.UserTagCategoryService;
import org.example.antares.member.mapper.UserTagCategoryMapper;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【user_tag_category】的数据库操作Service实现
* @createDate 2023-03-05 22:01:36
*/
@Service
public class UserTagCategoryServiceImpl extends ServiceImpl<UserTagCategoryMapper, UserTagCategory>
    implements UserTagCategoryService{

}




