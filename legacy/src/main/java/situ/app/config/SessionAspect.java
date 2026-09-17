package situ.app.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;

@Aspect
@Component
public class SessionAspect {

    @Pointcut("execution(* situ.app.serviceImpl.*.*(..))")
    public void executeService() {
    }
    @Around(value="executeService()")//好的
    public Object doAroundAdvice(ProceedingJoinPoint joinPoint)throws  Throwable{
        System.out.println("前置通知");
        Object target=joinPoint.getTarget();

        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.getRequestAttributes())).getRequest();
        HttpSession session=request.getSession();
        try {
           Field f= target.getClass().getSuperclass().getDeclaredField("current");
           f.setAccessible(true);
           f.set(target,session.getAttribute("user"));
        }catch (Exception e) {
            e.printStackTrace();
        }
            Object obj = joinPoint.proceed();//执行
        System.out.println("后置通知");
            return obj;
    }
}
