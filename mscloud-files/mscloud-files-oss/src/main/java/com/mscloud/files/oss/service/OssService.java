package com.mscloud.files.oss.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.mscloud.files.core.FileUtil;
import com.mscloud.files.core.constants.FileDirType;
import com.mscloud.files.core.constants.StoreType;
import com.mscloud.files.core.pojo.FileInfo;
import com.mscloud.files.core.pojo.FileResult;
import com.mscloud.files.core.pojo.HttpResponse;
import com.mscloud.files.oss.util.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * 阿里OSS文件操作服务类
 */
@Slf4j
@Component
public class OssService implements InitializingBean {



    private OSS ossClient;

    @Autowired
    OssUtil ossUtil;

    @Override
    public void afterPropertiesSet() throws Exception {

    }
    /**
     * 上传单个文件
     * @param inputStream 文件流
     * @param fileInfo 文件信息
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定目录
     * @param isBackShareLink 是否返回临时链接
     * @return
     */
    public FileResult uploadFile(InputStream inputStream, FileInfo fileInfo,boolean isRename, FileDirType fileDirType, String fileDirPath, boolean isBackShareLink, Map<String,String> tagMap) {

        ossClient= ossUtil.getOssClient();
        if(ossClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到OSS数据源配置").build();
        }
        String endpoint=ossUtil.getEndpoint();
        String prefixUrl=ossUtil.getReplaceUrlPrefix();
        String bucket=ossUtil.getDefaultBucket();
        if(bucket==null){
            log.error("默认的Bucket桶为空，请检测配置");
            return FileResult.builder().isOk(false).msg("上传失败,OSS数据源配置默认bucket为空,请指定Bucket").build();
        }

        try {
            Long fileSize=fileInfo.getFileSize();//文件大小字节
            String contentType=fileInfo.getContentType();//文件类型
            String orignalFilename=fileInfo.getOrignalName();//文件原始名称
            String fileSuffix= fileInfo.getFileSuffix();//后缀
            String reName= UUID.randomUUID().toString().replaceAll("-","")+ (fileSuffix==null?"":fileSuffix);
            String  newStoreName=(isRename?reName:orignalFilename);

            //创建目录格式
            if(fileDirType==null|| ! fileDirType.equals(FileDirType.EMPTY)){
                //拼接目录
                newStoreName= FileUtil.dirName(fileDirType,fileDirPath)+"/"+newStoreName;
            }else{
                newStoreName=fileDirPath+"/"+newStoreName;
            }

            // 创建PutObjectRequest对象并设置ObjectMetadata
            ObjectMetadata metadata = new ObjectMetadata();
            for(String key:tagMap.keySet()){
                String value = tagMap.get(key).toString();
                metadata.addUserMetadata(key, value);
            }
            // 上传文件到指定的存储空间（bucketName）并将其保存为指定的文件名称（objectName）。
            PutObjectResult putObjectResult= ossClient.putObject(bucket, newStoreName, inputStream,metadata);

            System.out.println(putObjectResult);
            String filePath="";
            if(StringUtils.isNotBlank(prefixUrl)){
                //替换应用域名前缀
                filePath=prefixUrl+"/"+newStoreName;
            }else{
                //将bucket名称拼接到域名上
                String url=endpoint.replace("https://","https://"+bucket+".");
                filePath=url+"/"+newStoreName;
            }

            String storeName=newStoreName;//存储返回的名称

            String shareLink ="";
            if(isBackShareLink){
                FileResult linkResult=getShareLink(storeName,1*24*60*60);
                shareLink =linkResult.getFilePath();
            }
            return FileResult.builder().isOk(true).msg("上传成功").bucketName(bucket).storeType(StoreType.OSS.value()).shareLink(shareLink).filePath(filePath).fileSize(fileSize).contentType(contentType).storeName(storeName).orignalName(orignalFilename).fileSuffix(fileSuffix).build();

        }catch (Exception e){
            e.printStackTrace();
            return FileResult.builder().isOk(false).msg("上传失败"+e.getLocalizedMessage()).build();
        }
        finally {
            if(inputStream !=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return FileResult.builder().isOk(false).msg("上传失败").build();
                }
            }
        }

    }

    /**
     * 上传多个文件
     * @param multipartFiles 多个文件流数组
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定目录
     * @return
     */
    public List<FileResult> uploadFiles(MultipartFile[] multipartFiles,boolean isRename,FileDirType fileDirType,String fileDirPath,boolean isBackShareLink,Map<String,String> tagMap){
        List<FileResult> list=new ArrayList<>();
        for(MultipartFile file:multipartFiles){
            //文件基本信息
            String orignalFilename=file.getOriginalFilename();//文件原始名称
            String fileSuffix= FileUtil.splitFileSuffix(orignalFilename);//后缀
            FileInfo fileInfo=FileInfo.builder().fileSize(file.getSize()).contentType(file.getContentType()).orignalName(orignalFilename).fileSuffix(fileSuffix).build();
            InputStream inputStream= null;
            try {
                inputStream = file.getInputStream();
                FileResult fileResult= this.uploadFile(inputStream,fileInfo,isRename,fileDirType,fileDirPath,isBackShareLink,tagMap);
                list.add(fileResult);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    /**
     * 下载文件
     @param filePath 指定下载的文件全路径
     * @param storeAsFilename 下载后文件名
     * @param response  响应文件流
     */
    public FileResult downloadFile(String filePath,String storeAsFilename,HttpServletResponse response){
        ossClient=ossUtil.getOssClient();
        if(ossClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到OSS数据源配置").build();
        }

          String  bucket=ossUtil.getDefaultBucket();
            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("下载失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
            }

        BufferedInputStream inputStream = null;
        OutputStream outputStream =null;
        try{

            OSSObject ossObject = ossClient.getObject(bucket, filePath);
            inputStream =new BufferedInputStream(ossObject.getObjectContent());
            byte buf[] = new byte[1024];
            int length = 0;
            response.reset();
            //下载名称
            String storeAs = storeAsFilename==null?filePath:storeAsFilename;
            response.setHeader("Content-Disposition","attachment;filename=" + URLEncoder.encode(storeAs, "UTF-8"));
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            outputStream = response.getOutputStream();
            while((length = inputStream.read(buf)) > 0){
                outputStream.write(buf,0,length);
            }
            return FileResult.builder().isOk(true).msg("下载成功").build();
        } catch (IOException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,OSS链接IO异常,"+e.getMessage()).build();

        } catch (Throwable throwable) {
            log.warn("下载失败{}",throwable.getMessage());
        } finally {
            try {
                if(inputStream !=null){
                    inputStream.close();
                }
                if(outputStream !=null){
                    outputStream.close();
                }

            } catch (IOException e) {
            }
        }
        return FileResult.builder().isOk(false).msg("下载失败,OSS异常").build();
    }


    /**
     * 获取一个指定了 HTTP 方法、到期时间和自定义请求参数的对象URL地址，也就是返回带签名的URL，
     *  这个地址可以提供给没有登录的第三方共享访问或者上传对象。
     *  返回的是进行加密算法的地址，通过它可以直接访问文件
     * @param filePath 文件名称
     * @param expires 过期时间 单位：秒
     * @return url
     */
    public FileResult getShareLink(String filePath,Integer expires){

        boolean bucketExists = false;
        ossClient=ossUtil.getOssClient();
        if(ossClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到OSS数据源配置").build();
        }

        String  bucket=ossUtil.getDefaultBucket();
            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("获取失败,OSS数据源配置默认bucket为空,请指定Bucket").build();
            }

        if(expires !=null){
            if(expires <= 0 ){
                return FileResult.builder().isOk(false).msg("链接有效时长不能为负数").build();
            }
        }else{//默认为7天
            expires =7*24*60*60;
        }
        // 设置URL过期时间
        long expireTime=expires+(new Date().getTime());
        Date expiration=new Date(expireTime);

        if(! fileExists(filePath)){//检测是否存在
            return FileResult.builder().isOk(false).msg("文件不存在:"+filePath).build();
        }
        //文件存在生成分享链接
        URL url = ossClient.generatePresignedUrl(bucket, filePath, expiration);
        if(url !=null){
            return FileResult.builder().isOk(true).msg("获取链接成功").filePath(url.toString()).build();
        }else{
            return FileResult.builder().isOk(false).msg("获取败").build();
        }
    }

    /**
     * 判断文件是否存在
     * @param objectName 文件路径
     * @return true存在，false不存在
     */
    public Boolean fileExists(String objectName) {
        if( StringUtils.isEmpty(objectName)){
            return false;
        }
        try {
            ossClient=ossUtil.getOssClient();
            String  bucket=ossUtil.getDefaultBucket();
            if(ossClient==null){
                return  false;
            }
            boolean isExists = ossClient.doesObjectExist(bucket,objectName);
            if(isExists){
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return
     */
    public FileResult deleteFile(String filePath) {

        try {
            ossClient=ossUtil.getOssClient();
            if(ossClient==null){
                return FileResult.builder().isOk(false).msg("上传失败,找不到OSS数据源配置").build();
            }

             String   bucket=ossUtil.getDefaultBucket();
                if(bucket==null){
                    log.error("默认的Bucket桶为空，请检测配置");
                    return FileResult.builder().isOk(false).msg("删除失败,OSS数据源配置默认bucket为空,请指定Bucket").build();
                }

            if(! fileExists(filePath)){
                return FileResult.builder().isOk(false).msg("删除失败，文件不存在:"+filePath).build();
            }
            ossClient.deleteObject(bucket,filePath);
            log.info("删除文件bucket={},file={}",bucket,filePath);
            return FileResult.builder().isOk(true).msg("删除成功").build();
        }catch (Exception e){
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio异常").build();
        }
    }

    /**
     * 本方法会清空目录的所有文件，请谨慎使用
     * 删除目录以及以下的所有文件
     * @param folderPathPrefix 目录
     */
    public FileResult deleteDir(String folderPathPrefix){
        ossClient=ossUtil.getOssClient();
        if(ossClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到OSS数据源配置").build();
        }

         String  bucket=ossUtil.getDefaultBucket();
            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("删除失败,OSS数据源配置默认bucket为空,请指定Bucket").build();
            }

        ObjectListing list= this.ossClient.listObjects(bucket,folderPathPrefix);
        System.out.println(list.getObjectSummaries());
        if(list !=null && list.getObjectSummaries().size()>0){
            List<OSSObjectSummary> objList= list.getObjectSummaries();
            for(OSSObjectSummary item:objList){
                //是否文件夹
                try {

                    this.ossClient.deleteObject(bucket,item.getKey());
                    log.info("删除文件bucket={},file={}",bucket,item.getKey());
                }catch (Exception e){
                    log.warn("删除失败{}",e.getMessage());
                    return FileResult.builder().isOk(false).msg("删除失败,OSS异常").build();
                }
            }
        }


        return FileResult.builder().isOk(true).msg("成功").build();
    }
}
