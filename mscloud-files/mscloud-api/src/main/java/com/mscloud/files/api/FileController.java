package com.mscloud.files.api;

import com.mscloud.files.core.FileAllowSuffix;
import com.mscloud.files.core.FileUtil;
import com.mscloud.files.core.constants.FileDirType;
import com.mscloud.files.core.constants.StoreType;
import com.mscloud.files.core.pojo.FileInfo;
import com.mscloud.files.core.pojo.HttpResponse;
import com.mscloud.files.service.IFileService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * 文件存储
 */
@Slf4j
@RequestMapping("/file")
@RestController
public class FileController {

    @Autowired
    IFileService fileService;

    @Autowired
    FileAllowSuffix fileAllowSuffix;

    @ApiOperation(value = "上传文件", notes = "上传文件,根据网络URL地址")
    @PostMapping("/uploadFileByUrl")
    public HttpResponse uploadFile(@ApiParam("网络URL地址") @RequestParam("url") String url ,
                                   @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS,FTP") @RequestParam(value = "storeType",required = false) StoreType storeType,
                                   @ApiParam("是否重命名存储：默认为false，可为空") @RequestParam(value = "isRename",required = false) Boolean isRename,
                                   @ApiParam("路径目录格式:YEAR，MONTH，DAY三种,默认为DAY，EMPTY则不需要目录，ROOT指定目录") @RequestParam(value = "fileDirType",required = false) FileDirType fileDirType,
                                   @ApiParam("指定存储目录") @RequestParam(value = "fileDirPath",required = false) String fileDirPath,
                                   @ApiParam("是否返回临时链接，默认为否") @RequestParam(value = "isBackShareLink",required = false) Boolean isBackShareLink,
                                   @ApiParam("表单ID,不为空则插入表单和文件之间关系") @RequestParam( value = "formId",required = false) String formId,
                                   @ApiParam("模块标识") @RequestParam(value = "moduleName",required = false) String moduleName
    ){
        InputStream inputStream= null;
        try {
            URL fileUrl = new URL(url);

            URLConnection connection = fileUrl.openConnection();
            inputStream = connection.getInputStream();


            if(StringUtils.isNotBlank(formId)){
                if(StringUtils.isBlank(moduleName)){//绑定表单和文档关系，需要传入模块标识
                    return HttpResponse.error("绑定formId时，需要传递参数：moduleName模块标识");
                }
            }
            if(isBackShareLink==null){//默认否
                isBackShareLink=false;
            }
            String orignalFilename="";//文件原始名称
            String fileSuffix= FileUtil.splitUrlFileSuffix(fileUrl);//后缀
            if(StringUtils.isNotBlank(fileSuffix)){//统一转为小写
                fileSuffix =fileSuffix.toLowerCase();
            }
            if(this.fileAllowSuffix.getEnabled()){//开启后缀过滤
                boolean isAllow=this.fileAllowSuffix.isAllow(fileSuffix);
                if(! isAllow){//不允许上传
                    return HttpResponse.error(fileSuffix+"文件格式不允许上传");
                }
            }
            if(isRename==null){
                isRename=true;
            }
            //根据系统获取用户ID
            Long userId =0L;
            //文件基本信息
            FileInfo fileInfo=FileInfo.builder().fileSize(Long.parseLong(connection.getContentLength()+"")).contentType(connection.getContentType()).orignalName(orignalFilename).fileSuffix(fileSuffix).build();
            return this.fileService.uploadFile(inputStream,fileInfo,storeType,isRename,fileDirType,fileDirPath,formId,moduleName,isBackShareLink,userId);
        }catch (MalformedURLException e) {
            log.error("url地址解析错误:"+url);
            return HttpResponse.error("url地址解析错误");
        }catch (FileNotFoundException e) {
            log.error("url地址错误，文件不存在:"+url);
            return HttpResponse.error("url地址错误，文件不存在");
        } catch (IOException e) {
            log.error("url地址解析错误,{},:{}",url,e.getMessage());
        }
        return HttpResponse.error("url地址解析错误");
    }

    @ApiOperation(value = "上传文件", notes = "上传文件")
    @PostMapping("/uploadFile")
    public HttpResponse uploadFile(@ApiParam("文件流") @RequestParam("file") MultipartFile file ,
                                   @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS,FTP") @RequestParam(value = "storeType",required = false) StoreType storeType,
                                   @ApiParam("是否重命名存储：默认为false，可为空") @RequestParam(value = "isRename",required = false) Boolean isRename,
                                   @ApiParam("路径目录格式:YEAR，MONTH，DAY三种,默认为DAY，EMPTY则不需要目录，ROOT指定目录") @RequestParam(value = "fileDirType",required = false)FileDirType fileDirType,
                                   @ApiParam("指定存储目录") @RequestParam(value = "fileDirPath",required = false) String fileDirPath,
                                   @ApiParam("是否返回临时链接，默认为否") @RequestParam(value = "isBackShareLink",required = false) Boolean isBackShareLink,
                                   @ApiParam("表单ID,不为空则插入表单和文件之间关系") @RequestParam( value = "formId",required = false) String formId,
                                   @ApiParam("模块标识") @RequestParam(value = "moduleName",required = false) String moduleName
    ){


        if(StringUtils.isNotBlank(formId)){
            if(StringUtils.isBlank(moduleName)){//绑定表单和文档关系，需要传入模块标识
                return HttpResponse.error("绑定formId时，需要传递参数：moduleName模块标识");
            }
        }
        if(isBackShareLink==null){//默认否
            isBackShareLink=false;
        }
        String orignalFilename=file.getOriginalFilename();//文件原始名称
        String fileSuffix= FileUtil.splitFileSuffix(orignalFilename);//后缀
        if(StringUtils.isNotBlank(fileSuffix)){//统一转为小写
            fileSuffix =fileSuffix.toLowerCase();
        }
        if(this.fileAllowSuffix.getEnabled()){//开启后缀过滤
            boolean isAllow=this.fileAllowSuffix.isAllow(fileSuffix);
            if(! isAllow){//不允许上传
                return HttpResponse.error(fileSuffix+"文件格式不允许上传");
            }
        }
        if(isRename==null){
            isRename=true;
        }

        InputStream inputStream= null;
        try {
            Long userId =0L;
            inputStream = file.getInputStream();
            //文件基本信息
            FileInfo fileInfo=FileInfo.builder().fileSize(file.getSize()).contentType(file.getContentType()).orignalName(orignalFilename).fileSuffix(fileSuffix).build();
            return this.fileService.uploadFile(inputStream,fileInfo,storeType,isRename,fileDirType,fileDirPath,formId,moduleName,isBackShareLink,userId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  HttpResponse.error("文件错误");
    }
    @ApiOperation(value = "上传多个文件", notes = "上传多文件")
    @PostMapping("/uploadFiles")
    public HttpResponse uploadFiles(@ApiParam("文件流数组") @RequestParam("files") MultipartFile[] files ,
                                   @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS") @RequestParam(value = "storeType",required = false) StoreType storeType,
                                   @ApiParam("是否重命名存储：默认为false，可为空") @RequestParam(value = "isRename",required = false) Boolean isRename,
                                   @ApiParam("路径目录格式:YEAR，MONTH，DAY三种,默认为DAY，EMPTY则不需要目录，ROOT指定目录") @RequestParam(value = "fileDirType",required = false) FileDirType fileDirType,
                                   @ApiParam("指定存储目录") @RequestParam(value = "fileDirPath",required = false) String fileDirPath,
                                   @ApiParam("是否返回临时链接，默认为是") @RequestParam(value = "isBackShareLink",required = false) Boolean isBackShareLink,
                                   @ApiParam("表单ID,不为空则插入表单和文件之间关系") @RequestParam( value = "formId",required = false) String formId,
                                   @ApiParam("模块标识") @RequestParam(value = "moduleName",required = false) String moduleName
                                    ){

        if(StringUtils.isNotBlank(formId)){
            if(StringUtils.isBlank(moduleName)){//绑定表单和文档关系，需要传入模块标识
                return HttpResponse.error("绑定formId时，需要传递参数：moduleName模块标识");
            }
        }
        for(MultipartFile file:files){
            String orignalFilename=file.getOriginalFilename();//文件原始名称
            String fileSuffix= FileUtil.splitFileSuffix(orignalFilename);//后缀
            if(StringUtils.isNotBlank(fileSuffix)){//统一转为小写
                fileSuffix =fileSuffix.toLowerCase();
            }
            if(this.fileAllowSuffix.getEnabled()){//开启后缀过滤
                boolean isAllow=this.fileAllowSuffix.isAllow(fileSuffix);
                if(! isAllow){//不允许上传
                    return HttpResponse.error(fileSuffix+"文件格式不允许上传");
                }
            }
        }
        if(isRename==null){
            isRename=true;
        }
        if(isBackShareLink==null){//默认否
            isBackShareLink=false;
        }
        Long userId =0L;
        return this.fileService.uploadFiles(files,storeType,isRename,fileDirType,fileDirPath,formId,moduleName,isBackShareLink,userId);
    }

    @ApiOperation(value = "分片上传文件", notes = "分片上传文件，支持续传,前端需要将文件分片并循环顺序调用本接口")
    @PostMapping("/uploadPieceFile")
    public HttpResponse uploadPieceFile(@ApiParam("分片文件流") @RequestParam("file") MultipartFile file ,
                                           @ApiParam("文件名") @RequestParam(value = "fileName",required = true) String fileName,
                                           @ApiParam("分片索引") @RequestParam(value = "sliceIndex",required = true) int sliceIndex,
                                           @ApiParam("切片总数") @RequestParam(value = "totalPieces",required = true) int totalPieces,
                                           @ApiParam("文件MD5加密文本") @RequestParam(value = "md5String",required = true) String md5String,
                                           @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS") @RequestParam(value = "storeType",required = false) StoreType storeType,
                                           @ApiParam("是否重命名存储：默认为false，可为空") @RequestParam(value = "isRename",required = false) Boolean isRename,
                                           @ApiParam("路径目录格式:YEAR，MONTH，DAY三种,默认为DAY，EMPTY则不需要目录，ROOT指定目录") @RequestParam(value = "fileDirType",required = false)FileDirType fileDirType,
                                           @ApiParam("指定存储目录") @RequestParam(value = "fileDirPath",required = false) String fileDirPath,
                                           @ApiParam("是否返回临时链接，默认为否") @RequestParam(value = "isBackShareLink",required = false) Boolean isBackShareLink,
                                           @ApiParam("表单ID,不为空则插入表单和文件之间关系") @RequestParam( value = "formId",required = false) String formId,
                                           @ApiParam("模块标识") @RequestParam(value = "moduleName",required = false) String moduleName){


        if(StringUtils.isNotBlank(formId)){
            if(StringUtils.isBlank(moduleName)){//绑定表单和文档关系，需要传入模块标识
                return HttpResponse.error("绑定formId时，需要传递参数：moduleName模块标识");
            }
        }
        if(isBackShareLink==null){//默认否
            isBackShareLink=false;
        }
        String orignalFilename=file.getOriginalFilename();//文件原始名称
        String fileSuffix= FileUtil.splitFileSuffix(orignalFilename);//后缀
        if(StringUtils.isNotBlank(fileSuffix)){//统一转为小写
            fileSuffix =fileSuffix.toLowerCase();
        }
        if(this.fileAllowSuffix.getEnabled()){//开启后缀过滤
            boolean isAllow=this.fileAllowSuffix.isAllow(fileSuffix);
            if(! isAllow){//不允许上传
                return HttpResponse.error(fileSuffix+"文件格式不允许上传");
            }
        }
        if(isRename==null){
            isRename=true;
        }
        Long userId =0L;
        return this.fileService.uploadPieceFile(file,fileName,sliceIndex, totalPieces, md5String,storeType,isRename,fileDirType,fileDirPath,formId,moduleName,isBackShareLink,userId);

    }

    @ApiOperation(value = "下载文件", notes = "从存储桶下载文件")
    @GetMapping("/downloadFile")
    public void downloadFile(@ApiParam("文件存储路径,如oa/test.txt") @RequestParam("filePath") String filePath,
                                  @ApiParam("下载文件重命名名字") @RequestParam(value = "storeAsFilename",required = false) String storeAsFilename,
                                  @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS") @RequestParam(value = "storeType",required = false) StoreType storeType,
                                  HttpServletResponse response){

        fileService.downloadFile(filePath,storeAsFilename,storeType,response);
    }


    @ApiOperation(value = "获取文件的分享链接",notes = "获取文件的分享链接")
    @GetMapping("/getShareLink")
    public HttpResponse getShareLink(@ApiParam("文件存储路径") @RequestParam("filePath") String filePath,
                                     @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS")  @RequestParam(value = "storeType",required = false) StoreType storeType,
                                     @ApiParam("链接有效时长:单位为秒，为空则为默认值") @RequestParam(value = "expires",required = false) Integer expires){



        return fileService.getShareLink(filePath,expires,storeType);
    }

    @ApiOperation(value = "删除文件",notes = "删除文件")
    @PostMapping("/deleteFile")
    public HttpResponse deleteFile(@ApiParam("文件存储路径") @RequestParam("filePath") String filePath,
                                   @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS") @RequestParam(value = "storeType",required = false) StoreType storeType){

        Long systemId =0L;
        return fileService.deleteFile(filePath,storeType);
    }

    @ApiOperation(value = "删除目录及文件",notes = "删除目录及文件")
    @PostMapping("/deleteDirFile")
    public HttpResponse deleteDirFile(@ApiParam("文件存储路径") @RequestParam("dirPath") String dirPath,
                                      @ApiParam("存储方式：为空 MINIO默认，阿里：OSS，华为：OBS") @RequestParam(value = "storeType",required = false) StoreType storeType){

        Long systemId =0L;
        return fileService.deleteDirFile(dirPath,storeType);
    }

}
