package com.mscloud.files.core.pojo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件信息
 */
@Builder
@Data
public class FileInfo {

    /**
     /**
     * 文件原有名称
     */
    private String orignalName;

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
}
