package org.apache.ibatis.demo;

import org.apache.ibatis.demo.mapper.AdminMapper;
import org.apache.ibatis.demo.model.Admin;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * TODO
 *
 * @author Fantasy Dream
 * @date 2019/6/24
 */
public class MybatisTest {
    @Test
    public void test() {
        String resource = "resources/configuration.xml";
        try {
            SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream(resource));
            SqlSession session = sessionFactory.openSession();
            AdminMapper adminMapper = session.getMapper(AdminMapper.class);
            List<Admin> adminList = adminMapper.listAdmins();
            adminList.forEach(System.out::println);
            session.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void jdbc() {
        String driver="com.mysql.jdbc.Driver";
        String url="jdbc:mysql://localhost:3306/lucky_shop?characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC";
        String username="root";
        String password="123456";
        String sql = "select * from t_admin";

        try {
            Class.forName(driver);
            Connection connection = DriverManager.getConnection(url,username,password);
            connection.setAutoCommit(false);
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            int col = rs.getMetaData().getColumnCount();
            System.out.println("====================================================");
            while(rs.next()) {
                Admin admin = new Admin();
                admin.setId(rs.getLong(1));
                admin.setEmail(rs.getString(2));
                admin.setPassword(rs.getString(3));
                admin.setCreateTime(rs.getTimestamp(4));
                admin.setModifyTime(rs.getTimestamp(5));
                System.out.println(admin);
            }
            System.out.println("====================================================");
            connection.commit();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
