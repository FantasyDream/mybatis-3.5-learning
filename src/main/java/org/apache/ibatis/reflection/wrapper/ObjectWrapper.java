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
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 */
public interface ObjectWrapper {

  /**
   * 如果ObjectWrapper中封装的是普通的Bean对象,则调用相应属性的getter方法
   * 如果封装的是集合类,则获取指定key或下标对应的value值
   *
   * @param prop 属性的解析器
   * @return getter的返回值或者集合的value值
   */
  Object get(PropertyTokenizer prop);

  /**
   * 如果ObjectWrapper中封装的是普通的Bean对象,则调用相应属性的setter方法
   * 如果封装的是集合类,则设置指定key或下标对应的value值
   *
   * @param prop 属性的解析器
   * @param value 要设置的值
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 查找属性表达式指定的属性
   *
   * @param name 属性表达式
   * @param useCamelCaseMapping 是否做驼峰命名映射,即忽略下划线
   * @return 属性名
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  /**
   * 查找可读属性的名称集合
   *
   * @return 属性集合
   */
  String[] getGetterNames();

  /**
   * 查找可写属性的名称集合
   *
   * @return 属性集合
   */
  String[] getSetterNames();

  /**
   * 解析属性表达式指定属性的setter方法的参数类型
   *
   * @param name 属性名
   * @return 参数类型
   */
  Class<?> getSetterType(String name);

  /**
   * 解析属性表达式指定属性的getter方法的返回值
   *
   * @param name 属性名
   * @return 返回值类型
   */
  Class<?> getGetterType(String name);

  /**
   * 判断属性是否有setter方法
   *
   * @param name 属性名
   * @return 是否有
   */
  boolean hasSetter(String name);

  /**
   * 判断属性是否有getter方法
   *
   * @param name 属性名
   * @return 是否有
   */
  boolean hasGetter(String name);

  /**
   * 为属性表达式指定的属性创建相应的MetaObject对象
   * todo
   * @param name
   * @param prop
   * @param objectFactory
   * @return
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  /**
   * 是否为Collection类型
   *
   * @return 是否
   */
  boolean isCollection();

  /**
   * 调用Collection的add()方法
   *
   * @param element 要添加的对象
   */
  void add(Object element);

  /**
   * 调用Collection的addAll()方法
   * @param element 要添加的对象列表
   * @param <E> 对象类型
   */
  <E> void addAll(List<E> element);

}
