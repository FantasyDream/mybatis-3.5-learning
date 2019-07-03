package org.apache.ibatis.demo.model;

/**
 * @Description 管理员类
 * @Author Zhang Diwen
 * @Date 2018/12/7 11:09
 */
public class Admin {
    private Long id;
    private String email;
    private String password;
    private java.sql.Timestamp createTime;
    private java.sql.Timestamp modifyTime;

    public java.sql.Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.sql.Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public java.sql.Timestamp getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(java.sql.Timestamp modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", createTime=" + createTime +
                ", modifyTime=" + modifyTime +
                '}';
    }
}
