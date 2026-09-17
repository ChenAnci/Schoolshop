package situ.app.dto;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class SearchInfo {

    private String items="";//多个条件   col`val`>
    public HashMap<String,Object> getSearchItem(){
        HashMap<String,Object> params = new HashMap<String,Object>() ;
        if(items.length()>0) {
            String[] ts = items.split("~");
            for (int i = 0; i < ts.length; i++) {
                String item = ts[i];
                String[] vs = item.split("`");
                params.put(vs[0], vs[1]);
            }
        }
        return params;
    }



    private  Long page;
    private  Long limit;
    public Long getLimit(){
        return limit==null?100000:limit;
    }

    public <T> IPage<T> getPageInfo(Class<T> cls){
        if(page==null) return null;
        IPage<T> po = new Page<T>(page, limit);
        return po;
    }
    public <T> QueryWrapper<T>  getQuery(QueryWrapper<T> ex,Class<T> cls){
        QueryWrapper<T> q =null;
        if(ex==null) q=new QueryWrapper<T>();
        else q=ex;
        if(items.length()>0){
            String[] ts=items.split("~");
            for(int i=0;i<ts.length;i++){
                String item=ts[i];
               String[] vs= item.split("`");
                if(vs[2].equals("==")) q.eq(vs[0],vs[1]);
                else if(vs[2].equals("like")) q.like(vs[0],vs[1]);
            }
        }

        return q;
    }
}
