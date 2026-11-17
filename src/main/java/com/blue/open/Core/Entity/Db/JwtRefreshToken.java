package com.blue.open.Core.Entity.Db;

import com.blue.open.Code.Entity.Db.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Table(name = "auth_jwt_refresh_tokens")
@Entity
public class JwtRefreshToken extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 1024)
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;
}
