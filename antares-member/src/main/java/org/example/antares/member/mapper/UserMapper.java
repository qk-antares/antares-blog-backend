package org.example.antares.member.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.antares.member.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author Antares
* @description 针对表【user】的数据库操作Mapper
* @createDate 2023-03-05 22:04:15
* @Entity org.example.antares.member.domain.entity.User
*/
public interface UserMapper extends BaseMapper<User> {

    List<User> getRandomRecommend(@Param("uid") Long uid, @Param("count") int count);

    List<User> getRandom(@Param("count") int count);
}




