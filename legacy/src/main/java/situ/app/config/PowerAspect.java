package situ.app.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import situ.app.dto.ResultInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class PowerAspect {

    @Pointcut("@annotation(situ.app.config.PowerCheck)")
    public void executeService() {
    }
    @Around(value="executeService()")//好的
    public Object doAroundAdvice(ProceedingJoinPoint joinPoint)throws  Throwable{

        Object target=joinPoint.getTarget();
        MethodSignature sg= (MethodSignature) joinPoint.getSignature();
        PowerCheck p= (PowerCheck) sg.getMethod().getAnnotation(PowerCheck.class);
       int[] vs= p.value();  //声明的权限


        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.getRequestAttributes())).getRequest();
        HttpSession session=request.getSession();
        try {

          Object u=session.getAttribute("user");

          Method m=u.getClass().getMethod("getPower");
            int pv= (Integer) m.invoke(u);  //当前的权限
            boolean isok=false;
            for(int i=0;i<vs.length;i++)if(vs[i]==pv) {
                isok=true;
                break;
            }
            if(!isok) {
                ResultInfo info=new ResultInfo();
                info.setCode(-1);
                info.setMsg("你的权限不足");
                return info;
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
            Object obj = joinPoint.proceed();//执行
            return obj;
    }
}
