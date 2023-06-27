package com.mscloud.files.minio.util;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MinioClientUtil {

    private MinioClient minioClient =null;
    @Value("${mscloud.minio.endpoint:'http://localhost:9000'}")
    private String endpoint;
    /**
     * 默认配置的Bucket桶
     */
    @Value("${mscloud.minio.bucketName:'bucket'}")
    private String defaultBucket;
    @Value("${mscloud.minio.accessKey:'accessKey'}")
    private String accessKey;
    @Value("${mscloud.minio.secretKey:'secretKey'}")
    private String secretKey;
    @Value("${mscloud.minio.replaceUrlPrefix:''}")
    private String replaceUrlPrefix;


    /**
     * MinioClient初始化
     * @return
     */
    public  MinioClient getMinioClient(){
        if(minioClient !=null){
            return minioClient;
        }
         this.minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
        return minioClient;
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
