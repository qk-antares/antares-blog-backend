package org.example.antares.oj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.antares.oj.model.entity.Post;
import org.example.antares.oj.service.PostService;
import org.example.antares.oj.mapper.PostMapper;
import org.springframework.stereotype.Service;

/**
* @author Antares
* @description 针对表【post(帖子)】的数据库操作Service实现
* @createDate 2023-08-24 10:36:35
*/
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
    implements PostService {

}




