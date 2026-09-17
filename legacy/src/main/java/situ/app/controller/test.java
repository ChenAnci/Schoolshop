package situ.app.controller;

import org.springframework.stereotype.Component;
import situ.app.pojo.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
//@Component
public class test {
    public static void main(String[] args) throws  Exception{

//    Class cls=User.class;
//    Class cls=u.getClass();
        Class cls=Class.forName("situ.app.pojo.User") ;
        Object a= cls.newInstance();                    // user a=new user();
        Field f= cls.getDeclaredField("name");
        f.setAccessible(true);//跳过安全
        f.set(a,"哇哦的名字");                            //  a.name="哇哦的名字";

        Method m=cls.getMethod("setId",Integer.class);
       Object returndata= m.invoke(a,100);          // a.setId(100)

        User u= (User) a;
        System.out.println(u);
    }
}
