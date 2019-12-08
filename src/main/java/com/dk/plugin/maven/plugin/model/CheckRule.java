package com.dk.plugin.maven.plugin.model;

import java.util.List;

/**
 * @author : kai.dai
 * @date : 2019-12-08 11:56
 */
public class CheckRule {
    /**
     * 具体校验哪些属性
     */
    List<String> checkFieldNames;

    boolean checkForce;

    public CheckRule(boolean checkForce) {
        this.checkForce = checkForce;
    }

    public List<String> getCheckFieldNames() {
        return checkFieldNames;
    }

    public void setCheckFieldNames(List<String> checkFieldNames) {
        this.checkFieldNames = checkFieldNames;
    }

    public boolean isCheckForce() {
        return checkForce;
    }

    public void setCheckForce(boolean checkForce) {
        this.checkForce = checkForce;
    }
}
