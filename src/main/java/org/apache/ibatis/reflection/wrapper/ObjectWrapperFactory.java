/**
 *    Copyright 2009-2016 the original author or authors.
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

import org.apache.ibatis.reflection.MetaObject;

/**
 * 该接口的默认实现DefaultObjectWrapperFactory实际上式无法使用的,所以这个接口只能由我们在配置文件中自定义进行扩展
 *
 * @author Clinton Begin
 */
public interface ObjectWrapperFactory {

  /**
   * 是否有ObjectWrapper对应object
   *
   * @param object
   * @return
   */
  boolean hasWrapperFor(Object object);

  /**
   * 获得ObjectWrapper
   *
   * @param metaObject
   * @param object
   * @return
   */
  ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);

}
