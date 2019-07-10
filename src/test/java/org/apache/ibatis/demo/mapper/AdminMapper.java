package org.apache.ibatis.demo.mapper;


import org.apache.ibatis.demo.model.Admin;

import java.util.List;

/**
 * @Description 管理员数据库操作类
 * @Author Zhang Diwen
 * @Date 2018/11/67 09:23
 */
public interface AdminMapper {

    /**
     * 通过id删除管理员
     *
     * @param id 管理员id
     * @return int
     */
    int deleteById(Long id);

    /**
     * 插入管理员信息
     *
     * @param record 管理员对象
     * @return int
     */
    int insert(Admin record);

    /**
     * 更新管理员
     *
     * @param record 管理员对象
     * @return int
     */
    int update(Admin record);

    /**
     * 通过邮箱密码获得管理员
     *
     * @param record 管理员对象
     * @return User
     */
    Admin getByEmailAndPassword(Admin record);

    /**
     * 通过id获得管理员
     *
     * @param id 管理员id
     * @return User
     */
    Admin get(Long id);

    Admin getByEmail(String email);

    List<Admin> listAdmins();
}
