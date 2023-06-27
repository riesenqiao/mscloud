package com.mscloud.files.ftp.service;

import com.mscloud.files.core.FileUtil;
import com.mscloud.files.core.constants.FileDirType;
import com.mscloud.files.core.constants.StoreType;
import com.mscloud.files.core.pojo.FileInfo;
import com.mscloud.files.core.pojo.FileResult;
import com.mscloud.files.ftp.util.FtpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FtpService {



    @Autowired
    private FtpClientUtil ftpClientUtil;


    /**
     * 上传单个文件
     * @param inputStream 文件流
     * @param fileInfo 文件信息
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定目录
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    public FileResult uploadFile(InputStream inputStream, FileInfo fileInfo, boolean isRename, FileDirType fileDirType, String fileDirPath,boolean isBackShareLink){

        FTPClient ftpClient= null;
        try {
            ftpClient = ftpClientUtil.open();
            if(ftpClient==null){
                return FileResult.builder().isOk(false).msg("上传失败,找不到FTP数据源配置").build();
            }
        } catch (IOException e) {
            return FileResult.builder().isOk(false).msg("上传失败,连接FTP失败").build();
        }

        String ip=ftpClientUtil.getIp();
        String username=ftpClientUtil.getUsername();
        String password=ftpClientUtil.getPassword();
        String workDir=ftpClientUtil.getWorkDir();

        try {

            Long fileSize=fileInfo.getFileSize();//文件大小字节
            String contentType=fileInfo.getContentType();//文件类型
            String orignalFilename=fileInfo.getOrignalName();//文件原始名称
            String fileSuffix= fileInfo.getFileSuffix();//后缀
            String reName= UUID.randomUUID().toString().replaceAll("-","")+ (fileSuffix==null?"":fileSuffix);
            String  newStoreName=(isRename?reName:orignalFilename);

            String dir="";
            //创建目录格式
            if(fileDirType==null|| ! fileDirType.equals(FileDirType.EMPTY)){
                //拼接目录
                dir= FileUtil.dirName(fileDirType,fileDirPath,false);
            }else{
                dir=fileDirPath;
            }
            if(StringUtils.isNotBlank(workDir)){
                dir =workDir+"/"+dir;
            }else{
                dir ="/"+dir;
            }
            //切换路径 创建路径,只能创建一级目录
            ftpClient.makeDirectory(dir);
            //FTPClient工作目录必须切换到文件所在的目录
            ftpClient.changeWorkingDirectory(dir);

            ftpClient.enterLocalPassiveMode();
            //设置缓冲
            ftpClient.setBufferSize(1024 * 1024 * 20);
            //保持连接
            ftpClient.setKeepAlive(true);
            //上传FTP
            boolean isSuccess = ftpClient.storeFile(new String(newStoreName.getBytes("utf-8"),"iso-8859-1"), inputStream);
            inputStream.close();
            String shareLink ="";
            if(isSuccess) {//上传成功
                String storeName="";
                if(dir.startsWith("/")){
                    storeName=dir+"/"+newStoreName;
                }else{
                    storeName="/"+dir+"/"+newStoreName;
                }

                if(isBackShareLink){
                    shareLink ="ftp://"+username+":"+password+"@"+ip+storeName;
                }
                return FileResult.builder().isOk(true).msg("上传成功").storeType(StoreType.FTP.value()).shareLink(shareLink).fileSize(fileSize).contentType(contentType).storeName(storeName).orignalName(orignalFilename).fileSuffix(fileSuffix).build();
            }else {//上传失败
                return FileResult.builder().isOk(false).msg("上传失败,FTP失败").build();
            }


        } catch (IOException e) {
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,FTP链接IO异常,"+e.getMessage()).build();

        }catch (Exception e){
            log.warn("上传失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("上传失败,FTP异常").build();
        }finally {
            try {
                if(inputStream !=null){
                    inputStream.close();
                }
                if(ftpClientUtil !=null){
                    ftpClientUtil.close();
                }

            } catch (IOException e) {
            }
        }
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
    public List<FileResult> uploadFiles(MultipartFile[] multipartFiles, boolean isRename, FileDirType fileDirType, String fileDirPath, boolean isBackShareLink){
        List<FileResult> list=new ArrayList<>();
        for(MultipartFile file:multipartFiles){
            //文件基本信息
            String orignalFilename=file.getOriginalFilename();//文件原始名称
            String fileSuffix= FileUtil.splitFileSuffix(orignalFilename);//后缀
            FileInfo fileInfo=FileInfo.builder().fileSize(file.getSize()).contentType(file.getContentType()).orignalName(orignalFilename).fileSuffix(fileSuffix).build();
            InputStream inputStream= null;
            try {
                inputStream = file.getInputStream();
                FileResult fileResult= this.uploadFile(inputStream,fileInfo,isRename,fileDirType,fileDirPath,isBackShareLink);
                list.add(fileResult);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    /**
     * 下载文件
     * @param filePath 指定下载的文件全路径 如： oa/test.txt
     * @param storeAsFilename 下载后文件名
     * @param response  响应文件流
     */
    public FileResult downloadFile(String filePath,String storeAsFilename,HttpServletResponse response){

        FTPClient ftpClient= null;
        try {
            ftpClient = ftpClientUtil.open();
            if(ftpClient==null){
                return FileResult.builder().isOk(false).msg("上传失败,找不到FTP数据源配置").build();
            }
        } catch (IOException e) {
            return FileResult.builder().isOk(false).msg("上传失败,连接FTP失败").build();
        }
        BufferedInputStream inputStream = null;
        OutputStream outputStream =null;
        try{
            /**没有对应路径时，FTPFile[] 大小为0，不会为null*/
            FTPFile[] ftpFiles = ftpClient.listFiles(filePath);
            FTPFile ftpFile = null;
            if (ftpFiles.length >= 1) {
                ftpFile = ftpFiles[0];
            }
            if (ftpFile != null && ftpFile.isFile()) {
                String workDir = filePath.substring(0, filePath.lastIndexOf("/"));
                if (StringUtils.isBlank(workDir)) {
                    workDir = "/";
                }
                /**文件下载前，FTPClient工作目录必须切换到文件所在的目录，否则下载失败
                 * "/" 表示用户根目录*/
                ftpClient.changeWorkingDirectory(workDir);
                inputStream=new BufferedInputStream(ftpClient.retrieveFileStream(ftpFile.getName()));
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
            }
            return FileResult.builder().isOk(false).msg("下载失败,找不到文件").build();
        }catch (IOException e) {
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,FTP链接IO异常,"+e.getMessage()).build();

        }catch (Exception e){
            log.warn("下载失败{}",e.getMessage());
            return FileResult.builder().isOk(false).msg("下载失败,FTP异常").build();
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
}
