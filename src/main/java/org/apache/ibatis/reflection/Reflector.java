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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * 该类为mybatis反射模块的基础,一个Reflector对象对应一个类,该类的实例缓存了反射操作需要使用的类的元信息.
 *
 * @author Clinton Begin
 */
public class Reflector {
  /**
   * 对应的Class
   */
  private final Class<?> type;
  /**
   * 可读属性的名称集合,可读属性就是存在相应getter方法的属性
   */
  private final String[] readablePropertyNames;
  /**
   * 可写属性的名称集合,可写属性就是存在相应setter方法的属性
   */
  private final String[] writeablePropertyNames;
  /**
   * setter方法的缓存,key是属性名称,value是setter方法对应的Method对象的封装Invoker
   */
  private final Map<String, Invoker> setMethods = new HashMap<>();
  /**
   * getter方法的缓存,key是属性名称,value是getter方法对应的Method对象的封装Invoker
   */
  private final Map<String, Invoker> getMethods = new HashMap<>();
  /**
   * 记录setter方法的参数类型,key是属性名称,value是setter方法的参数类型
   */
  private final Map<String, Class<?>> setTypes = new HashMap<>();
  /**
   * 记录getter方法的返回值类型,key是属性名称,value是getter方法的返回值类型
   */
  private final Map<String, Class<?>> getTypes = new HashMap<>();
  /**
   * 默认构造方法,即无参构造方法
   */
  private Constructor<?> defaultConstructor;
  /**
   * 所有属性的集合,,key为大写的属性名,value为属性名
   */
  private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

  /**
   * @param clazz 对应的类的Class对象
   */
  public Reflector(Class<?> clazz) {
    // 初始化各个字段
    type = clazz;
    addDefaultConstructor(clazz);
    addGetMethods(clazz);
    addSetMethods(clazz);
    // 处理没有getter/setter方法的字段
    addFields(clazz);
    readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
    writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
    // 通过map消除重复的元素，获得拥有所有属性的集合
    for (String propName : readablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
    for (String propName : writeablePropertyNames) {
      caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
    }
  }

  /**
   * 初始化defaultConstructor
   * @param clazz Reflector对应的类型
   */
  private void addDefaultConstructor(Class<?> clazz) {
    Constructor<?>[] consts = clazz.getDeclaredConstructors();
    for (Constructor<?> constructor : consts) {
      if (constructor.getParameterTypes().length == 0) {
          this.defaultConstructor = constructor;
      }
    }
  }

  /**
   * 添加所有getter方法
   * @param cls 当前类型
   */
  private void addGetMethods(Class<?> cls) {
    Map<String, List<Method>> conflictingGetters = new HashMap<>();
    // 获取该类型所有方法
    Method[] methods = getClassMethods(cls);
    for (Method method : methods) {
      // 有参数的method跳过
      if (method.getParameterTypes().length > 0) {
        continue;
      }
      String name = method.getName();
      // 以get和is开头的方法添加到conflictingGetters
      if ((name.startsWith("get") && name.length() > 3)
          || (name.startsWith("is") && name.length() > 2)) {
        // 从方法名中截取出属性名
        name = PropertyNamer.methodToProperty(name);
        // 将name和method作为key和value放入conflictingGetters中
        addMethodConflict(conflictingGetters, name, method);
      }
    }
    resolveGetterConflicts(conflictingGetters);
  }

  /**
   * 处理子类覆盖父类方法时,返回值不同导致同一方法数量大于一个的情况
   * @param conflictingGetters
   */
  private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
    for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
      Method winner = null;
      String propName = entry.getKey();
      for (Method candidate : entry.getValue()) {
        if (winner == null) {
          winner = candidate;
          continue;
        }
        // 判断两个方法的返回值类型是否相同
        Class<?> winnerType = winner.getReturnType();
        Class<?> candidateType = candidate.getReturnType();
        // 当返回值相同时，一般要抛出异常，但是有特例。
        if (candidateType.equals(winnerType)) {
          // 若不是Boolean类型，则抛出异常，因为Boolean型的getter和setter可能会出现同时有isXXX和getXXX的现象。
          if (!boolean.class.equals(candidateType)) {
            throw new ReflectionException(
                "Illegal overloaded getter method with ambiguous type for property "
                    + propName + " in class " + winner.getDeclaringClass()
                    + ". This breaks the JavaBeans specification and can cause unpredictable results.");
          } else if (candidate.getName().startsWith("is")) {
            // 选择isXXX为getter方法
            winner = candidate;
          }
        } else if (candidateType.isAssignableFrom(winnerType)) {
          // 若winnerType是candidateType的子类，则什么都不用做
        } else if (winnerType.isAssignableFrom(candidateType)) {
          // 反之，winnerType切换成当前类，这是为了符合java的override规则
          winner = candidate;
        } else {
          // 返回值完全没有关联，二义性，抛出异常
          throw new ReflectionException(
              "Illegal overloaded getter method with ambiguous type for property "
                  + propName + " in class " + winner.getDeclaringClass()
                  + ". This breaks the JavaBeans specification and can cause unpredictable results.");
        }
      }
      // 将getter方法和方法返回值放入getMethods和getTypes中
      addGetMethod(propName, winner);
    }
  }

  /**
   * 添加GetMethod到getMethods和getTypes字段中
   * @param name 属性名
   * @param method 属性名对应的getter方法
   */
  private void addGetMethod(String name, Method method) {
    // 检测属性是否合法
    if (isValidPropertyName(name)) {
      // 将属性名和对应的MethodInvoker添加到getMethods中
      getMethods.put(name, new MethodInvoker(method));
      // 获取返回值的type
      Type returnType = TypeParameterResolver.resolveReturnType(method, type);
      // 将属性名称和返回值添加到getTypes中
      getTypes.put(name, typeToClass(returnType));
    }
  }

  private void addSetMethods(Class<?> cls) {
    Map<String, List<Method>> conflictingSetters = new HashMap<>();
    Method[] methods = getClassMethods(cls);
    for (Method method : methods) {
      String name = method.getName();
      if (name.startsWith("set") && name.length() > 3) {
        if (method.getParameterTypes().length == 1) {
          name = PropertyNamer.methodToProperty(name);
          addMethodConflict(conflictingSetters, name, method);
        }
      }
    }
    resolveSetterConflicts(conflictingSetters);
  }

  /**
   * 以属性名为key，对应的方法组成的数组为value，存入map中
   *
   * @param conflictingMethods
   * @param name
   * @param method
   */
  private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
    List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
    list.add(method);
  }

  private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
    for (String propName : conflictingSetters.keySet()) {
      List<Method> setters = conflictingSetters.get(propName);
      Class<?> getterType = getTypes.get(propName);
      Method match = null;
      ReflectionException exception = null;
      for (Method setter : setters) {
        Class<?> paramType = setter.getParameterTypes()[0];
        if (paramType.equals(getterType)) {
          // should be the best match
          match = setter;
          break;
        }
        if (exception == null) {
          try {
            match = pickBetterSetter(match, setter, propName);
          } catch (ReflectionException e) {
            // there could still be the 'best match'
            match = null;
            exception = e;
          }
        }
      }
      if (match == null) {
        throw exception;
      } else {
        addSetMethod(propName, match);
      }
    }
  }

  private Method pickBetterSetter(Method setter1, Method setter2, String property) {
    if (setter1 == null) {
      return setter2;
    }
    Class<?> paramType1 = setter1.getParameterTypes()[0];
    Class<?> paramType2 = setter2.getParameterTypes()[0];
    if (paramType1.isAssignableFrom(paramType2)) {
      return setter2;
    } else if (paramType2.isAssignableFrom(paramType1)) {
      return setter1;
    }
    throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '"
        + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '"
        + paramType2.getName() + "'.");
  }

  private void addSetMethod(String name, Method method) {
    if (isValidPropertyName(name)) {
      setMethods.put(name, new MethodInvoker(method));
      Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
      setTypes.put(name, typeToClass(paramTypes[0]));
    }
  }

  private Class<?> typeToClass(Type src) {
    Class<?> result = null;
    if (src instanceof Class) {
      result = (Class<?>) src;
    } else if (src instanceof ParameterizedType) {
      result = (Class<?>) ((ParameterizedType) src).getRawType();
    } else if (src instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) src).getGenericComponentType();
      if (componentType instanceof Class) {
        result = Array.newInstance((Class<?>) componentType, 0).getClass();
      } else {
        Class<?> componentClass = typeToClass(componentType);
        result = Array.newInstance(componentClass, 0).getClass();
      }
    }
    if (result == null) {
      result = Object.class;
    }
    return result;
  }

  private void addFields(Class<?> clazz) {
    // 获得clazz的所有字段
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      // 若setMethods不存在该属性时，则将它添加到setMethods和setTypes中
      if (!setMethods.containsKey(field.getName())) {
        int modifiers = field.getModifiers();
        // 排除被final和static修饰的字段
        if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
          addSetField(field);
        }
      }
      // 若getMethods不存在该属性时，则将它添加到getMethods和getTypes中,其中setMethods的value是mybatis自己定义的类GetFieldInvoker
      if (!getMethods.containsKey(field.getName())) {
        addGetField(field);
      }
    }
    // 处理父类中的字段
    if (clazz.getSuperclass() != null) {
      addFields(clazz.getSuperclass());
    }
  }

  private void addSetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      setMethods.put(field.getName(), new SetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      setTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  private void addGetField(Field field) {
    if (isValidPropertyName(field.getName())) {
      getMethods.put(field.getName(), new GetFieldInvoker(field));
      Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
      getTypes.put(field.getName(), typeToClass(fieldType));
    }
  }

  private boolean isValidPropertyName(String name) {
    return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
  }

  /**
   * 获取一个类的所有Method,包括父类和接口的Method.
   *
   * @param cls 类
   * @return 包含所有method的数组
   */
  private Method[] getClassMethods(Class<?> cls) {
    Map<String, Method> uniqueMethods = new HashMap<>();
    Class<?> currentClass = cls;
    while (currentClass != null && currentClass != Object.class) {
      // 记录当前类定义的所有方法,并为每个方法生成唯一签名
      addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

      // 记录接口中的方法,因为当前类可能是抽象类
      Class<?>[] interfaces = currentClass.getInterfaces();
      for (Class<?> anInterface : interfaces) {
        addUniqueMethods(uniqueMethods, anInterface.getMethods());
      }

      // 获取父类,继续循环
      currentClass = currentClass.getSuperclass();
    }

    Collection<Method> methods = uniqueMethods.values();

    return methods.toArray(new Method[methods.size()]);
  }

  private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
    for (Method currentMethod : methods) {
      if (!currentMethod.isBridge()) {
        // 生成签名
        String signature = getSignature(currentMethod);
        // check to see if the method is already known
        // if it is known, then an extended class must have
        // overridden a method
        if (!uniqueMethods.containsKey(signature)) {
          uniqueMethods.put(signature, currentMethod);
        }
      }
    }
  }

  /**
   * 生成的签名格式为: 返回值类型#方法名称:参数类型列表
   * @param method
   * @return
   */
  private String getSignature(Method method) {
    StringBuilder sb = new StringBuilder();
    Class<?> returnType = method.getReturnType();
    if (returnType != null) {
      sb.append(returnType.getName()).append('#');
    }
    sb.append(method.getName());
    Class<?>[] parameters = method.getParameterTypes();
    for (int i = 0; i < parameters.length; i++) {
      if (i == 0) {
        sb.append(':');
      } else {
        sb.append(',');
      }
      sb.append(parameters[i].getName());
    }
    return sb.toString();
  }

  /**
   * 检查是否有反射控制权限
   *
   * @return If can control member accessible, it return {@literal true}
   * @since 3.5.0
   */
  public static boolean canControlMemberAccessible() {
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (null != securityManager) {
        securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
      }
    } catch (SecurityException e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the name of the class the instance provides information for
   *
   * @return The class name
   */
  public Class<?> getType() {
    return type;
  }

  public Constructor<?> getDefaultConstructor() {
    if (defaultConstructor != null) {
      return defaultConstructor;
    } else {
      throw new ReflectionException("There is no default constructor for " + type);
    }
  }

  public boolean hasDefaultConstructor() {
    return defaultConstructor != null;
  }

  public Invoker getSetInvoker(String propertyName) {
    Invoker method = setMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  public Invoker getGetInvoker(String propertyName) {
    Invoker method = getMethods.get(propertyName);
    if (method == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return method;
  }

  /**
   * Gets the type for a property setter
   *
   * @param propertyName - the name of the property
   * @return The Class of the property setter
   */
  public Class<?> getSetterType(String propertyName) {
    Class<?> clazz = setTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets the type for a property getter
   *
   * @param propertyName - the name of the property
   * @return The Class of the property getter
   */
  public Class<?> getGetterType(String propertyName) {
    Class<?> clazz = getTypes.get(propertyName);
    if (clazz == null) {
      throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
    }
    return clazz;
  }

  /**
   * Gets an array of the readable properties for an object
   *
   * @return The array
   */
  public String[] getGetablePropertyNames() {
    return readablePropertyNames;
  }

  /**
   * Gets an array of the writable properties for an object
   *
   * @return The array
   */
  public String[] getSetablePropertyNames() {
    return writeablePropertyNames;
  }

  /**
   * Check to see if a class has a writable property by name
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a writable property by the name
   */
  public boolean hasSetter(String propertyName) {
    return setMethods.keySet().contains(propertyName);
  }

  /**
   * Check to see if a class has a readable property by name
   *
   * @param propertyName - the name of the property to check
   * @return True if the object has a readable property by the name
   */
  public boolean hasGetter(String propertyName) {
    return getMethods.keySet().contains(propertyName);
  }

  public String findPropertyName(String name) {
    return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
  }
}
