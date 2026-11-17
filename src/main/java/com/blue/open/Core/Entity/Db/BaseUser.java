package com.blue.open.Core.Entity.Db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@MappedSuperclass
public abstract class BaseUser extends AbstractEntity {

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCompanyRole> userCompanyRoles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAccessToken> tokens = new ArrayList<>();

    @Column(unique = true)
    private String username;
    private String password;
    private String ssoUuid;
    private String malvinasLegajo;

    private String name;
    private String departmentName;
    private String groupName;
    private String email;
    private String phone;

    private String verificationCode;
    private Instant verificationCodeExpiry;
    private Instant verificationCodeLastSent;
}
