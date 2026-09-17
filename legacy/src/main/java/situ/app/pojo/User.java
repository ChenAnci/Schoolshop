package situ.app.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("v_user")
public class User {
//    @TableId
    private Integer id;
//    @TableField("aaa")
    private  String name;
    private  String pass;
    private Date createdate;
    private Integer power;
    private Integer status;
    private Integer department_id;

    private Integer isdel;

    private String department_name;

    public static  String[] STATUSS={"在职","休假","离职"};
    public  String getStatusname(){
        return STATUSS[status];
    }
    public static  String[] POWERS={"管理员","操作员"};
    public  String getPowername(){
        return POWERS[power];
    }

//    @TableField(exist = false)
//    private String allinfio;
}
