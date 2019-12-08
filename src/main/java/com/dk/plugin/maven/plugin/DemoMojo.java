package com.dk.plugin.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author : kai.dai
 * @date : 2019-12-07 23:27
 */
@Mojo(name = "enum-check-goal", defaultPhase = LifecyclePhase.PACKAGE)
public class DemoMojo extends AbstractMojo {

    @Parameter
    private String msg;

    @Parameter
    private List<String> options;

    @Parameter(property = "args")
    private String args;

    @Parameter(
            defaultValue = "${project.build.sourceDirectory}",
            property = "sourceDirectory",
            required = true
    )
    private File sourceDirectory;

    @Parameter(
            defaultValue = "${project.build.directory}",
            property = "directory",
            required = true
    )
    private File directory;
    @Parameter(
            defaultValue = "${project.build.outputDirectory}",
            property = "outputDirectory",
            required = true
    )
    private File outputDirectory;

    @Parameter(
            property = "classPath",
            defaultValue = "${basedir}/target/classes/")
    private String classPath;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println("------12312-312-312-312-----");
        System.out.println("msg++++" + msg);
        System.out.println(options);
        System.out.println(args);
        System.out.println(null == sourceDirectory ? null : sourceDirectory.getAbsolutePath());
        System.out.println(null == directory ? null : directory.getAbsolutePath());
        System.out.println(null == outputDirectory ? null : outputDirectory.getAbsolutePath());
        System.out.println("------12312-312-312-312-----");
        Set<Class> sets = new HashSet<>();

        try {
            URL[] urls = new URL[1];
            urls[0] = new URL("file:" + classPath);
            URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            Enumeration<URL> dirs = urlClassLoader.getResources("");
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                if ("file".equals(url.getProtocol())) {
                    System.err.println("file类型的扫描");
                    // 获取包的物理路径
                    String packagePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packagePath, sets, urlClassLoader);
                }
            }
        } catch (Exception e) {
            getLog().error("DemoMojo log输出：" + e);
            throw new MojoFailureException(e.getMessage());
        }
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
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            @Override
            public boolean accept(File file) {
                return (file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(file.getAbsolutePath(),
                        classes, classLoader);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去 这里用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    // classes.add(Class.forName(packageName + '.' + className));
                    String path = file.getAbsolutePath();
                    String packageName = path.substring(path.indexOf("target/classes") + 15, path.lastIndexOf("/"));
                    packageName = packageName.replace("/", ".");
                    System.out.println("packageName+className => " + packageName + '.' + className);
//                    Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className);
                    Class<?> clazz = classLoader.loadClass(packageName + '.' + className);
                    System.out.println("clazz is => " + clazz);
                    // if (clazz.isEnum() && isDicClass(clazz)) {
                    //     dicClassesRealPath.add(file.getAbsolutePath());
                    //     // 获得枚举类详细信息
                    //     dicInformation.add(enumInformation(clazz));
                    // }
                    classes.add(clazz);
                } catch (Exception e) {
                    getLog().error("DemoMojo log输出：load class failed", e);
                }
            }
        }
    }
}

