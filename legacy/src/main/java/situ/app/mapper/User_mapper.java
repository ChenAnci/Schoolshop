package situ.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import situ.app.pojo.User;

import javax.naming.Name;
import java.util.List;

@Mapper
public interface User_mapper extends BaseMapper<User> {

//    @Select("select * from user ${where}")
//    public List<User> select(@Param("where") String where);
//
//    @Insert("insert into user(name) values(#{name})")
//    public void insert(User u);
//
    @Delete("delete from user where id=#{id}")
    public void deleteall(int id);
}
