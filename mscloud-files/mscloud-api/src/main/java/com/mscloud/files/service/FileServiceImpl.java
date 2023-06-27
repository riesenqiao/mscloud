package com.mscloud.files.service;


import cn.hutool.json.JSONUtil;
import com.mscloud.files.core.constants.FileDirType;
import com.mscloud.files.core.constants.StoreType;
import com.mscloud.files.core.pojo.FileInfo;
import com.mscloud.files.core.pojo.FileResult;
import com.mscloud.files.core.pojo.HttpResponse;
import com.mscloud.files.ftp.service.FtpService;
import com.mscloud.files.minio.service.MinioService;
import com.mscloud.files.oss.service.OssService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 各存储方式的文件处理实现类
 */
@Service
public class FileServiceImpl implements IFileService {

    @Autowired
    MinioService minioService;

    @Autowired
    OssService ossService;

    @Autowired
    FtpService ftpService;


    /**
     * 上传文件
     * @param inputStream 文件流
     * @param fileInfo 文件信息
     * @param storeType 存储方式
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定存储目录
     * @param formId 表单ID,不为空则插入表单和文件之间关系
     * @param moduleName 模块标识
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    @Override
    public HttpResponse uploadFile(InputStream inputStream, FileInfo fileInfo, StoreType storeType, boolean isRename, FileDirType fileDirType, String fileDirPath, String formId, String moduleName, boolean isBackShareLink, Long userId) {

        if(storeType==null){//默认MINIO方式
            storeType= StoreType.MINIO;
        }

        FileResult fileResult=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            Map<String,String> tags=new HashMap<>();
            if(StringUtils.isNotBlank(formId)){
                tags.put("formId",formId);
            }
            if(StringUtils.isNotBlank(moduleName)){
                tags.put("moduleName",moduleName);
            }
            if(userId !=null){
                tags.put("userId",userId+"");
            }
            fileResult=this.minioService.uploadFile(inputStream,fileInfo,isRename,fileDirType,fileDirPath,isBackShareLink,tags);

        }else if( storeType== StoreType.OSS){//阿里OSS方式
            Map<String,String> tags=new HashMap<>();
            if(StringUtils.isNotBlank(formId)){
                tags.put("formId",formId);
            }
            if(StringUtils.isNotBlank(moduleName)){
                tags.put("moduleName",moduleName);
            }
            if(userId !=null){
                tags.put("userId",userId+"");
            }
            fileResult=this.ossService.uploadFile(inputStream,fileInfo,isRename,fileDirType,fileDirPath,isBackShareLink,tags);

        }else if( storeType== StoreType.OBS){//华为OBS方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            fileResult=this.ftpService.uploadFile(inputStream,fileInfo,isRename,fileDirType,fileDirPath,isBackShareLink);
        }else{
            return HttpResponse.error("存储方式不支持");
        }

        if(fileResult==null){
            return HttpResponse.error("存储方式不支持");
        }else {
            if(fileResult.isOk()){//上传成功
                return HttpResponse.success("上传成功",fileResult);

            }else{//失败
                return HttpResponse.error(fileResult.getMsg());
            }
        }
    }
    /**
     * 上传文件至指定的桶Bucket
     * @param files 多个文件流
     * @param storeType 存储方式
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定存储目录
     * @param formId 表单ID,不为空则插入表单和文件之间关系
     * @param moduleName 模块标识
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    @Override
    public HttpResponse uploadFiles(MultipartFile[] files, StoreType storeType, boolean isRename, FileDirType fileDirType,String fileDirPath,String formId,String moduleName,boolean isBackShareLink,Long userId) {
        if(storeType==null){//默认MINIO方式
            storeType=StoreType.MINIO;
        }
        List<FileResult> fileResults=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            Map<String,String> tags=new HashMap<>();
            if(StringUtils.isNotBlank(formId)){
                tags.put("formId",formId);
            }
            if(StringUtils.isNotBlank(moduleName)){
                tags.put("moduleName",moduleName);
            }
            if(userId !=null){
                tags.put("userId",userId+"");
            }
            fileResults=this.minioService.uploadFiles(files,isRename,fileDirType,fileDirPath,isBackShareLink,tags);
        }else if( storeType== StoreType.OSS){//阿里OSS方式
            Map<String,String> tags=new HashMap<>();
            if(StringUtils.isNotBlank(formId)){
                tags.put("formId",formId);
            }
            if(StringUtils.isNotBlank(moduleName)){
                tags.put("moduleName",moduleName);
            }
            if(userId !=null){
                tags.put("userId",userId+"");
            }
            fileResults=this.ossService.uploadFiles(files,isRename,fileDirType,fileDirPath,isBackShareLink,tags);
        }else if( storeType== StoreType.OBS){//华为OBS方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            fileResults=this.ftpService.uploadFiles(files,isRename,fileDirType,fileDirPath,isBackShareLink);
        }else{
            return HttpResponse.error("存储方式不支持");
        }

        if(fileResults==null){
            return HttpResponse.error("存储方式不支持");
        }else {

            return HttpResponse.success("上传成功",fileResults);
        }
    }
    /**
     * 分片上传文件
     * @param file 分片文件流
     * @param fileName 文件名称，由于分片文件获取不了文件名，需要指定文件名进行合并存储，为空则默认随机名字
     * @param sliceIndex 分片索引
     * @param totalPieces 切片总数
     * @param md5String 文件MD5加密文本
     * @param storeType 存储方式
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定存储目录
     * @param formId 表单ID,不为空则插入表单和文件之间关系
     * @param moduleName 模块标识
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    @Override
    public HttpResponse uploadPieceFile(MultipartFile file, String fileName, int sliceIndex, int totalPieces, String md5String, StoreType storeType, boolean isRename, FileDirType fileDirType, String fileDirPath,String formId, String moduleName, boolean isBackShareLink,Long userId) {

        if(storeType==null){//默认MINIO方式
            storeType=StoreType.MINIO;
        }
        FileResult fileResult=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            Map<String,String> tags=new HashMap<>();
            if(StringUtils.isNotBlank(formId)){
                tags.put("formId",formId);
            }
            if(StringUtils.isNotBlank(moduleName)){
                tags.put("moduleName",moduleName);
            }
            if(userId !=null){
                tags.put("userId",userId+"");
            }
            fileResult=this.minioService.uploadPieceFile(file,fileName,sliceIndex,totalPieces,md5String,isRename,fileDirType,fileDirPath,isBackShareLink,tags);

        }else if( storeType== StoreType.OSS){//阿里OSS方式

            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.OBS){//华为OBS方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            return HttpResponse.error("存储方式开发中");
        }else{
            return HttpResponse.error("存储方式不支持");
        }

        if(fileResult==null){
            return HttpResponse.error("存储方式不支持");
        }else {
            if(fileResult.isOk()){//上传成功

                return HttpResponse.success(fileResult);

            }else{//失败
                return HttpResponse.error(fileResult.getMsg());
            }
        }
    }

    /**
     * 下载文件
     * @param filePath 指定下载文件在Bucket中的存储全路径
     * @param storeAsFilename 下载后指定的命名文字，为空则和存储的名称一致
     * @param storeType 目录格式，默认年月日格式目录
     * @param storeType 存储方式
     * @return
     */
    @Override
    public void downloadFile(String filePath, String storeAsFilename, StoreType storeType, HttpServletResponse response) {
        if(storeType==null){//默认MINIO方式
            storeType=StoreType.MINIO;
        }
        HttpResponse httpResponse=null;
        FileResult fileResult=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            fileResult=this.minioService.downloadFile(filePath,storeAsFilename,response);
        }else if( storeType== StoreType.OSS){//阿里OSS方式
            fileResult=this.ossService.downloadFile(filePath,storeAsFilename,response);
        }else if( storeType== StoreType.OBS){//华为OBS方式
            httpResponse= HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            httpResponse= HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            fileResult=this.ftpService.downloadFile(filePath,storeAsFilename,response);
        }else{
            httpResponse= HttpResponse.error("存储方式不支持");
        }

        if(fileResult==null){
            httpResponse= HttpResponse.error("存储方式不支持");
            //下载文件失败时，需要通过流的方式返回
            repsonseStream(  httpResponse,response);
        }else {
            if(fileResult.isOk()){//下载成功
                httpResponse= HttpResponse.success();
            }else{//失败
                httpResponse= HttpResponse.error(fileResult.getMsg());
                //下载文件失败时，需要通过流的方式返回
                 repsonseStream(  httpResponse,response);
            }
        }

    }
    private void repsonseStream(HttpResponse httpResponse,HttpServletResponse response){
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        PrintWriter pw= null;
        try {
            pw = response.getWriter();
            String body= JSONUtil.toJsonStr(httpResponse);
            pw.write(body);
            pw.flush();
            pw.close();
        } catch (IOException e) {
        }finally {
            if(pw !=null){
                pw.close();
            }
        }
    }
    /**
     * 获取文件的分享链接
     * @param filePath 指定下载文件在Bucket中的存储全路径
     * @param expires 链接有效时长:单位为秒，为空则为默认值
     * @param storeType 存储方式
     * @return
     */
    @Override
    public HttpResponse getShareLink(String filePath, Integer expires, StoreType storeType) {
        if(storeType==null){//默认MINIO方式
            storeType=StoreType.MINIO;
        }
        FileResult fileResult=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            fileResult=this.minioService.getShareLink(filePath,expires);
        }else if( storeType== StoreType.OSS){//阿里OSS方式
            fileResult=this.ossService.getShareLink(filePath,expires);
        }else if( storeType== StoreType.OBS){//华为OBS方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            return HttpResponse.error("存储方式开发中");
        }else{
            return HttpResponse.error("存储方式不支持");
        }

        if(fileResult==null){
            return HttpResponse.error("存储方式不支持");
        }else {
            if(fileResult.isOk()){//下载成功
                return HttpResponse.success("成功",fileResult.getFilePath());
            }else{//失败
                return HttpResponse.error(fileResult.getMsg());
            }
        }
    }

    /**
     * 删除多个文件
     * @param filePaths 文件在Bucket中的存储全路径 数组
     * @param storeType 存储方式
     * @return
     */
    @Override
    public HttpResponse deleteFiles(List<String> filePaths, StoreType storeType){
        HttpResponse httpResponse=null;

        for(String filePath:filePaths){

            httpResponse= deleteFile(filePath,storeType);
        }
        return httpResponse;
    }
    /**
     * 删除文件
     * @param filePath 文件在Bucket中的存储全路径
     * @param storeType 存储方式
     * @return
     */
    @Override
    public HttpResponse deleteFile(String filePath, StoreType storeType) {
        if(storeType==null){//默认MINIO方式
            storeType=StoreType.MINIO;
        }
        FileResult fileResult=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            fileResult=this.minioService.deleteFile(filePath);
        }else if( storeType== StoreType.OSS){//阿里OSS方式
            fileResult=this.ossService.deleteFile(filePath);
        }else if( storeType== StoreType.OBS){//华为OBS方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            return HttpResponse.error("存储方式开发中");
        }else{
            return HttpResponse.error("存储方式不支持");
        }

        if(fileResult==null){
            return HttpResponse.error("存储方式不支持");
        }else {
            if(fileResult.isOk()){//删除成功
                return HttpResponse.success();//返回链接
            }else{//失败
                return HttpResponse.error(fileResult.getMsg());
            }
        }
    }
    /**
     * 删除目录及文件，本方法会清空目录的所有文件，请谨慎使用
     * @param folderPathPrefix 文件在Bucket中的存储全路径
     * @param storeType 存储方式
     * @return
     */
    @Override
    public HttpResponse deleteDirFile(String folderPathPrefix, StoreType storeType) {
        if(storeType==null){//默认MINIO方式
            storeType=StoreType.MINIO;
        }
        FileResult fileResult=null;
        if( storeType== StoreType.MINIO){//MINIO方式
            fileResult=this.minioService.deleteDir(folderPathPrefix);
        }else if( storeType== StoreType.OSS){//阿里OSS方式
            fileResult=this.ossService.deleteDir(folderPathPrefix);
        }else if( storeType== StoreType.OBS){//华为OBS方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.LOCAL){//本地服务器方式
            return HttpResponse.error("存储方式开发中");
        }else if( storeType== StoreType.FTP){//FTP方式
            return HttpResponse.error("存储方式开发中");
        }else{
            return HttpResponse.error("存储方式不支持");
        }

        if(fileResult==null){
            return HttpResponse.error("存储方式不支持");
        }else {
            if(fileResult.isOk()){//删除成功
                return HttpResponse.success();//返回链接
            }else{//失败
                return HttpResponse.error(fileResult.getMsg());
            }
        }
    }
}
