package com.dk.plugin.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
            Set<Class> errorClass = new LinkedHashSet<>();
            // 从集合中取出判断是否是枚举，是否唯一
            for (Class<?> clazz : classes) {
                if (!clazz.isEnum()) {
                    return;
                }
                if (isEnumNotSuitRule(clazz)) {
                    errorClass.add(clazz);
                }
            }
            if (errorClass.size() > 0) {
                throw new MojoFailureException("存在没通过唯一性校验的枚举:" + errorClass.stream().map(z -> z.getName()).collect(Collectors.joining("\n")));
            }
        } catch (Exception e) {
            getLog().error("EnumCheckGoal error：", e);
            throw new MojoFailureException(e.getMessage());
        }
    }

    private boolean isEnumNotSuitRule(Class<?> clazz) {
        Class<Enum> enumClass = (Class<Enum>) clazz;
        Field[] fields = enumClass.getDeclaredFields();
        Enum[] constants = enumClass.getEnumConstants();
        if (null == constants || constants.length == 0 || null == fields || fields.length == 0) {
            return false;
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
            return false;
        }
        // 设置访问可见性
        checkField.setAccessible(true);

        for (Enum m : constants) {
            try {
                Object obj = checkField.get(m);
                if (uniqSet.contains(obj)) {
                    return true;
                } else {
                    uniqSet.add(obj);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return false;
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
        File[] dirFiles = dir.listFiles((file) ->
                // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
                (file.isDirectory()) || (file.getName().endsWith(".class"))
        );
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
                    getLog().info("clazz is => " + clazz);
                    classes.add(clazz);
                } catch (Exception e) {
                    getLog().error(String.format("EnumCheckGoal className:%s,path:%s,error：", className, path), e);
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

