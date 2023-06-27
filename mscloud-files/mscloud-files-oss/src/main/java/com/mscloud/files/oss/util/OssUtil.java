package com.mscloud.files.oss.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OSS配置以及初始化链接
 */
@Slf4j
@Component
public class OssUtil {
    private OSS ossClient =null;
    @Value("${mscloud.oss.endpoint:'http://XXXXX'}")
    private String endpoint;
    /**
     * 默认配置的Bucket桶
     */
    @Value("${mscloud.oss.bucketName:'bucket'}")
    private String defaultBucket;
    @Value("${mscloud.oss.accessKey:'accessKey'}")
    private String accessKey;
    @Value("${mscloud.oss.secretKey:'secretKey'}")
    private String secretKey;
    @Value("${mscloud.oss.replaceUrlPrefix:''}")
    private String replaceUrlPrefix;


    /**
     * OSSClient初始化
     * @return
     */
    public OSS getOssClient(){

      if(ossClient !=null){
          return ossClient;
      }
      OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);

        return ossClient;
    }


    public String getEndpoint() {
        return endpoint;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getReplaceUrlPrefix() {
        return replaceUrlPrefix;
    }
}
