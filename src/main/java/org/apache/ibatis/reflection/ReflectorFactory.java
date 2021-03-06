/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.reflection;

/**
 * 这是Reflector的工厂接口，它的实现类需要实现三个功能，打开缓存功能，查看是否会缓存，和创建Reflector。
 * 该类会在框架启动加载配置文件时创建，是单例的。
 *
 * @author mybatis
 */
public interface ReflectorFactory {

  /**
   * 检测本对象是否会缓存Reflector对象
   *
   * @return 有没有缓存
   */
  boolean isClassCacheEnabled();

  /**
   * 这是是否缓存Reflector对象
   *
   * @param classCacheEnabled 是否缓存
   */
  void setClassCacheEnabled(boolean classCacheEnabled);

  /**
   * 创建指定Class对应的Reflector对象返回,或者从缓存中找到并返回
   *
   * @param type 需要创建Reflector的类
   * @return reflector
   */
  Reflector findForClass(Class<?> type);
}