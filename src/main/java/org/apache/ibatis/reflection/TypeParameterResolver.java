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
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * 该类是一个工具类,就是用来解析某个类的方法参数,方法返回值,字段的类型
 * @author Iwao AVE!
 */
public class TypeParameterResolver {

  /**
   * 解析字段的类型--Type，并返回。Type是Class的接口，只有一个返回类型名称的方法。
   *
   * @return 字段类型
   */
  public static Type resolveFieldType(Field field, Type srcType) {
    // 获取字段的声明类型
    Type fieldType = field.getGenericType();
    // 获取字段所在类的Class对象
    Class<?> declaringClass = field.getDeclaringClass();
    // 后续处理
    return resolveType(fieldType, srcType, declaringClass);
  }

  /**
   * 解析Class的返回值类型，具体实现与resolveFieldType相同
   *
   * @return 返回值类型
   */
  public static Type resolveReturnType(Method method, Type srcType) {
    Type returnType = method.getGenericReturnType();
    Class<?> declaringClass = method.getDeclaringClass();
    return resolveType(returnType, srcType, declaringClass);
  }

  /**
   * 解析参数的类型，与resolveFieldType和resolveReturnType具体实现类似
   *
   * @return 参数类型数组
   */
  public static Type[] resolveParamTypes(Method method, Type srcType) {
    Type[] paramTypes = method.getGenericParameterTypes();
    Class<?> declaringClass = method.getDeclaringClass();
    Type[] result = new Type[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      result[i] = resolveType(paramTypes[i], srcType, declaringClass);
    }
    return result;
  }

  /**
   * 真正解析类型的方法
   *
   * @param type 字段，返回值或者方法参数的类型
   * @param srcType 查找该字段、返回值或方法参数的起始位置
   * @param declaringClass 字段，返回值或者方法参数所在的类
   * @return 解析后的类型
   */
  private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
    // 解析TypeVariable类型，TypeVariable类型表示的是类型变量，如List<T>中的T就是类型变量
    if (type instanceof TypeVariable) {
      return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
    } else if (type instanceof ParameterizedType) {
      // 解析ParameterizedType类型，ParameterizedType类型表示的是参数化类型，例如List<String>
      return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
    } else if (type instanceof GenericArrayType) {
      // 解析GenericArrayType类型，GenericArrayType类型表示的是组成类型为TypeVariable或ParameterizedType类型的数组类型
      return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
    } else {
      return type;
    }
  }

  /**
   * 解析GenericArrayType类型，GenericArrayType类型表示的是组成类型为TypeVariable或ParameterizedType类型的数组类型
   *
   * @param genericArrayType 要解析的变量
   * @param srcType          定位的类，解析的起始点
   * @param declaringClass   定义字段或方法的类
   * @return
   */
  private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
    // 获取数组元素的类型
    Type componentType = genericArrayType.getGenericComponentType();
    Type resolvedComponentType = null;
    // 与resolveType的处理一致
    if (componentType instanceof TypeVariable) {
      resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
    } else if (componentType instanceof GenericArrayType) {
      // 递归调用
      resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
    } else if (componentType instanceof ParameterizedType) {
      resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
    }
    // 根据解析后的数组项类型构造返回类型
    if (resolvedComponentType instanceof Class) {
      return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
    } else {
      // GenericArrayTypeImpl是TypeParameterResolver的内部类，实现了GenericArrayType接口
      return new GenericArrayTypeImpl(resolvedComponentType);
    }
  }

  /**
   * 解析ParameterizedType类型，ParameterizedType类型表示的是参数化类型，类似List<String>的类型。
   *
   * @param parameterizedType
   * @param srcType
   * @param declaringClass
   * @return
   */
  private static ParameterizedType resolveParameterizedType(ParameterizedType parameterizedType, Type srcType, Class<?> declaringClass) {
    // 获得原始类型对应的对象，ParameterizedType.getRawType()返回的是参数类型的原类型，如Map<K,V>返回的是Map
    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
    // 获得类型变量对应的对象，ParameterizedType.getActualTypeArguments()返回的是参数类型中的类型变量，如Map<K,V>返回的是K，V组成的数组
    Type[] typeArgs = parameterizedType.getActualTypeArguments();
    // 用于保存解析后的结果
    Type[] args = new Type[typeArgs.length];
    // 逐个解析typeArgs中的元素，解析过程与ResolveType基本一样
    for (int i = 0; i < typeArgs.length; i++) {
      if (typeArgs[i] instanceof TypeVariable) {
        args[i] = resolveTypeVar((TypeVariable<?>) typeArgs[i], srcType, declaringClass);
      } else if (typeArgs[i] instanceof ParameterizedType) {
        args[i] = resolveParameterizedType((ParameterizedType) typeArgs[i], srcType, declaringClass);
      } else if (typeArgs[i] instanceof WildcardType) {
        args[i] = resolveWildcardType((WildcardType) typeArgs[i], srcType, declaringClass);
      } else {
        args[i] = typeArgs[i];
      }
    }
    // 将解析结果封装成TypeParameterResolver中定义的ParameterizedType实现并返回
    return new ParameterizedTypeImpl(rawType, null, args);
  }

  /**
   * 处理WildcardType类型的变量，WildcardType表示的是通配符泛型，例如？extends Number 和？ super Integer
   *
   * @param wildcardType    要解析的类型
   * @param srcType         解析开始的类
   * @param declaringClass  定义了要解析的类型的类
   * @return 解析好的变量
   */
  private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
    // 解析wildcardType的下边界
    Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
    // 解析wildcardType的上边界
    Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
    return new WildcardTypeImpl(lowerBounds, upperBounds);
  }

  /**
   * 解析WildcardType的边界，解析过程与resolveType类似
   *
   * @param bounds 上界或下界
   * @param srcType 解析的起始类
   * @param declaringClass 定义要解析的字段或者方法的类
   * @return 解析好的类
   */
  private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
    Type[] result = new Type[bounds.length];
    for (int i = 0; i < bounds.length; i++) {
      if (bounds[i] instanceof TypeVariable) {
        result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
      } else if (bounds[i] instanceof ParameterizedType) {
        result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
      } else if (bounds[i] instanceof WildcardType) {
        result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
      } else {
        result[i] = bounds[i];
      }
    }
    return result;
  }

  /**
   * 解析TypeVariable类型，TypeVariable类型表示的是类型变量，如List<T>中的T就是类型变量
   *
   * @param typeVar 类型变量
   * @param srcType 解析的起始类
   * @param declaringClass 定义要解析的字段或者方法的类
   * @return 解析好的类
   */
  private static Type resolveTypeVar(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass) {
    Type result = null;
    Class<?> clazz = null;
    // 判断srcType是哪种Type类型，以此来确定clazz
    if (srcType instanceof Class) {
      clazz = (Class<?>) srcType;
    } else if (srcType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) srcType;
      clazz = (Class<?>) parameterizedType.getRawType();
    } else {
      throw new IllegalArgumentException("The 2nd arg must be Class or ParameterizedType, but was: " + srcType.getClass());
    }

    // 判断clazz是否是type所在的类，即type是否是在clazz中定义的
    if (clazz == declaringClass) {
      // 获取typeVar的上边界，上边界即typeVar的类型所继承的类
      Type[] bounds = typeVar.getBounds();
      // 返回第一个继承的类
      if(bounds.length > 0) {
        return bounds[0];
      }
      // 若不存在上边界，则返回Object.class
      return Object.class;
    }

    // 获取声明的父类类型
    Type superclass = clazz.getGenericSuperclass();
    // 通过扫描父类进行解析，scanSupperTypes中可能会调用resolveTypeVar方法，当前方法就是一个递归方法，一直寻找到合适的父类为止
    result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
    if (result != null) {
      return result;
    }

    // 获取声明的接口
    Type[] superInterfaces = clazz.getGenericInterfaces();
    // 透过过扫描接口进行解析，逻辑与父类相同
    for (Type superInterface : superInterfaces) {
      result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superInterface);
      if (result != null) {
        return result;
      }
    }

    // 若整个继承结构中都没有解析成功，则返回object
    return Object.class;
  }

  /**
   * 将要解析的字段在其父类中解析
   *
   * @param typeVar 类型变量
   * @param srcType 解析的起始类
   * @param declaringClass 定义要解析的字段或者方法的类
   * @param clazz 当前类或当前接口
   * @param superclass 父类或父接口
   * @return 解析好的类
   */
  private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz, Type superclass) {
    if (superclass instanceof ParameterizedType) {
      // 强转为参数化类型，并取出它的原始类型和类型变量列表
      // 这里解释下为什么能强转，这个方法是在当前类class无法解析类型变量的实际类型的时候才会调用的
      // 这种情况下，它的类型变量就是从父类那继承的，那么class的父接口或者父类superclass必然存在类型变量。
      ParameterizedType parentAsType = (ParameterizedType) superclass;
      Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();
      TypeVariable<?>[] parentTypeVars = parentAsClass.getTypeParameters();
      if (srcType instanceof ParameterizedType) {
        // 如果srcType是参数化类型则需要进行转换，将可能存在的如List<T>中的T转为真正的Class对象
        parentAsType = translateParentTypeVars((ParameterizedType) srcType, clazz, parentAsType);
      }
      // 如果字段或者方法所在的类与父类相同，即字段或者方法是在当前类中定义的，则找出parentAsType与TypeVar对应的实际类型
      if (declaringClass == parentAsClass) {
        for (int i = 0; i < parentTypeVars.length; i++) {
          if (typeVar == parentTypeVars[i]) {
            return parentAsType.getActualTypeArguments()[i];
          }
        }
      }
      // 若声明该字段或方法的类是parentClass的父类，则重新调用resolveTypeVar来解析
      if (declaringClass.isAssignableFrom(parentAsClass)) {
        return resolveTypeVar(typeVar, parentAsType, declaringClass);
      }
    } else if (superclass instanceof Class && declaringClass.isAssignableFrom((Class<?>) superclass)) {
      // 若声明该字段或方法的类是parentClass的父类，则重新调用resolveTypeVar来解析
      return resolveTypeVar(typeVar, superclass, declaringClass);
    }
    return null;
  }

  /**
   * 将泛型转为实际类型，如List<T>转为List<String>
   *
   * @param srcType
   * @param srcClass
   * @param parentType
   * @return
   */
  private static ParameterizedType translateParentTypeVars(ParameterizedType srcType, Class<?> srcClass, ParameterizedType parentType) {
    // 获取parentType的参数列表，这个参数列表一般是参数变量的，这个方法要做的就是要尽量把参数变量转为Class
    Type[] parentTypeArgs = parentType.getActualTypeArguments();
    // 获取srcType的参数类型，srcType是从最初的resolveXXX()方法一路进来的变量，他的参数类型一般会是真正的Class类型
    Type[] srcTypeArgs = srcType.getActualTypeArguments();
    // 获取srcType的参数变量，用于与parentTypeArgs对比，将parentTypeArgs中与srcTypeVars相同的转换为对应的srcTypeArgs
    TypeVariable<?>[] srcTypeVars = srcClass.getTypeParameters();
    Type[] newParentArgs = new Type[parentTypeArgs.length];
    boolean noChange = true;
    // 通过嵌套循环，将parentType中与srcTypeVars中相同的元素转换为对应的srcTypeArgs
    for (int i = 0; i < parentTypeArgs.length; i++) {
      if (parentTypeArgs[i] instanceof TypeVariable) {
        for (int j = 0; j < srcTypeVars.length; j++) {
          if (srcTypeVars[j] == parentTypeArgs[i]) {
            noChange = false;
            newParentArgs[i] = srcTypeArgs[j];
          }
        }
      } else {
        newParentArgs[i] = parentTypeArgs[i];
      }
    }
    // 如果经过对比parentTypeArgs没有变化，则说明parentType不需要解析，直接返回；
    // 若有变化，则返回新构建的ParameterizedType，这个ParameterizedTypeImpl是内部类
    return noChange ? parentType : new ParameterizedTypeImpl((Class<?>)parentType.getRawType(), null, newParentArgs);
  }

  private TypeParameterResolver() {
    super();
  }

  static class ParameterizedTypeImpl implements ParameterizedType {
    private Class<?> rawType;

    private Type ownerType;

    private Type[] actualTypeArguments;

    public ParameterizedTypeImpl(Class<?> rawType, Type ownerType, Type[] actualTypeArguments) {
      super();
      this.rawType = rawType;
      this.ownerType = ownerType;
      this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return actualTypeArguments;
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public String toString() {
      return "ParameterizedTypeImpl [rawType=" + rawType + ", ownerType=" + ownerType + ", actualTypeArguments=" + Arrays.toString(actualTypeArguments) + "]";
    }
  }

  static class WildcardTypeImpl implements WildcardType {
    private Type[] lowerBounds;

    private Type[] upperBounds;

    WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
      super();
      this.lowerBounds = lowerBounds;
      this.upperBounds = upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBounds;
    }

    @Override
    public Type[] getUpperBounds() {
      return upperBounds;
    }
  }

  static class GenericArrayTypeImpl implements GenericArrayType {
    private Type genericComponentType;

    GenericArrayTypeImpl(Type genericComponentType) {
      super();
      this.genericComponentType = genericComponentType;
    }

    @Override
    public Type getGenericComponentType() {
      return genericComponentType;
    }
  }
}
