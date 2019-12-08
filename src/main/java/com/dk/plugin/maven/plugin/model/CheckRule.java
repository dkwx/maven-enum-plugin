package com.dk.plugin.maven.plugin.model;

/**
 * @author : kai.dai
 * @date : 2019-12-08 11:56
 */
public class CheckRule {

    boolean checkForce;

    public CheckRule(boolean checkForce) {
        this.checkForce = checkForce;
    }


    public boolean isCheckForce() {
        return checkForce;
    }

    public void setCheckForce(boolean checkForce) {
        this.checkForce = checkForce;
    }
}
