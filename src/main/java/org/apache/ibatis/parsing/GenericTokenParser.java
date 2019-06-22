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
package org.apache.ibatis.parsing;

/**
 * 占位符解析器,用于解析XML文件中的占位符
 */
public class GenericTokenParser {

  /**
   * 占位符开始标记
   */
  private final String openToken;
  /**
   * 占位符结束标记
   */
  private final String closeToken;
  /**
   * 占位符解析,得到占位符的值
   */
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // 寻找占位符开始值,并检测是否含有占位符
    int start = text.indexOf(openToken);
    if (start == -1) {
      return text;
    }
    char[] src = text.toCharArray();
    int offset = 0;
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;
    while (start > -1) {
      if (start > 0 && src[start - 1] == '\\') {
        // 遇到转义的开始标记(反斜杠),直接删除并继续.
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
      } else {
        // 已找到开始字符,现在寻找结束字符
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        // 将开始字符前面的字符追加到builder中
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        // 从offset后面查找结束字符
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if (end > offset && src[end - 1] == '\\') {
            // 处理转义的结束字符
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            end = text.indexOf(closeToken, offset);
          } else {
            // 将开始标记和结束标记中间的字符放到expression中.
            expression.append(src, offset, end - offset);
            offset = end + closeToken.length();
            break;
          }
        }
        if (end == -1) {
          // 未找到结束字符
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          // 将占位符包含的值交给TokenHandler处理,并将处理结果追加到builder中保存. 最终组成完整内容
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      start = text.indexOf(openToken, offset);
    }
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
