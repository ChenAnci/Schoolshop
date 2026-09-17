package situ.app.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import situ.app.config.PowerCheck;
import situ.app.dto.ResultData;
import situ.app.dto.ResultInfo;
import situ.app.dto.SearchInfo;
import situ.app.pojo.User;
import situ.app.service.User_service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@RestController
@RequestMapping("/User")
public class User_controller extends  BaseController<User>{
    @Autowired
    User_service service;

    @Override
//    @PowerCheck({0})
    public ResultInfo select(SearchInfo sea) throws Exception {
        return super.select(sea);
    }

    @RequestMapping("updatepass")
    public ResultData updatepass(String pass,String pass1,String pass2){
        return service.updatepass(pass,pass1,pass2);
    }

    @RequestMapping("self")
    public Object self(HttpSession session){
        return session.getAttribute("user");
    }

    @PostMapping("login")
    public ResultData login(User u, HttpSession session){
        return service.login(u,session);
    }

    @RequestMapping("outlogin")
    public void outlogin(HttpSession session,HttpServletResponse resp)throws  Exception{
        session.removeAttribute("user");
        resp.sendRedirect("/login.html");
    }


}
