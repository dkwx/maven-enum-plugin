package com.dk.plugin.maven.plugin.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author : kai.dai
 * @date : 2019-12-08 11:58
 */
public class CheckResponse {
    /**
     * 枚举的类名
     */
    Class errClazz;
    /**
     * 枚举常量的名字
     */
    Set<String> enumConstNames = new LinkedHashSet<>();
    /**
     * 具体哪些属性重复
     */
    Set<String> fieldNames = new LinkedHashSet<>();

    public CheckResponse(Class errClazz) {
        this.errClazz = errClazz;
    }

    public Class getErrClazz() {
        return errClazz;
    }

    public void setErrClazz(Class errClazz) {
        this.errClazz = errClazz;
    }

    public Set<String> getEnumConstNames() {
        return enumConstNames;
    }


    public Set<String> getFieldNames() {
        return fieldNames;
    }


    public void addEnumConstName(String name) {
        this.enumConstNames.add(name);
    }

    public void addFieldName(String name) {
        this.fieldNames.add(name);
    }
}
