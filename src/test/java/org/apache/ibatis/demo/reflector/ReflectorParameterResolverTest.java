package org.apache.ibatis.demo.reflector;

import org.apache.ibatis.reflection.TypeParameterResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * TODO
 *
 * @author Fantasy Dream
 * @date 2019/6/28
 */
public class ReflectorParameterResolverTest {

    Child<String> child = new Child<>();

    @Test
    public void test(){
        try {
            Field field = Child.class.getField("map");
            field.setAccessible(true);
            System.out.println(field.getGenericType());
            System.out.println(field.getGenericType() instanceof ParameterizedType);

            Type type = TypeParameterResolver.resolveFieldType(field, ReflectorParameterResolverTest.class.getDeclaredField(
                    "child").getGenericType());
            System.out.println(type.getClass());
            ParameterizedType p = (ParameterizedType) type;
            System.out.println(p.getOwnerType());
            System.out.println(p.getRawType());
            for (Type t : p.getActualTypeArguments()) {
                System.out.println(t);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
