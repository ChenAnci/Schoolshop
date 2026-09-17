package situ.app.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import situ.app.dto.ResultData;
import situ.app.mapper.User_mapper;
import situ.app.pojo.User;
import situ.app.service.User_service;
import situ.app.utils.MD5Utils;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

@Service
public class User_serviceImpl extends  BaseServiceImpl<User> implements User_service {
@Autowired
    User_mapper mapper;

    @Override
    public void insert(User u) {
        u.setCreatedate(new Date());
        u.setPass(MD5Utils.encode("123"));
        super.insert(u);
    }

    @Override
    public ResultData login(User u, HttpSession session) {
       List<User> list= mapper.selectList(new QueryWrapper<User>().eq("name",u.getName()));
       if(list.size()!=1) return new ResultData(-1,"用户名异常");
       User n=list.get(0);
       if(n.getIsdel()==1) return new ResultData(-2,"无效的用户名");
       if(n.getStatus()==2) return new ResultData(-3,"该用户受限");
       u.setPass(MD5Utils.encode(u.getPass()));
       if(!u.getPass().equals(n.getPass())) return new ResultData(-4,"用户账户或密码错误");
       session.setMaxInactiveInterval(20*60);
       session.setAttribute("user",n);
       return new ResultData(1);
    }

    @Override
    public ResultData updatepass(String pass, String pass1, String pass2) {
        if(!pass1.equals(pass2)) return new ResultData(-1,"新密码错误");
        pass=MD5Utils.encode(pass);
        if(!current.getPass().equals(pass))return new ResultData(-2,"旧密码错误");
        current.setPass(MD5Utils.encode(pass1));
        mapper.update(null,new UpdateWrapper<User>().eq("id",current.getId()).set("pass",current.getPass()));
        return new ResultData(1,"密码修改成果");
    }
}
