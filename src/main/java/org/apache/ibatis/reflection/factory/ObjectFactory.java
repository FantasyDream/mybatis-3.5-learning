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
package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 *
 * @author Clinton Begin
 */
public interface ObjectFactory {

  /**
   * 设置配置信息
   * @param properties configuration properties
   */
  void setProperties(Properties properties);

  /**
   * 通过无参构造方法创建一个对象
   * @param type 要创建的对象类型
   * @return 创建好的对象
   */
  <T> T create(Class<T> type);

  /**
   * 根据参数列表，从指定类型中选择合适的构造方法创建对象
   * @param type                要创建的对象类型
   * @param constructorArgTypes 构造方法参数类型
   * @param constructorArgs     构造方法参数值
   * @return 创建好的对象
   */
  <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

  /**
   * 检测指定类型是否为集合类型，主要处理java.util.Collection及其子类
   *
   * @param type 对象类型
   * @return 是否为集合类型
   * @since 3.1.0
   */
  <T> boolean isCollection(Class<T> type);

}
