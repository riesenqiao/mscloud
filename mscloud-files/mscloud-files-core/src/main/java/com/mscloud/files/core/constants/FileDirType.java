package com.mscloud.files.core.constants;

public enum  FileDirType {
    EMPTY("EMPTY","没有目录"),
    ROOT("ROOT","指定目录"),
    YEAR("yyyy","年"),
    MONTH("yyyy/MM","年月"),
    DAY("yyyy/MM/dd","年月日");


    private final String value;

    /** 描述 */
    private final String desc;

    private FileDirType(String value,String desc) {
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
