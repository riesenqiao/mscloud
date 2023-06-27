package com.mscloud.files.minio.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Slf4j
@Component
public class BucketPolicyJsonUtil {

    /**
     * 给存储桶生成只读访问策略，
     * Minio的桶策略是基于访问策略语言规范（Access Policy Language specification）
     * 的解析和验证存储桶访问策略 –Amazon S3
     * @param path  /policyJson-readOnly.json 文件路径
     * @param bucketName  要给哪只桶生成访问策略
     * @return
     * @throws IOException
     */
    public static String jsonToString(String path,String bucketName) throws IOException {
        StringBuilder result = new StringBuilder();
        if(StringUtils.isEmpty(path)){
            path = "/policyJson-readOnly.json";
        }
        ClassPathResource pathResource = new ClassPathResource(path);
        InputStream in = pathResource.getInputStream();

        InputStreamReader isr = new InputStreamReader(in,"UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while((line = br.readLine()) != null){
            result.append(line);
        }
        isr.close();
        //将policyJson-readOnly.json文件中的内容转成json，然后用bucketName替换文件中的ltc存储桶
        JSONObject object = (JSONObject) JSONObject.parse(result.toString());
        JSONArray statement = (JSONArray) object.get("Statement");
        for(int i=0;i<statement.size();i++){
            JSONObject o = (JSONObject) statement.get(i);
            String resource = o.getString("Resource");
            JSONArray resourceArray = JSONArray.parseArray(resource);
            resource = resourceArray.getString(0);
            resource = resource.replace("ltc",bucketName);
            resourceArray.set(0,resource);
            o.fluentPut("Resource",resourceArray);
        }

        log.info(bucketName+"policy===="+object.toString());
        return object.toString();
    }
}
