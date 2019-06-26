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
   * 创建指定Class对应的Reflector对象
   *
   * @param type 需要创建Reflector的类
   * @return Reflector
   */
  Reflector findForClass(Class<?> type);
}