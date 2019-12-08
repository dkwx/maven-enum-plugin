package com.dk.plugin.maven;


public enum EnumDemo {
    Normal(2),
    Error(2),
    Error2(1),
    Error3(1),
    Error4(2),
    ;
    private int code;

    EnumDemo(int code) {
        this.code = code;
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
