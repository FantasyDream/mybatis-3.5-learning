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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 */
public class MetaClass {

  /**
   *  用于创建或者缓存Reflector对象，这个属性应该是单例的，所有MetaClass共享一个reflectorFactory
   */
  private final ReflectorFactory reflectorFactory;
  /**
   *  用于记录在创建时指定的一个类的源信息，这个则是每个MetaClass都自带一个
   */
  private final Reflector reflector;

  /**
   * MetaClass的构造方法是私有的,不能直接用new来创建MetaClass
   * @param type 用于创建reflector的类
   * @param reflectorFactory 创建reflector或者缓存它
   */
  private MetaClass(Class<?> type, ReflectorFactory reflectorFactory) {
    this.reflectorFactory = reflectorFactory;
    this.reflector = reflectorFactory.findForClass(type);
  }

  /**
   * 静态方法,用于创建MetaClass
   *
   * @param type 用于创建reflector的类
   * @param reflectorFactory 创建reflector或者缓存它
   * @return 创建好的MetaClass对象
   */
  public static MetaClass forClass(Class<?> type, ReflectorFactory reflectorFactory) {
    return new MetaClass(type, reflectorFactory);
  }

  /**
   * 根据属性名创建属性类型对应的MetaClass
   * @param name 属性名
   * @return 属性名类型对应的MetaClass
   */
  public MetaClass metaClassForProperty(String name) {
    Class<?> propType = reflector.getGetterType(name);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 根据大小写错误的属性名查找到真正的属性名
   * @param name 大小写错误的属性名
   * @return 真正的属性名
   */
  public String findProperty(String name) {
    StringBuilder prop = buildProperty(name, new StringBuilder());
    return prop.length() > 0 ? prop.toString() : null;
  }

  /**
   * 添加驼峰命名解析开关的方法,应该是用于数据库字段名转JavaBean属性名的查询
   * @param name 属性名
   * @param useCamelCaseMapping
   * @return
   */
  public String findProperty(String name, boolean useCamelCaseMapping) {
    if (useCamelCaseMapping) {
      // 将下划线去掉
      name = name.replace("_", "");
    }
    return findProperty(name);
  }

  public String[] getGetterNames() {
    return reflector.getGetablePropertyNames();
  }

  public String[] getSetterNames() {
    return reflector.getSetablePropertyNames();
  }

  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop.getName());
      return metaProp.getSetterType(prop.getChildren());
    } else {
      return reflector.getSetterType(prop.getName());
    }
  }

  /**
   * 重载方法,通过String型的name构建出PropertyTokenizer,再调用以PropertyTokenizer的方法
   * @param name 属性名
   * @return getter返回值类型
   */
  public Class<?> getGetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      MetaClass metaProp = metaClassForProperty(prop);
      return metaProp.getGetterType(prop.getChildren());
    }
    // issue #506. Resolve the type inside a Collection Object
    return getGetterType(prop);
  }

  /**
   * 重载方法，实现逻辑比以string为参数的方法复杂，主要通过getGetterType实现
   * @param prop 属性的PropertyTokenizer
   * @return 属性对应的类型的MetaClass
   */
  private MetaClass metaClassForProperty(PropertyTokenizer prop) {
    Class<?> propType = getGetterType(prop);
    return MetaClass.forClass(propType, reflectorFactory);
  }

  /**
   * 通过PropertyTokenizer获得其对应的类型
   * @param prop
   * @return
   */
  private Class<?> getGetterType(PropertyTokenizer prop) {
    // 获取属性类型
    Class<?> type = reflector.getGetterType(prop.getName());
    // 该表达式中是否使用"[]",且是Collection子类
    if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
      // 通过TypeParameterResolver工具类解析属性的类型
      Type returnType = getGenericGetterType(prop.getName());
      // 针对泛型集合进行处理
      if (returnType instanceof ParameterizedType) {
        // 获取实际的类型参数
        Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
        // 这里增加了一个length==1的判定条件,我觉得应该是用来指定数组类的集合来解析,而Map类的不能用来解析,也不能用表达式表示
        if (actualTypeArguments != null && actualTypeArguments.length == 1) {
          // 获得泛型的类型
          returnType = actualTypeArguments[0];
          if (returnType instanceof Class) {
            type = (Class<?>) returnType;
          } else if (returnType instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) returnType).getRawType();
          }
        }
      }
    }
    return type;
  }

  /**
   * 获得集合的类型,主要用于判断是否为泛型集合
   * @param propertyName 属性名
   * @return 集合类型
   */
  private Type getGenericGetterType(String propertyName) {
    try {
      // 获得属性名对应的方法对象
      Invoker invoker = reflector.getGetInvoker(propertyName);
      // 根据invoker的类型的不同,判断是解析字段类型还是方法返回值类型
      if (invoker instanceof MethodInvoker) {
        Field _method = MethodInvoker.class.getDeclaredField("method");
        _method.setAccessible(true);
        Method method = (Method) _method.get(invoker);
        return TypeParameterResolver.resolveReturnType(method, reflector.getType());
      } else if (invoker instanceof GetFieldInvoker) {
        Field _field = GetFieldInvoker.class.getDeclaredField("field");
        _field.setAccessible(true);
        Field field = (Field) _field.get(invoker);
        return TypeParameterResolver.resolveFieldType(field, reflector.getType());
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }
    return null;
  }

  /**
   * 与hasGetter逻辑基本相同
   * @param name 属性名
   * @return 是否有setter
   */
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (reflector.hasSetter(prop.getName())) {
        MetaClass metaProp = metaClassForProperty(prop.getName());
        return metaProp.hasSetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      return reflector.hasSetter(prop.getName());
    }
  }

  public boolean hasGetter(String name) {
    // 解析属性表达式
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 存在待处理的子表达式
    if (prop.hasNext()) {
      // PropertyTokenizer.name指定的属性有getter方法，才能处理子表达式
      if (reflector.hasGetter(prop.getName())) {
        // 这里仍是递归调用，判断每个子表达式是否有Getter方法，有一个没有就返回false
        MetaClass metaProp = metaClassForProperty(prop);
        return metaProp.hasGetter(prop.getChildren());
      } else {
        return false;
      }
    } else {
      // 跳出递归
      return reflector.hasGetter(prop.getName());
    }
  }

  public Invoker getGetInvoker(String name) {
    return reflector.getGetInvoker(name);
  }

  public Invoker getSetInvoker(String name) {
    return reflector.getSetInvoker(name);
  }

  /**
   * 下面是findProperty()的具体实现
   * @param name 要解析的属性名称
   * @param builder 拥有部分信息的builder
   * @return
   */
  private StringBuilder buildProperty(String name, StringBuilder builder) {
    // 解析属性表达式
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 是否有子表达式
    if (prop.hasNext()) {
      // 查找PropertyTokenizer.name对应的属性
      String propertyName = reflector.findPropertyName(prop.getName());
      // 将属性名追加到builder后面
      if (propertyName != null) {
        builder.append(propertyName);
        builder.append(".");
        // 为该属性创建对应的MetaClass对象
        MetaClass metaProp = metaClassForProperty(propertyName);
        // 递归解析PropertyTokenizer.Children字段,并将结果转嫁到builder后面
        metaProp.buildProperty(prop.getChildren(), builder);
      }
    } else {
      // 如果prop没有子表达式,则追加属性后返回,这是递归出口
      String propertyName = reflector.findPropertyName(name);
      if (propertyName != null) {
        builder.append(propertyName);
      }
    }
    return builder;
  }

  /**
   * 判断是否拥有默认构造方法,即无参的构造方法
   * @return 是否拥有
   */
  public boolean hasDefaultConstructor() {
    return reflector.hasDefaultConstructor();
  }

}
