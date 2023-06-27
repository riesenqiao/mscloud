package com.mscloud.files.service;

import com.mscloud.files.core.constants.FileDirType;
import com.mscloud.files.core.constants.StoreType;
import com.mscloud.files.core.pojo.FileInfo;
import com.mscloud.files.core.pojo.HttpResponse;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 文件存储公共接口
 */
public interface IFileService {


    /**
     * 上传文件
     * @param inputStream 文件流
     * @param fileInfo 文件基本信息
     * @param storeType 存储方式
     * @param isRename 是否重命名，默认为false， true则使用UUID重命名存储
     * @param fileDirType 目录格式，默认年月日格式目录
     * @param fileDirPath 指定存储目录
     * @param formId 表单ID,不为空则插入表单和文件之间关系
     * @param moduleName 模块标识
     * @param isBackShareLink 是否返回临时链接，默认为是
     * @return
     */
    public HttpResponse uploadFile(InputStream inputStream, FileInfo fileInfo, StoreType storeType, boolean isRename, FileDirType fileDirType, String fileDirPath, String formId, String moduleName, boolean isBackShareLink, Long userId);


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
    public HttpResponse uploadFiles(MultipartFile[] files, StoreType storeType,  boolean isRename, FileDirType fileDirType, String fileDirPath, String formId, String moduleName, boolean isBackShareLink, Long userId);


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
    public HttpResponse uploadPieceFile(MultipartFile file, String fileName, int sliceIndex, int totalPieces, String md5String, StoreType storeType,  boolean isRename, FileDirType fileDirType, String fileDirPath, String formId, String moduleName, boolean isBackShareLink, Long userId);



    /**
     * 下载文件
     * @param filePath 指定下载文件在Bucket中的存储全路径
     * @param storeAsFilename 下载后指定的命名文字，为空则和存储的名称一致
     * @param storeType 存储方式
     * @return
     */
    public void downloadFile(String filePath, String storeAsFilename, StoreType storeType,HttpServletResponse response);
    /**
     * 获取文件分享链接
     * @param filePath 指定下载文件在Bucket中的存储全路径
     * @param expires 链接有效时长:单位为秒，为空则为默认值
     * @param storeType 存储方式
     * @return
     */
    public HttpResponse getShareLink(String filePath, Integer expires, StoreType storeType);
    /**
     * 删除文件
     * @param filePath 文件在Bucket中的存储全路径
     * @param storeType 存储方式
     * @return
     */
    HttpResponse deleteFile(String filePath, StoreType storeType);

    /**
     * 删除多个文件
     * @param filePaths 文件在Bucket中的存储全路径 数组
     * @param storeType 存储方式
     * @return
     */
    HttpResponse deleteFiles(List<String> filePaths, StoreType storeType);

    /**
     * 删除目录及文件
     * @param dirPath 目录
     * @param storeType 存储方式
     * @return
     */
    HttpResponse deleteDirFile(String dirPath, StoreType storeType);


}
