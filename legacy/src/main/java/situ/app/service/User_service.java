package situ.app.service;

import situ.app.dto.ResultData;
import situ.app.pojo.User;

import javax.servlet.http.HttpSession;

public interface User_service extends  BaseService<User>{
    ResultData login(User u, HttpSession session);

    ResultData updatepass(String pass, String pass1, String pass2);
}
