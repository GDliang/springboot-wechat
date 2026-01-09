// [file name]: Admin.java（修改后的版本）
package com.goldenegg.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "admin_users")
@Data
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String openid;          // 微信openid（唯一且非空）

    @Column(name = "username")
    private String username;        // 管理员账号（可选，改为可空）

    @Column
    private String password;        // 密码（可选）

    @Column(name = "nickname")
    private String nickname;        // 管理员昵称

    @Column(name = "role")
    private String role = "admin";  // 角色：admin/super_admin

    @Column(name = "is_active")
    private Boolean isActive = true; // 是否启用

    @Column(name = "last_login")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;         // 最后登录时间

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}