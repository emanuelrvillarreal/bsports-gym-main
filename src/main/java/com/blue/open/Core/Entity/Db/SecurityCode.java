package com.blue.open.Core.Entity.Db;

import com.blue.open.Code.Entity.Db.User;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class SecurityCode extends AbstractEntity {

    private String code;

    private Instant createdAt;

    private String purpose;

    private Boolean used = false;

    @ManyToOne
    private User user;

}
