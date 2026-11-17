package com.blue.open.Core.Entity.Db;

import com.blue.open.Code.Entity.Db.User;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "auth_user_access_tokens")
@Entity()
public class UserAccessToken extends AbstractEntity {
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String name;
    private String code;
    private int active;
}
