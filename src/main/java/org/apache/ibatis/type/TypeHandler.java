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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

  /**
   * 通过预处理语句绑定参数，我们传入参数，和对应的jdbcType，会由方法自动完成Java类型到jdbcType的转换
   *
   * @param ps 预处理语句
   * @param i 参数序号
   * @param parameter 要传入的数据
   * @param jdbcType 参数对应的jdbcType
   * @throws SQLException sql异常
   */
  void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

  /**
   * 这三个方法，分别使用不同的方式获得结果集，结果集会从jdbcType转成Java类型
   *
   * @param rs 结果集
   * @param columnName 列名
   * @return 该列对应的数据对象
   * @throws SQLException sql异常
   */
  T getResult(ResultSet rs, String columnName) throws SQLException;

  T getResult(ResultSet rs, int columnIndex) throws SQLException;

  T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
