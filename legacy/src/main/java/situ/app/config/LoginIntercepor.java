package situ.app.config;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginIntercepor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       // System.out.println(request.getRequestURI());
        if(request.getSession().getAttribute("user")!=null){

            return HandlerInterceptor.super.preHandle(request, response, handler);
        }else {
            response.sendRedirect("/login.html");
            return false;
        }

    }
}
