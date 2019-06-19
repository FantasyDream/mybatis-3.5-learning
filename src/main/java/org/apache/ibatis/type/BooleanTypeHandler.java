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
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class BooleanTypeHandler extends BaseTypeHandler<Boolean> {

  /**
   * 构建PreparedStatement时，在对应的位置，set对应的类型
   *
   * @param ps
   * @param i
   * @param parameter
   * @param jdbcType
   * @throws SQLException
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBoolean(i, parameter);
  }

  /**
   * 根据列名，来获取Boolean类型
   *
   * @param rs
   * @param columnName
   * @return
   * @throws SQLException
   */
  @Override
  public Boolean getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    boolean result = rs.getBoolean(columnName);
    return !result && rs.wasNull() ? null : result;
  }

  /**
   * 根据列的位数，获取boolean类型的数据
   *
   * @param rs
   * @param columnIndex
   * @return
   * @throws SQLException
   */
  @Override
  public Boolean getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    boolean result = rs.getBoolean(columnIndex);
    return !result && rs.wasNull() ? null : result;
  }

  /**
   * 在存储过程的结果中，获取Boolean类型的数据
   *
   * @param cs
   * @param columnIndex
   * @return
   * @throws SQLException
   */
  @Override
  public Boolean getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    boolean result = cs.getBoolean(columnIndex);
    return !result && cs.wasNull() ? null : result;
  }
}
