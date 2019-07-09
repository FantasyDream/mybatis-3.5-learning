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
package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;
  /**
   * 记录了关联额sqlsession对象
   */
  private final SqlSession sqlSession;
  /**
   * 要代理的Mapper接口的class对象
   */
  private final Class<T> mapperInterface;
  /**
   * 用于缓存MapperMethod对象，key为Mapper接口的方法，value是对应的mapperMethod对象。MapperMethod中会完成参数转换，执行sql等功能
   */
  private final Map<Method, MapperMethod> methodCache;

  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // 如果是Object类的方法，则直接执行
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, args);
      } else if (isDefaultMethod(method)) {
        // 如果是mapper接口的default方法，则执行default方法
        return invokeDefaultMethod(proxy, method, args);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    // 当方法是操作数据库的方法时，则缓存并获取该方法对应的MapperMethod对象，然后执行
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    return mapperMethod.execute(sqlSession, args);
  }

  private MapperMethod cachedMapperMethod(Method method) {
    // computeIfAbsent方法是jdk8里的新方法，有则直接返回，没有的话先存储再返回
    // 这里的computeIfAbsent方法写的有点不规范，不符合方法的本意
    // 这个方法的第二个参数是一个单参数的方法接口Function，再computeIfAbsent内部中，会将要查询的key，即method当作参数带入Function中
    // 所以在新建MapperMethod对象时，应该将第二个参数替换为 k ，比较符合computeIfAbsent设计时的思想
    return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
  }

  /**
   * 创建Lookup对象，并通过lookup获得没有被从写的methodHandle，并执行该方法，目的是为了直接执行接口中的default方法
   * 这里的MethodHandle是jdk7引入的，比Method更轻量的一个代表 方法 的对象，对象的获取也更加灵活
   * 一般用于基于JVM的动态类型语言使用，jdk8的新特性也用到了这个
   * Lookup则是MethodHandle的工厂类，根据参数的不同生成不同的MethodHandle对象
   * @param proxy
   * @param method
   * @param args
   * @return
   * @throws Throwable
   */
  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
      throws Throwable {
    final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
        .getDeclaredConstructor(Class.class, int.class);
    if (!constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
    final Class<?> declaringClass = method.getDeclaringClass();
    return constructor
        .newInstance(declaringClass,
            MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC)
        .unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
  }

  /**
   * Backport of java.lang.reflect.Method#isDefault()
   */
  private boolean isDefaultMethod(Method method) {
    // Modifier中没有default属性，所以要找既不是abstract也不是static的public方法来判断
    return (method.getModifiers()
        & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
        && method.getDeclaringClass().isInterface();
  }
}
