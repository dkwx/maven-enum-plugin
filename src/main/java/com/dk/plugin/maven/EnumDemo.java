package com.dk.plugin.maven;

import com.dk.plugin.maven.plugin.annotation.EnumCheckCondition;

@EnumCheckCondition(uniqFields = {"desc"})
public enum EnumDemo {
    Normal(2, "1"),
    Error(2, "1"),
    Error2(1, "1"),
    Error3(1, "1"),
    Error4(2, "1"),
    ;
    private int code;
    private String desc;

    EnumDemo(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public EnumDemo getByCode(int code) {
        for (EnumDemo enumDemo : EnumDemo.values()) {
            if (enumDemo.code == code) {
                return enumDemo;
            }
        }
        return null;
    }

}
