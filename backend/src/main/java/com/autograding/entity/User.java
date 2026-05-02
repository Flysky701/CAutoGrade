package com.autograding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    public enum Role {
        STUDENT,
        TEACHER,
        ADMIN
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String code;

    @TableField("password_hash")
    private String passwordHash;

    private String nickname;

    private String avatar;

    private Role role;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
