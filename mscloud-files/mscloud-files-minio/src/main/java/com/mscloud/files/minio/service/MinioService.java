package com.mscloud.files.minio.service;


import com.mscloud.files.core.FileUtil;
import com.mscloud.files.core.constants.FileDirType;
import com.mscloud.files.core.constants.StoreType;
import com.mscloud.files.core.pojo.FileInfo;
import com.mscloud.files.core.pojo.FileResult;
import com.mscloud.files.minio.util.MinioClientUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
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
import java.net.ConnectException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Minio文件操作服务类
 */
@Slf4j
@Component
public class MinioService implements InitializingBean {

    @Autowired
    private MinioClientUtil minioClientUtil;


    @Override
    public void afterPropertiesSet() throws Exception {

    }

    /**
     * 上传多个文件
     * @param multipartFiles 多个文件流数组
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定目录
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    public List<FileResult> uploadFiles(MultipartFile[] multipartFiles, boolean isRename, FileDirType fileDirType, String fileDirPath,  boolean isBackShareLink, Map<String,String> tagMap){
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
     * 上传单个文件
     * @param inputStream 文件流
     * @param fileInfo 文件基本信息
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定目录
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    public FileResult uploadFile(InputStream inputStream, FileInfo fileInfo, boolean isRename, FileDirType fileDirType, String fileDirPath, boolean isBackShareLink,Map<String,String> tagMap){

        MinioClient minioClient=minioClientUtil.getMinioClient();
        if(minioClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到Minio数据源配置").build();
        }
        String endpoint=minioClientUtil.getEndpoint();
        String prefixUrl=minioClientUtil.getReplaceUrlPrefix();
        String bucket =minioClientUtil.getDefaultBucket();
        if(StringUtils.isBlank(bucket)){
            bucket=minioClientUtil.getDefaultBucket();
            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("上传失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
            }
        }
        try {

            Long fileSize=fileInfo.getFileSize();//文件大小字节
            String contentType=fileInfo.getContentType();//文件类型
            String orignalFilename=fileInfo.getOrignalName();//文件原始名称
            String fileSuffix=fileInfo.getFileSuffix();//后缀
            String reName=UUID.randomUUID().toString().replaceAll("-","")+ (fileSuffix==null?"":fileSuffix);;
            String  newStoreName=(isRename?reName:orignalFilename);

            //创建目录格式
            if(fileDirType==null|| ! fileDirType.equals(FileDirType.EMPTY)){
                //拼接目录
                newStoreName=FileUtil.dirName(fileDirType,fileDirPath)+"/"+newStoreName;
            }else{
                newStoreName=fileDirPath+"/"+newStoreName;
            };
            //
            if(tagMap==null){
                tagMap=new HashMap<>();
            }

           ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .tags(tagMap)
                    .userMetadata(tagMap)
                    .object(newStoreName)
//                    .contentType(contentType)
                    .stream(inputStream,inputStream.available(),-1)
                    .build());

            String filePath="/"+bucket+"/"+response.object();
            filePath=filePath.replaceAll("//","/");
            if(StringUtils.isNotBlank(prefixUrl)){
                filePath=prefixUrl+filePath;
            }else{
                filePath=endpoint+filePath;
            }

            String storeName=response.object();//存储返回的名称
            String shareLink ="";
            if(isBackShareLink){
                FileResult linkResult=getShareLink(storeName,5*24*60*60);
                shareLink =linkResult.getFilePath();
            }

            return FileResult.builder().isOk(true).msg("上传成功").bucketName(bucket).storeType(StoreType.MINIO.value()).shareLink(shareLink).filePath(filePath).fileSize(fileSize).contentType(contentType).storeName(storeName).orignalName(orignalFilename).fileSuffix(fileSuffix).build();
        }catch (ConnectException e){
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio链接endpoint错误,"+e.getMessage()).build();
        } catch (IOException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio链接IO异常,"+e.getMessage()).build();
        } catch (XmlParserException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio链接XML异常,"+e.getMessage()).build();
        } catch (ServerException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio ServerL异常,"+e.getMessage()).build();
        } catch (NoSuchAlgorithmException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio Algorithm异常,"+e.getMessage()).build();
        } catch (InsufficientDataException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,"+e.getMessage()).build();
        } catch (InvalidKeyException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,InvalidKey错误"+e.getMessage()).build();
        } catch (InvalidResponseException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,Invalid响应错误"+e.getMessage()).build();
        } catch (ErrorResponseException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio错误认证信息"+e.getMessage()).build();
        } catch (InternalException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio Internal错误").build();
        }catch (Exception e){
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio异常").build();
        }finally {
            try {
                if(inputStream !=null){
                    inputStream.close();
                }

            } catch (IOException e) {
            }
        }
    }

    /**
     * 分片上传文件，先上传到临时目录，然后合并所有分片文件
     * @param file 分片文件流
     * @param sliceIndex 分片索引
     * @param totalPieces 切片总数
     * @param md5String 整体文件MD5
     * @return
     */
    public FileResult uploadPieceFile(MultipartFile file, String fileName,int sliceIndex, int totalPieces,String md5String,boolean isRename,FileDirType fileDirType,String fileDirPath,boolean isBackShareLink,Map<String,String> tagMap){
        int index =-2;//-1表示已全部分片上传完毕
        MinioClient minioClient=minioClientUtil.getMinioClient();
        if(minioClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到Minio数据源配置").build();
        }
        String endpoint=minioClientUtil.getEndpoint();
        String prefixUrl=minioClientUtil.getReplaceUrlPrefix();
        String bucket=minioClientUtil.getDefaultBucket();
        if(bucket==null){
            log.error("默认的Bucket桶为空，请检测配置");
            return FileResult.builder().isOk(false).msg("上传失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
        }

        try {
            // 存放临时目录
            String tempDir="temp_upload".concat("/").concat(md5String).concat("/");

            // 查询已经上传成功的文件序号
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(tempDir).build());
            Set<String> objectNames = new HashSet();
            for (Result<Item> item : results) {
                objectNames.add(item.get().objectName());
            }
            //查询已经上传成功的最大序号的分片临时文件
            List<Integer> indexs = Stream.iterate(0, i -> ++i)
                    .limit(totalPieces)
                    .filter(i -> !objectNames.contains(tempDir.concat(Integer.toString(i))))
                    .sorted()
                    .collect(Collectors.toList());
            // 返回需要上传的文件序号，-1是上传完成
            if (indexs.size() > 0) {
                if (!indexs.get(0).equals(sliceIndex)) {
                    index = indexs.get(0);
                    return FileResult.builder().isOk(true).sliceIndex(index).msg("分片上传成功").build();//单个分片上传成功
                }
            } else {
                index = -1;//全部分片上传完成
            }

            if (index != -1) {
                // 写入文件
                String objectName=tempDir.concat(Integer.toString(sliceIndex));
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }

            if (sliceIndex < totalPieces - 1) {
                index = ++sliceIndex;
            } else {
                index = -1;
            }

            if (index == -1) {//合并迁移临时分片文件
                FileResult tempResult = this.composePieceFile(minioClient, fileName, totalPieces, md5String,prefixUrl ,endpoint,bucket,isRename,fileDirType,fileDirPath,isBackShareLink,tagMap);

               return tempResult;

            }
            return FileResult.builder().isOk(true).sliceIndex(index).msg("分片上传成功").build();
        }catch (ConnectException e){
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio链接endpoint错误,"+e.getMessage()).build();
        } catch (IOException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio链接IO异常,"+e.getMessage()).build();
        } catch (XmlParserException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio链接XML异常,"+e.getMessage()).build();
        } catch (ServerException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio ServerL异常,"+e.getMessage()).build();
        } catch (NoSuchAlgorithmException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio Algorithm异常,"+e.getMessage()).build();
        } catch (InsufficientDataException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,"+e.getMessage()).build();
        } catch (InvalidKeyException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,InvalidKey错误"+e.getMessage()).build();
        } catch (InvalidResponseException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,Invalid响应错误"+e.getMessage()).build();
        } catch (ErrorResponseException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio错误认证信息"+e.getMessage()).build();
        } catch (InternalException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio Internal错误").build();
        }catch (Exception e){
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,minio异常").build();
        }
    }

    /**
     * 上传从临时目录合并分片文件迁移到正式目录
     * @param totalPieces 切片总数
     * @param md5String 整体文件MD5
     * @return
     * @throws Exception
     */
    private FileResult composePieceFile(MinioClient minioClient ,String fileName, Integer totalPieces,String md5String,String prefixUrl,String endpoint,String bucket,boolean isRename,FileDirType fileDirType,String fileDirPath,boolean isBackShareLink,Map<String,String> tagMap) throws Exception {

        String orignalFilename= fileName;//文件原始名称,传入的名字为空
        String fileSuffix=FileUtil.splitFileSuffix(orignalFilename);//后缀
        String reName=UUID.randomUUID().toString().replaceAll("-","")+ (fileSuffix==null?"":fileSuffix);
        String  newStoreName=(isRename?reName:orignalFilename);

        //创建目录格式
        if(fileDirType==null|| ! fileDirType.equals(FileDirType.EMPTY)){
            //拼接目录
            newStoreName=FileUtil.dirName(fileDirType,fileDirPath)+"/"+newStoreName;
        }
        String tempDir="temp_upload".concat("/").concat(md5String).concat("/");

        if(tagMap==null){
            tagMap=new HashMap<>();
        }

        // 完成上传从缓存目录合并迁移到正式目录
        List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
                .limit(totalPieces)
                .map(i -> ComposeSource.builder()
                        .bucket(bucket)
                        .object(tempDir.concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());

        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        ObjectWriteResponse response = minioClient.composeObject(
                ComposeObjectArgs.builder()
                        .bucket(bucket)
                        .object(newStoreName)
                        .tags(tagMap)
                        .sources(sourceObjectList)
                        .build());
        // 删除所有的分片文件
        deleteDir(tempDir);

        // 验证md5

        String filePath="";
        if(StringUtils.isNotBlank(prefixUrl)){
            filePath=prefixUrl+"/"+bucket+"/"+response.object();
        }else{
            filePath=endpoint+"/"+bucket+"/"+response.object();
        }
        String storeName=response.object();//存储返回的名称
        String shareLink ="";
        if(isBackShareLink){
            FileResult linkResult=getShareLink(storeName,1*24*60*60);
            shareLink =linkResult.getFilePath();
        }

        return FileResult.builder().isOk(true).sliceIndex(-1).msg("全部上传成功").bucketName(bucket).storeType(StoreType.MINIO.value()).shareLink(shareLink).filePath(filePath).storeName(storeName).fileSuffix(fileSuffix).orignalName(orignalFilename).build();

    }
    /**
     * 下载文件
     @param filePath 指定下载的文件全路径
     * @param storeAsFilename 下载后文件名
     * @param response  响应文件流
     */
    public FileResult downloadFile(String filePath,String storeAsFilename,HttpServletResponse response){

        MinioClient minioClient=minioClientUtil.getMinioClient();
        if(minioClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到Minio数据源配置").build();
        }
        String bucket=minioClientUtil.getDefaultBucket();
        if(StringUtils.isBlank(bucket)){
            bucket=minioClientUtil.getDefaultBucket();
            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("下载失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
            }
        }
        BufferedInputStream inputStream = null;
        OutputStream outputStream =null;
        try{
             inputStream = new BufferedInputStream(minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build()));
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
        }catch (ErrorResponseException e){
            log.warn("minio下载文件失败，找不到文件,bucket="+bucket+" , fileName="+filePath);

            if("NoSuchKey".equals(e.errorResponse().code())){//
                log.warn("下载失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("下载失败,文件不存在").build();
            }else if("NoSuchBucket".equals(e.errorResponse().code())){
                log.warn("下载失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("下载失败,Bucket错误").build();

            }else {
                log.warn("下载失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("下载失败,文件不存在,"+e.getMessage()).build();
            }

        } catch (IOException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,minio链接IO异常,"+e.getMessage()).build();
        } catch (XmlParserException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,minio链接XML异常,"+e.getMessage()).build();
        } catch (ServerException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,minio ServerL异常,"+e.getMessage()).build();
        } catch (NoSuchAlgorithmException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,minio Algorithm异常,"+e.getMessage()).build();
        } catch (InsufficientDataException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,"+e.getMessage()).build();
        } catch (InvalidKeyException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,InvalidKey错误"+e.getMessage()).build();
        } catch (InvalidResponseException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,Invalid响应错误"+e.getMessage()).build();
        } catch (InternalException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,minio Internal错误").build();
        }catch (Exception e){
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,minio异常").build();
        }finally {
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
    }



    /**
     * 获取一个指定了 HTTP 方法、到期时间和自定义请求参数的对象URL地址，也就是返回带签名的URL，
     *  这个地址可以提供给没有登录的第三方共享访问或者上传对象。
     *  返回的是进行加密算法的地址，通过它可以直接访问文件
     * @param filePath 文件名称
     * @param expires 过期时间 单位：秒，最小为1秒，最大为7天(7*24*60*60)
     * @return url
     */
    public FileResult getShareLink(String filePath,Integer expires){

        boolean bucketExists = false;
        try {
            MinioClient minioClient=minioClientUtil.getMinioClient();
            if(minioClient==null){
                return FileResult.builder().isOk(false).msg("上传失败,找不到Minio数据源配置").build();
            }
            String bucket=minioClientUtil.getDefaultBucket();

            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("获取失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
            }

            bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if(!bucketExists){
                return FileResult.builder().isOk(false).msg(bucket+"桶不存在").build();
            }
            if(expires !=null){
                if(expires <= 0 || expires > 7*24*60*60){
                    return FileResult.builder().isOk(false).msg("链接有效时长：最小为1秒，最大为7天").build();
                }
            }
            filePath=filePath.replaceAll("//","/");
            if(filePath.startsWith("/")){//去掉开头的/
                filePath=filePath.substring(1);
            }
            if(! fileExists(filePath)){//检测是否存在
                return FileResult.builder().isOk(false).msg("文件不存在:"+filePath).build();
            }
            //文件存在生成分享链接
            String url  = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(filePath)
                                .expiry(expires == null ? 7*24*60*60 : expires, TimeUnit.SECONDS)
                                .build());

            return FileResult.builder().isOk(true).msg("获取链接成功").filePath(url).build();
        } catch (IOException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取败,minio链接IO异常,"+e.getMessage()).build();
        } catch (XmlParserException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取败,minio链接XML异常,"+e.getMessage()).build();
        } catch (ServerException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,minio ServerL异常,"+e.getMessage()).build();
        } catch (NoSuchAlgorithmException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,minio Algorithm异常,"+e.getMessage()).build();
        } catch (InsufficientDataException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,"+e.getMessage()).build();
        } catch (InvalidKeyException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,InvalidKey错误"+e.getMessage()).build();
        } catch (InvalidResponseException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,Invalid响应错误"+e.getMessage()).build();
        } catch (InternalException e) {
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,minio Internal错误").build();
        }catch (Exception e){
            log.warn("获取失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("获取失败,minio异常").build();
        }
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return
     */
    public FileResult deleteFile(String filePath) {

        try {

            MinioClient minioClient=minioClientUtil.getMinioClient();
            if(minioClient==null){
                return FileResult.builder().isOk(false).msg("上传失败,找不到Minio数据源配置").build();
            }
            String bucket=minioClientUtil.getDefaultBucket();

            if(bucket==null){
                log.error("默认的Bucket桶为空，请检测配置");
                return FileResult.builder().isOk(false).msg("删除失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
            }

            if(! fileExists(filePath)){
                return FileResult.builder().isOk(false).msg("删除失败，文件不存在:"+filePath).build();
            }
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(filePath).build());
            log.info("删除文件bucket={},file={}",bucket,filePath);
            return FileResult.builder().isOk(true).msg("删除成功").build();
        }catch (ConnectException e){
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio链接endpoint错误,"+e.getMessage()).build();
        } catch (IOException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio链接IO异常,"+e.getMessage()).build();
        } catch (XmlParserException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio链接XML异常,"+e.getMessage()).build();
        } catch (ServerException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio ServerL异常,"+e.getMessage()).build();
        } catch (NoSuchAlgorithmException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio Algorithm异常,"+e.getMessage()).build();
        } catch (InsufficientDataException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,"+e.getMessage()).build();
        } catch (InvalidKeyException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,InvalidKey错误"+e.getMessage()).build();
        } catch (InvalidResponseException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,Invalid响应错误"+e.getMessage()).build();
        } catch (ErrorResponseException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio错误认证信息"+e.getMessage()).build();
        } catch (InternalException e) {
            log.warn("删除失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("删除失败,minio Internal错误").build();
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

        MinioClient minioClient=minioClientUtil.getMinioClient();
        if(minioClient==null){
            return FileResult.builder().isOk(false).msg("上传失败,找不到Minio数据源配置").build();
        }
        String bucket=minioClientUtil.getDefaultBucket();

        if(bucket==null){
            log.error("默认的Bucket桶为空，请检测配置");
            return FileResult.builder().isOk(false).msg("删除失败,Minio数据源配置默认bucket为空,请指定Bucket").build();
        }

        Iterable<Result<Item>> list= minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(folderPathPrefix).recursive(false).build());

        for(Result<Item> item:list){
            //是否文件夹
            try {
                String objectName=item.get().objectName();
                System.out.println( objectName);
                boolean isDir =item.get().isDir();
                if(isDir){//递归删除下一级
                    deleteDir(objectName);
                }
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
                log.info("删除文件bucket={},file={}",bucket,objectName);
            } catch (ConnectException e){
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio链接endpoint错误,"+e.getMessage()).build();
            } catch (IOException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio链接IO异常,"+e.getMessage()).build();
            } catch (XmlParserException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio链接XML异常,"+e.getMessage()).build();
            } catch (ServerException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio ServerL异常,"+e.getMessage()).build();
            } catch (NoSuchAlgorithmException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio Algorithm异常,"+e.getMessage()).build();
            } catch (InsufficientDataException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,"+e.getMessage()).build();
            } catch (InvalidKeyException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,InvalidKey错误"+e.getMessage()).build();
            } catch (InvalidResponseException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,Invalid响应错误"+e.getMessage()).build();
            } catch (ErrorResponseException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio错误认证信息"+e.getMessage()).build();
            } catch (InternalException e) {
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio Internal错误").build();
            }catch (Exception e){
                log.warn("删除失败{}",e.getMessage());
                return FileResult.builder().isOk(false).msg("删除失败,minio异常").build();
            }
        }

        return FileResult.builder().isOk(true).msg("成功").build();
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
            MinioClient minioClient=minioClientUtil.getMinioClient();
            String bucket=minioClientUtil.getDefaultBucket();
            if(minioClient==null){
                return false;
            }
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(objectName).build());
            if(response !=null){
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
        }
        return false;
    }
}
