package org.apache.ibatis.demo.reflector;

import java.util.Map;

/**
 * TODO
 *
 * @author Fantasy Dream
 * @date 2019/6/28
 */
public class Parent<K,V> {

    public Map<K,V> map;

    public Map<K, V> getMap() {
        return map;
    }

    public void setMap(Map<K, V> map) {
        this.map = map;
    }
}
