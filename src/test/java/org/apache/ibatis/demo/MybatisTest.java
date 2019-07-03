package org.apache.ibatis.demo;

import org.apache.ibatis.demo.mapper.AdminMapper;
import org.apache.ibatis.demo.model.Admin;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * TODO
 *
 * @author Fantasy Dream
 * @date 2019/6/24
 */
public class MybatisTest {
    @Test
    public void test() {
        SqlSessionFactory sessionFactory = null;
        String resource = "resources/configuration.xml";
        try {
            sessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream(resource));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        SqlSession session = sessionFactory.openSession();
        AdminMapper adminMapper = session.getMapper(AdminMapper.class);
        Admin admin = adminMapper.get(1L);
        System.out.println(admin);
        admin.setEmail("7@q.com");
        adminMapper.update(admin);
        System.out.println(admin);
        session.commit();
    }
}
