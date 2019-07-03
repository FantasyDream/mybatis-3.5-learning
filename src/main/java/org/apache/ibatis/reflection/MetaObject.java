/**
 *    Copyright 2009-2018 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.BeanWrapper;
import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
import org.apache.ibatis.reflection.wrapper.MapWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author Clinton Begin
 */
public class MetaObject {

  /**
   * 原始的JavaBean对象
   */
  private final Object originalObject;
  /**
   * ObjectWrapper对象,其中封装了originalObject对象
   */
  private final ObjectWrapper objectWrapper;
  /**
   * 创建对象的工厂
   */
  private final ObjectFactory objectFactory;
  /**
   * 创建objectWrapper的工厂
   */
  private final ObjectWrapperFactory objectWrapperFactory;
  /**
   * 创建并缓存Reflector对象的工厂
   */
  private final ReflectorFactory reflectorFactory;

  private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    this.originalObject = object;
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;

    if (object instanceof ObjectWrapper) {
      // 若object已经是ObjectWrapper类型,则直接使用
      this.objectWrapper = (ObjectWrapper) object;
    } else if (objectWrapperFactory.hasWrapperFor(object)) {
      // 若ObjectWrapperFactory能够为改原始对象创建对应的ObjectWrapper对象,则优先使用,这里一般使用用户自定义的ObjectWrapperFactory,因为默认的无法使用
      this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
    } else if (object instanceof Map) {
      // 若object为Map类型,则创建MapWrapper
      this.objectWrapper = new MapWrapper(this, (Map) object);
    } else if (object instanceof Collection) {
      // 若object为Collection类型,则创建CollectionWrapper
      this.objectWrapper = new CollectionWrapper(this, (Collection) object);
    } else {
      // 最后,若前面都不成立,则创建普通的BeanWrapper类型
      this.objectWrapper = new BeanWrapper(this, object);
    }
  }

  /**
   * MetaObject的构造方法是私有的,这里提供一个静态方法来创建MetaObject类型
   *
   * @param object
   * @param objectFactory
   * @param objectWrapperFactory
   * @param reflectorFactory
   * @return
   */
  public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    if (object == null) {
      // 若Object为Null,则统一返回SystemMetaObject.Null_META_OBJECT对象,
      return SystemMetaObject.NULL_META_OBJECT;
    } else {
      // 正常新建
      return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
    }
  }

  // 从getObjectFactory到hasGetter都是直接返回属性或者直接调用方法,无需赘述

  public ObjectFactory getObjectFactory() {
    return objectFactory;
  }

  public ObjectWrapperFactory getObjectWrapperFactory() {
    return objectWrapperFactory;
  }

  public ReflectorFactory getReflectorFactory() {
	return reflectorFactory;
  }

  public Object getOriginalObject() {
    return originalObject;
  }

  public String findProperty(String propName, boolean useCamelCaseMapping) {
    return objectWrapper.findProperty(propName, useCamelCaseMapping);
  }

  public String[] getGetterNames() {
    return objectWrapper.getGetterNames();
  }

  public String[] getSetterNames() {
    return objectWrapper.getSetterNames();
  }

  public Class<?> getSetterType(String name) {
    return objectWrapper.getSetterType(name);
  }

  public Class<?> getGetterType(String name) {
    return objectWrapper.getGetterType(name);
  }

  public boolean hasSetter(String name) {
    return objectWrapper.hasSetter(name);
  }

  public boolean hasGetter(String name) {
    return objectWrapper.hasGetter(name);
  }

  public Object getValue(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      // 若有子表达式,创建相应的MetaObject对象
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        // 排除为NULL的情况
        return null;
      } else {
        // 递归处理子表达式
        return metaValue.getValue(prop.getChildren());
      }
    } else {
      // 通过ObjectWrapper获取指定的属性值
      return objectWrapper.get(prop);
    }
  }

  public void setValue(String name, Object value) {
    // 为属性表达式创建对应的解析类
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        if (value == null) {
          // don't instantiate child path if value is null
          return;
        } else {
          // 初始化表达式路径上为空的属性
          metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
        }
      }
      // 递归调用，知道处理到最后一个子表达式为止
      metaValue.setValue(prop.getChildren(), value);
    } else {
      // 赋值
      objectWrapper.set(prop, value);
    }
  }

  /**
   * 这个是通过属性名来创建MetaObject对象,不是静态方法,用于递归调用
   *
   * @param name 属性名
   * @return MetaObject
   */
  public MetaObject metaObjectForProperty(String name) {
    // 获取指定的属性
    Object value = getValue(name);
    // 创建该属性对应的MetaObject对象
    return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  }

  public ObjectWrapper getObjectWrapper() {
    return objectWrapper;
  }

  public boolean isCollection() {
    return objectWrapper.isCollection();
  }

  public void add(Object element) {
    objectWrapper.add(element);
  }

  public <E> void addAll(List<E> list) {
    objectWrapper.addAll(list);
  }

}
