package com.dk.plugin.maven.plugin;

import com.dk.plugin.maven.plugin.model.CheckResponse;
import com.dk.plugin.maven.plugin.model.CheckRule;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author : kai.dai
 * @date : 2019-12-07 23:27
 */
@Mojo(name = "enum-check-goal", defaultPhase = LifecyclePhase.PACKAGE)
public class EnumCheckGoal extends AbstractMojo {

    @Parameter(
            property = "classPath",
            defaultValue = "${basedir}/target/classes/")
    private String classPath;


    @Parameter(
            property = "checkForce",
            defaultValue = "true")
    private boolean checkForce;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("------enum-check-goal-start-----");
        Set<Class> classes = new LinkedHashSet<>();
        try {
            URL[] urls = new URL[1];
            urls[0] = new URL("file:" + classPath);
            URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            Enumeration<URL> dirs = urlClassLoader.getResources("");
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                if ("file".equals(url.getProtocol())) {
                    // 获取包的物理路径
                    String packagePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packagePath, classes, urlClassLoader);
                }
            }
            List<CheckResponse> checkResponseList = new ArrayList<>();
            CheckRule checkRule = new CheckRule(checkForce);
            // 从集合中取出判断是否是枚举，是否唯一
            for (Class<?> clazz : classes) {
                if (!clazz.isEnum()) {
                    continue;
                }
                CheckResponse response = isEnumNotSuitRule(clazz, checkRule);
                if (null != response) {
                    checkResponseList.add(response);
                }
            }
            if (checkResponseList.size() > 0) {
                soutResponse(checkResponseList, checkRule);
            }
        } catch (Exception e) {
            getLog().error("EnumCheckGoal error：", e);
            throw new MojoFailureException(e.getMessage());
        }
    }

    private void soutResponse(List<CheckResponse> checkResponseList, CheckRule checkRule) throws MojoFailureException {
        if (null == checkResponseList || checkResponseList.size() == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder("存在没通过唯一性校验的枚举:\n");
        // 不使用java8语法，否则各个插件都要升级
        for (CheckResponse response : checkResponseList) {
            sb.append("class:" + response.getErrClazz().getName());
            sb.append(",consts:" + response.getEnumConstNames());
            sb.append(",fields:" + response.getFieldNames());
            sb.append("\n");
        }
        if (checkRule.isCheckForce()) {
            throw new MojoFailureException(sb.toString());
        } else {
            getLog().warn(sb.toString());
        }
    }

    private CheckResponse isEnumNotSuitRule(Class<?> clazz, CheckRule checkRule) {
        Class<Enum> enumClass = (Class<Enum>) clazz;
        Field[] fields = enumClass.getDeclaredFields();
        Enum[] constants = enumClass.getEnumConstants();
        if (null == constants || constants.length == 0 || null == fields || fields.length == 0) {
            return null;
        }
        Set<Object> uniqSet = new HashSet<>();
        Field checkField = null;
        for (Field field : fields) {
            if (!field.getType().equals(enumClass)) {
                checkField = field;
                break;
            }
        }
        if (null == checkField) {
            getLog().warn("找不到合适的比较Field");
            return null;
        }
        // 设置访问可见性
        checkField.setAccessible(true);
        boolean flag = false;
        CheckResponse response = new CheckResponse(enumClass);
        for (Enum m : constants) {
            try {
                Object obj = checkField.get(m);
                if (uniqSet.contains(obj)) {
                    flag = true;
                    response.addEnumConstName(m.name());
                    response.addFieldName(checkField.getName());
                } else {
                    uniqSet.add(obj);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return flag ? response : null;
    }

    private void findAndAddClassesInPackageByFile(String packagePath, Set<Class> classes, URLClassLoader classLoader) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            getLog().warn("用户定义包名 " + packagePath + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirFiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        if (null == dirFiles || dirFiles.length == 0) {
            return;
        }
        // 循环所有文件
        for (File file : dirFiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(file.getAbsolutePath(),
                        classes, classLoader);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                String path = file.getAbsolutePath();
                try {
                    // 添加到集合中去 这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    String packageName = "";
                    String classPath = className;
                    int start = path.indexOf("target/classes") + 15;
                    int end = path.lastIndexOf("/");
                    // 如果end <= start 则表明，类包路径是空
                    if (end > start) {
                        packageName = path.substring(start, end);
                        packageName = packageName.replace("/", ".");
                        classPath = packageName + '.' + className;
                    }

                    getLog().info("classPath => " + classPath);
                    Class<?> clazz = classLoader.loadClass(classPath);
                    classes.add(clazz);
                } catch (Exception e) {
                    getLog().error(String.format("EnumCheckGoal className:%s,path:%s,error：", className, path), e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

