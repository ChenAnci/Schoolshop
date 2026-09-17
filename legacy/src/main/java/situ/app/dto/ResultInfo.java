package situ.app.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class ResultInfo {
    private  int code;
    private  String msg;
    private  long count;
    private List data;

    private HashMap<String,Object> params;

}
