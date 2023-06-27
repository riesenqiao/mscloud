package com.mscloud.files.core.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * 2 * @Author: Riesen
 * 3 * @Date: 2020/7/6 11:52
 * 4 返回值封装
 */
@Data
public class HttpResponse implements Serializable{


    private boolean isOK;
    /**
     * 状态码
     */
    private int code=200;
    /**
     * 信息
     */
    private String msg ="成功";
    /**
     * 返回数据
     */
    private Object data;


    public  static HttpResponse success(){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=true;
        httpResponse.setCode(200);
        return  httpResponse ;

    }
    public  static HttpResponse success(String msg){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=true;
        httpResponse.setMsg(msg);
        httpResponse.setCode(200);
        return  httpResponse ;

    }
    public  static HttpResponse success(Object data){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=true;
        httpResponse.setCode(200);
        httpResponse.setData(data);
        return  httpResponse ;

    }
    public  static HttpResponse success(String msg,Object data){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=true;
        httpResponse.setMsg(msg);
        httpResponse.setCode(200);
        httpResponse.setData(data);
        return  httpResponse ;

    }

    public static HttpResponse error(){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=false;
        httpResponse.setCode(500);
        httpResponse.setMsg("未知异常，请联系管理员");
        return  httpResponse ;
    }
    public static HttpResponse error(String msg){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=false;
        httpResponse.setCode(500);
        httpResponse.setMsg(msg);
        return  httpResponse ;
    }
    public static HttpResponse error(int code, String msg){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=false;
        httpResponse.setCode(code);
        httpResponse.setMsg(msg);
        return  httpResponse ;
    }
    public static HttpResponse error(int code, String msg,Object data){
        HttpResponse httpResponse=new HttpResponse();
        httpResponse.isOK=false;
        httpResponse.setCode(code);
        httpResponse.setMsg(msg);
        httpResponse.setData(data);
        return  httpResponse ;
    }

}
