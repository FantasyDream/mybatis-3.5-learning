/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * A default implementation of {@link VFS} that works for most application servers.
 *
 * @author Ben Gunter
 */
public class DefaultVFS extends VFS {
  private static final Log log = LogFactory.getLog(DefaultVFS.class);

  /** The magic header that indicates a JAR (ZIP) file. */
  private static final byte[] JAR_MAGIC = { 'P', 'K', 3, 4 };

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public List<String> list(URL url, String path) throws IOException {
    InputStream is = null;
    try {
      List<String> resources = new ArrayList<>();

      // 如果url指向的资源在一个jar包中,则获取该jar包对应的url.否则返回null
      URL jarUrl = findJarForResource(url);
      if (jarUrl != null) {
        is = jarUrl.openStream();
        if (log.isDebugEnabled()) {
          log.debug("Listing " + url);
        }
        // 遍历jar中的资源,并返回以path开头的资源列表
        resources = listResources(new JarInputStream(is), path);
      }
      else {
        List<String> children = new ArrayList<>();
        try {
          if (isJar(url)) {
            // 有些JBoss VFS的版本可能提供一个Jar流,即url引用的资源不是一个jar包
            is = url.openStream();
            try (JarInputStream jarInput = new JarInputStream(is)) {
              if (log.isDebugEnabled()) {
                log.debug("Listing " + url);
              }
              for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null; ) {
                if (log.isDebugEnabled()) {
                  log.debug("Jar entry: " + entry.getName());
                }
                // 将资源名称记录到children集合中
                children.add(entry.getName());
              }
            }
          }
          else {
            // 某些servlet容器允许从目录资源（如文本文件）中读取，每行列出一个子资源。
            // 但是，只能通过读取它们来区分目录和文件资源。
            // 要解决这个问题，在读取每一行时，尝试通过类加载器将其作为当前资源path的子项进行查找。
            // 如果任何行失败，那么我们假设当前资源不是目录。
            is = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = new ArrayList<>();
            for (String line; (line = reader.readLine()) != null;) {
              if (log.isDebugEnabled()) {
                log.debug("Reader entry: " + line);
              }
              lines.add(line);
              if (getResources(path + "/" + line).isEmpty()) {
                lines.clear();
                break;
              }
            }

            if (!lines.isEmpty()) {
              if (log.isDebugEnabled()) {
                log.debug("Listing " + url);
              }
              children.addAll(lines);
            }
          }
        } catch (FileNotFoundException e) {
          // 对于文件URL，openStream()调用可能会失败，这取决于servlet容器，因为无法打开目录进行读取。如果发生这种情况，直接列出目录。
          if ("file".equals(url.getProtocol())) {
            // 如果url是文件协议
            File file = new File(url.getFile());
            if (log.isDebugEnabled()) {
                log.debug("Listing directory " + file.getAbsolutePath());
            }
            if (file.isDirectory()) {
              if (log.isDebugEnabled()) {
                  log.debug("Listing " + url);
              }
              // 如果file是目录,则直接列出该目录下的所有文件和目录名
              children = Arrays.asList(file.list());
            }
          }
          else {
            // No idea where the exception came from so rethrow it
            throw e;
          }
        }

        // 递归列出子资源时的前缀,即当前url的字符串形式
        String prefix = url.toExternalForm();
        if (!prefix.endsWith("/")) {
          prefix = prefix + "/";
        }

        // 遍历子资源,并递归遍历子资源中的目录,将其加入到resources里
        for (String child : children) {
          String resourcePath = path + "/" + child;
          resources.add(resourcePath);
          URL childUrl = new URL(prefix + child);
          resources.addAll(list(childUrl, resourcePath));
        }
      }

      return resources;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // Ignore
        }
      }
    }
  }

  /**
   * List the names of the entries in the given {@link JarInputStream} that begin with the
   * specified {@code path}. Entries will match with or without a leading slash.
   *
   * @param jar The JAR input stream
   * @param path The leading path to match
   * @return The names of all the matching entries
   * @throws IOException If I/O errors occur
   */
  protected List<String> listResources(JarInputStream jar, String path) throws IOException {
    // 匹配名称时需要在前后加上'/'
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    if (!path.endsWith("/")) {
      path = path + "/";
    }

    // 遍历entry并收集以path开头的元素
    List<String> resources = new ArrayList<>();
    for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
      if (!entry.isDirectory()) {
        // 如果不是以'/'开头,则添加
        StringBuilder name = new StringBuilder(entry.getName());
        if (name.charAt(0) != '/') {
          name.insert(0, '/');
        }

        // 检测name是否以path开头
        if (name.indexOf(path) == 0) {
          if (log.isDebugEnabled()) {
            log.debug("Found resource: " + name);
          }
          // 记录资源,并删除"/"
          resources.add(name.substring(1));
        }
      }
    }
    return resources;
  }

  /**
   * Attempts to deconstruct the given URL to find a JAR file containing the resource referenced
   * by the URL. That is, assuming the URL references a JAR entry, this method will return a URL
   * that references the JAR file containing the entry. If the JAR cannot be located, then this
   * method returns null.
   *
   * @param url The URL of the JAR entry.
   * @return The URL of the JAR file, if one is found. Null if not.
   * @throws MalformedURLException
   */
  protected URL findJarForResource(URL url) throws MalformedURLException {
    if (log.isDebugEnabled()) {
      log.debug("Find JAR URL: " + url);
    }

    // If the file part of the URL is itself a URL, then that URL probably points to the JAR
    try {
      for (;;) {
        url = new URL(url.getFile());
        if (log.isDebugEnabled()) {
          log.debug("Inner URL: " + url);
        }
      }
    } catch (MalformedURLException e) {
      // This will happen at some point and serves as a break in the loop
    }

    // Look for the .jar extension and chop off everything after that
    StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
    int index = jarUrl.lastIndexOf(".jar");
    if (index >= 0) {
      jarUrl.setLength(index + 4);
      if (log.isDebugEnabled()) {
        log.debug("Extracted JAR URL: " + jarUrl);
      }
    }
    else {
      if (log.isDebugEnabled()) {
        log.debug("Not a JAR: " + jarUrl);
      }
      return null;
    }

    // Try to open and test it
    try {
      URL testUrl = new URL(jarUrl.toString());
      if (isJar(testUrl)) {
        return testUrl;
      }
      else {
        // WebLogic fix: check if the URL's file exists in the filesystem.
        if (log.isDebugEnabled()) {
          log.debug("Not a JAR: " + jarUrl);
        }
        jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
        File file = new File(jarUrl.toString());

        // File name might be URL-encoded
        if (!file.exists()) {
          try {
            file = new File(URLEncoder.encode(jarUrl.toString(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding?  UTF-8?  That's unpossible.");
          }
        }

        if (file.exists()) {
          if (log.isDebugEnabled()) {
            log.debug("Trying real file: " + file.getAbsolutePath());
          }
          testUrl = file.toURI().toURL();
          if (isJar(testUrl)) {
            return testUrl;
          }
        }
      }
    } catch (MalformedURLException e) {
      log.warn("Invalid JAR URL: " + jarUrl);
    }

    if (log.isDebugEnabled()) {
      log.debug("Not a JAR: " + jarUrl);
    }
    return null;
  }

  /**
   * Converts a Java package name to a path that can be looked up with a call to
   * {@link ClassLoader#getResources(String)}.
   *
   * @param packageName The Java package name to convert to a path
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * Returns true if the resource located at the given URL is a JAR file.
   *
   * @param url The URL of the resource to test.
   */
  protected boolean isJar(URL url) {
    return isJar(url, new byte[JAR_MAGIC.length]);
  }

  /**
   * Returns true if the resource located at the given URL is a JAR file.
   *
   * @param url The URL of the resource to test.
   * @param buffer A buffer into which the first few bytes of the resource are read. The buffer
   *            must be at least the size of {@link #JAR_MAGIC}. (The same buffer may be reused
   *            for multiple calls as an optimization.)
   */
  protected boolean isJar(URL url, byte[] buffer) {
    InputStream is = null;
    try {
      is = url.openStream();
      is.read(buffer, 0, JAR_MAGIC.length);
      if (Arrays.equals(buffer, JAR_MAGIC)) {
        if (log.isDebugEnabled()) {
          log.debug("Found JAR: " + url);
        }
        return true;
      }
    } catch (Exception e) {
      // Failure to read the stream means this is not a JAR
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // Ignore
        }
      }
    }

    return false;
  }
}
