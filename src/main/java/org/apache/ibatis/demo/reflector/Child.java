package org.apache.ibatis.demo.reflector;

/**
 * TODO
 *
 * @author Fantasy Dream
 * @date 2019/6/28
 */
public class Child<T> extends Parent<T,T> {
    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
