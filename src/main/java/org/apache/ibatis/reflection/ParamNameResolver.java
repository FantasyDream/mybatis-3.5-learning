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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public class ParamNameResolver {

  private static final String GENERIC_NAME_PREFIX = "param";

  /**
   * 记录参数在参数列表中的位置索引和参数的名称，key为位置索引，value为@Param指定的名称，若没指定，则使用索引
   * 在map中，不会记录RowBounds和ResultHandler类型的参数，但作为key的索引仍会记录参数在原参数列表的索引
   * 而作为值的索引，则记录的是去掉RowBounds和ResultHandler的参数列表的位置索引
   * 下面是官方的例子，左边是方法声明，右边是对应的names：
   *
   * aMethod(@Param("M") int a, @Param("N") int b)  {{0, "M"}, {1, "N"}}
   * aMethod(int a, int b)                          {{0, "0"}, {1, "1"}}
   * aMethod(int a, RowBounds rb, int b)            {{0, "0"}, {2, "1"}}
   */
  private final SortedMap<Integer, String> names;

  /**
   * 记录方法是否使用了@Param注解
   */
  private boolean hasParamAnnotation;

  public ParamNameResolver(Configuration config, Method method) {
    // 获得参数的类型和注解
    final Class<?>[] paramTypes = method.getParameterTypes();
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    // 用于记录参数索引和参数名称的对应关系
    final SortedMap<Integer, String> map = new TreeMap<>();
    int paramCount = paramAnnotations.length;
    // 从@Param中获取数据放入到 names中
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // 如果参数是RowBounds类型或者ResultHandler类型，则跳过对该参数的分析
        continue;
      }
      // 从@Param中获取name属性
      String name = null;
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      if (name == null) {
        // 该参数没有对应的@Param注解，则根据配置信息决定是否用参数实际名称来作为其名称
        if (config.isUseActualParamName()) {
          name = getActualParamName(method, paramIndex);
        }
        if (name == null) {
          // 使用跳过ResultHandler，RowBounds的索引作为其名称
          name = String.valueOf(map.size());
        }
      }
      // 记录到map中保存
      map.put(paramIndex, name);
    }
    // 初始化names集合
    names = Collections.unmodifiableSortedMap(map);
  }

  private String getActualParamName(Method method, int paramIndex) {
    return ParamNameUtil.getParamNames(method).get(paramIndex);
  }

  /**
   * 过滤两种类型的参数
   * @param clazz
   * @return
   */
  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * Returns parameter names referenced by SQL providers.
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   * <p>
   * A single non-special parameter is returned without a name.
   * Multiple parameters are named using the naming rule.
   * In addition to the default names, this method also adds the generic names (param1, param2,
   * ...).
   * </p>
   */
  public Object getNamedParams(Object[] args) {
    final int paramCount = names.size();
    if (args == null || paramCount == 0) {
      // 无参数，返回null
      return null;
    } else if (!hasParamAnnotation && paramCount == 1) {
      // 未使用@Param且只有一个参数，直接将参数返还,不带参数名称
      return args[names.firstKey()];
    } else {
      // 处理使用@Param注解指定了参数名称或有多个参数的情况
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        // 将参数名与实参对应关系记录到parameter中
        param.put(entry.getValue(), args[entry.getKey()]);
        // 下面是为参数创建"param+索引"格式的默认参数名称，如 param1， param2等，并添加到param集合
        final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
        // 如果@Param注解指定的参数名称就是"param+索引"的格式,则不需要再添加
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }
}
