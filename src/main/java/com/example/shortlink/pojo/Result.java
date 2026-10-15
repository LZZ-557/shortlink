package com.example.shortlink.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public  static Result success(){
        Result result=new Result();
        result.code=0;
        result.msg="success";
        return result;
    }
    public static Result success(Object object){
        Result result=new Result();
        result.data=object;
        result.code=0;
        result.msg="success";
        return result;
    }
    public  static Result error(String msg){
        Result result=new Result();
        result.msg=msg;
        result.code=1;
        return result;
    }

}
