package com.mscloud.files.core;


import com.mscloud.files.core.constants.FileDirType;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 工具类
 */
public class FileUtil {
    private static Map<String, String> types;//contentType集合
    private static List<String> extensions = new ArrayList<String>();//后缀集合
    public final static Map<String, String> FILE_TYPE_MAP = new HashMap<String, String>();//文件头集合

    // 对应的http contenttype
    static  {
        types = new HashMap<String, String>();
        types.put("application/pdf", ".pdf");
        types.put("text/plain", ".txt");
        types.put("text/html", ".html");
        types.put("application/x-rtf", ".rtf");
        types.put("message/rfc822", ".mht");
        types.put("image/jpeg", ".jpg");
        types.put("image/png", ".png");
        types.put("image/gif", ".gif");
        types.put("image/bmp", ".bmp");
        types.put("image/tiff", ".tiff");
        types.put("image/webp", ".webp");
        types.put("image/svg+xml", ".svg");
        types.put("image/x-icon", ".icon");
        types.put("application/msword", ".doc");
        types.put("application/vnd.openxmlformats-officedocument.wordprocessingml.template", ".docx");
        types.put("application/x-xls", ".xls");
        types.put("application/-excel", ".xls");
        types.put("application/vnd.ms-excel", ".xls");
        types.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
        types.put("application/x-ppt", ".ppt");
        types.put("application/vnd.ms-powerpoint", ".ppt");
        types.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx");
        types.put("message/rfc822", ".eml");
        types.put("application/xml", ".xml");
        types.put("application/zip", ".zip");
        types.put("application/gzip", ".gzip");
        types.put("application/x-tar", ".tar");
        types.put("text/csv", ".csv");
        types.put("application/x-rar-compressed", ".rar");
        types.put("application/json", ".json");
        types.put("video/mp4", ".mp4");
        types.put("video/x-msvideo", ".avi");
        types.put("video/x-ms-wmv", ".mwv");
        types.put("video/quicktime", ".mov");
        types.put("video/x-flv", ".flv");
        types.put("video/mpeg", ".mpeg");
        types.put("video/ogg", ".ogg");
        types.put("video/webm", ".webm");
    }

    /**
     * 截取文件后缀名
     * @param fileName 文件名
     * @return
     */
    public static String splitFileSuffix(String fileName){
        if(fileName==null){
            return null;
        }
        int index=fileName.lastIndexOf(".");
        if(index ==-1){//没有后缀
            return null;
        }
        String suffix =fileName.substring(index);
        return suffix;
    }

    /**
     * 截取网络URL文件后缀名
     * @param url 网络地址
     * @return
     */
    public static String splitUrlFileSuffix(URL url){
        if(url==null){
            return null;
        }
        //优先获取URL路径的后缀
        String path=url.getPath();
        int index=path.lastIndexOf(".");
        if(index >0){//有后缀

            String suffix =path.substring(index);
            return suffix;
        }
        //Tika方式获取
        Tika tika = new Tika();
        try {
            String contentType=tika.detect(url);
            System.out.println(contentType);
            if (contentType!=null && types.containsKey(contentType.toLowerCase()))
                return types.get(contentType);
        } catch (IOException e) {

        }
        return null;
    }
    /**
     * 获取日期格式的目录名
     * @param dirType 日期格式
     * @param fileDirPath 固定目录
     * @return
     */
    public static String dirName(FileDirType dirType, String fileDirPath){

       return dirName( dirType, fileDirPath,true);
    }

    /**
     * 截取URL的文件名字
     * @param url
     * @return
     */
    public static String getFileName(String url) {
        String suffix = "";
        int index = url.lastIndexOf("/");
        if (-1 != index) {
            suffix = url.substring(index + 1, url.length());
        }

        return suffix;
    }
    /**
     * 获取日期格式的目录名
     * @param dirType 日期格式
     * @param fileDirPath 固定目录
     * @param isMutiDir 是否多层目录
     * @return
     */
    public static String dirName(FileDirType dirType,String fileDirPath,boolean isMutiDir){

        if(StringUtils.isNotBlank(fileDirPath)){
            if(dirType==null || dirType.equals(FileDirType.ROOT)){//固定目录
                if(! isMutiDir){
                    fileDirPath=fileDirPath.replaceAll("/","");//一层目录
                    return fileDirPath;
                }else{
                    return fileDirPath;
                }
            }else {//固定目录+日期目录
                SimpleDateFormat sdf=new SimpleDateFormat(dirType.value());
                String dirName=sdf.format(new Date());
                if(! isMutiDir){
                    dirName=dirName.replaceAll("/","");//一层目录
                    return fileDirPath+dirName;
                }else{
                    return fileDirPath+"/"+dirName;
                }

            }
        }else {
            if(dirType==null){
                dirType=FileDirType.DAY;
            }
            if(dirType.equals(FileDirType.EMPTY)){//不需要目录
                return "";
            }
            SimpleDateFormat sdf=new SimpleDateFormat(dirType.value());
            String dirName=sdf.format(new Date());
            if(! isMutiDir){
                dirName= dirName.replaceAll("/","");//一层目录
            }else{
            }
            return dirName;
        }

    }

}
