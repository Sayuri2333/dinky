/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.classloader;

import org.dinky.context.FlinkUdfPathContextHolder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @since 0.7.0
 */
@Slf4j
public class DinkyClassLoader extends URLClassLoader {

    FlinkUdfPathContextHolder udfPathContextHolder = new FlinkUdfPathContextHolder();

    public DinkyClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, null);
    }

    public DinkyClassLoader(Collection<File> fileSet, ClassLoader parent) {
        this(convertFilesToUrls(fileSet), parent, null);
    }

    public DinkyClassLoader(URL[] urls) {
        this(urls, Thread.currentThread().getContextClassLoader(), null);
    }

    public DinkyClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        // 调用父类的构造函数
        //URL[] urls:
        // 这个参数是一个URL数组，每个URL指向一个要加载类和资源的位置。这些位置通常是JAR文件或目录，也可以是其他任何可通过URL访问的资源。
        // URLClassLoader将从这些URL指定的位置加载类和资源。

        //ClassLoader parent:
        // 这个参数是一个父类加载器。在Java中，类加载器有一个层级关系，每个类加载器除了根类加载器外都有一个父类加载器。
        // 当URLClassLoader无法加载某个类时，它会委托给这个父类加载器尝试加载该类。
        // 这是Java类加载机制中的“双亲委派模型”，用于确保Java核心库的类优先于应用程序定义的类加载，并维护Java运行时环境的安全性和完整性。

        //URLStreamHandlerFactory factory:
        // 这个参数是一个用于创建URL流处理器的工厂。URL流处理器用于处理特定协议的URL连接（例如，HTTP、HTTPS、FTP）。
        // 如果提供了这个参数，URLClassLoader将使用这个工厂来创建URL流处理器，以便打开和处理URL指向的资源。
        // 这个参数通常用于处理特殊的URL协议，或者在需要自定义URL处理逻辑时使用。
        super(urls, parent, factory);
    }

    // class factory method with urls parameters
    public static DinkyClassLoader build(URL... urls) {
        return new DinkyClassLoader(urls);
    }

    public static DinkyClassLoader build(URL[] urls, ClassLoader parent) {
        return new DinkyClassLoader(urls, parent);
    }

    public static DinkyClassLoader build(ClassLoader parent) {
        return new DinkyClassLoader(new URL[] {}, parent);
    }

    // return udfPathContextHolder
    public FlinkUdfPathContextHolder getUdfPathContextHolder() {
        return udfPathContextHolder;
    }

    public void addURLs(URL... urls) {
        for (URL url : urls) {
            super.addURL(url);
        }
    }

    public void addURLs(Collection<File> fileSet) {
        URL[] urls = convertFilesToUrls(fileSet);
        addURLs(urls);
    }

    // 把File对象转换成URL
    private static URL[] convertFilesToUrls(Collection<File> fileSet) {
        return fileSet.stream()
                .map(x -> {
                    try {
                        return x.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);

            if (loadedClass == null) {
                try {
                    // try to use this classloader to load
                    return findClass(name);
                } catch (ClassNotFoundException e) {
                    // maybe is system class, try parents delegate
                    return super.loadClass(name, false);
                }
            } else if (resolve) {
                resolveClass(loadedClass);
            }
            return loadedClass;
        }
    }

    @Override
    public URL getResource(String name) {
        // first, try and find it via the URLClassloader
        URL urlClassLoaderResource = findResource(name);

        if (urlClassLoaderResource != null) {
            return urlClassLoaderResource;
        }

        // delegate to super
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // first get resources from URLClassloader
        Enumeration<URL> urlClassLoaderResources = findResources(name);

        final List<URL> result = new ArrayList<>();

        while (urlClassLoaderResources.hasMoreElements()) {
            result.add(urlClassLoaderResources.nextElement());
        }

        // get parent urls
        Enumeration<URL> parentResources = getParent().getResources(name);

        while (parentResources.hasMoreElements()) {
            result.add(parentResources.nextElement());
        }

        return new Enumeration<URL>() {
            Iterator<URL> iter = result.iterator();

            public boolean hasMoreElements() {
                return iter.hasNext();
            }

            public URL nextElement() {
                return iter.next();
            }
        };
    }

    public static List<File> getJarFiles(String[] paths, List<String> notExistsFiles) {
        List<File> result = new LinkedList<>();
        for (String path : paths) {
            File file = new File(path);
            if (file.isDirectory()) {
                FileUtil.walkFiles(file, f -> {
                    if (FileUtil.getSuffix(f).equals("jar")) {
                        result.add(f);
                    }
                });
                continue;
            }
            if (!file.exists()) {
                if (notExistsFiles != null && !notExistsFiles.isEmpty()) {
                    notExistsFiles.add(file.getAbsolutePath());
                }
                continue;
            }
            result.add(file);
        }
        return result;
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
