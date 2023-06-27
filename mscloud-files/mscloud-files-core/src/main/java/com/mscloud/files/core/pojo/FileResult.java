package com.mscloud.files.core.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件处理结果
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FileResult implements Serializable{

    private static final long serialVersionUID = -8466503272672401613L;
    boolean isOk=true;

    String msg="上传成功";
    /**
     * 分片序号
     */
    int sliceIndex=-2;

    /**
     * 桶名称
     */
    private String bucketName;
    /**
     * 直接链接
     */
    private String filePath;
    /**
     * 临时链接
     */
    private String shareLink;
    /**
     * 文件类型
     */
    private String contentType;
    /**
     * 文件大小,单位为字节byte
     */
    private Long fileSize;

    /**
     * 文件后缀
     */
    private String fileSuffix;
    /**
    /**
     * 文件原有名称
     */
    private String orignalName;
    /**
     * 存储名称
     */
    private String storeName;

    /**
     * 存储方式
     */
    private String storeType;

    /**
     * 表单ID
     */
    private String formId;

    /**
     * 表单类型
     */
    private String moduleName;

}
