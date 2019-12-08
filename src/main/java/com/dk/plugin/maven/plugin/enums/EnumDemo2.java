package com.dk.plugin.maven.plugin.enums;

public enum EnumDemo2 {
    Normal(2, "1"),
    Error(2, "1"),
    Error2(1, "1"),
    Error3(1, "1"),
    Error4(2, "1"),
    ;
    private int code;
    private String desc;

    EnumDemo2(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public EnumDemo2 getByCode(int code) {
        for (EnumDemo2 enumDemo : EnumDemo2.values()) {
            if (enumDemo.code == code) {
                return enumDemo;
            }
        }
        return null;
    }

}
