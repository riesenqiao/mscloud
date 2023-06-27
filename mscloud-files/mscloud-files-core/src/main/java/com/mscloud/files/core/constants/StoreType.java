package com.mscloud.files.core.constants;

/**
 * 文件存储方式
 */
public enum StoreType {
    MINIO("MINIO","MINIO"),
    OSS("OSS","阿里OSS"),
    OBS("OBS","华为OBS"),
    FTP("FTP","FTP"),
    LOCAL("LOCAL","本地服务器");


    private final String value;

    /** 描述 */
    private final String desc;

    private StoreType(String value,String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * Return the integer value of this status code.
     */
    public String value() {
        return this.value;
    }

    public String desc() {
        return this.desc;
    }

    public String toString() {
        return this.value.toString();
    }
}
