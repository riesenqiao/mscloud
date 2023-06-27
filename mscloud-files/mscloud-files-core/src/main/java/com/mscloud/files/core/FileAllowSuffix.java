package com.mscloud.files.core;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 文件上传后缀配置
 */
@RefreshScope
@Data
@Component
@ConfigurationProperties(prefix = "mscloud.files")
public class FileAllowSuffix implements InitializingBean{
    private Boolean enabled=true;
    private Set<String> allowSuffixs=new HashSet<>();

    private static final Set<String> DEFAULT_FILE_TYPE = new HashSet<>();

    static {
        DEFAULT_FILE_TYPE.add(".bpm");
        DEFAULT_FILE_TYPE.add(".jpg");
        DEFAULT_FILE_TYPE.add(".jpeg");
        DEFAULT_FILE_TYPE.add(".png");
        DEFAULT_FILE_TYPE.add(".gif");
        DEFAULT_FILE_TYPE.add(".pdf");
        DEFAULT_FILE_TYPE.add(".xls");
        DEFAULT_FILE_TYPE.add(".xlsx");
        DEFAULT_FILE_TYPE.add(".doc");
        DEFAULT_FILE_TYPE.add(".docx");
        DEFAULT_FILE_TYPE.add(".ppt");
        DEFAULT_FILE_TYPE.add(".pptx");
        DEFAULT_FILE_TYPE.add(".txt");
        DEFAULT_FILE_TYPE.add(".rar");
        DEFAULT_FILE_TYPE.add(".zip");
        DEFAULT_FILE_TYPE.add(".mv");
        DEFAULT_FILE_TYPE.add(".wmv");
        DEFAULT_FILE_TYPE.add(".swf");
        DEFAULT_FILE_TYPE.add(".mpg");
        DEFAULT_FILE_TYPE.add(".mov");
        DEFAULT_FILE_TYPE.add(".mp4");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(allowSuffixs);
        Set<String> set=new HashSet<>();
        for(String str:allowSuffixs){//转为小写
            set.add(str.toLowerCase());
        }
        set.addAll(DEFAULT_FILE_TYPE);
        allowSuffixs=new HashSet<>();
        allowSuffixs.addAll(set);
    }

    /**
     * 后缀是否允许
     * @param fileSuffix
     * @return
     */
    public boolean isAllow(String fileSuffix){
        if(StringUtils.isBlank(fileSuffix)){
            return true;
        }
        boolean isAllow=this.allowSuffixs.contains(fileSuffix);
        return isAllow;
    }
}
