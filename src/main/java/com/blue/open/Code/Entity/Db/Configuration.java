package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Table(name = "configurations")
@Entity()
@AllArgsConstructor
@NoArgsConstructor
public class Configuration extends AbstractEntity {
    @NotNull
    private String name;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String value;

    @NotNull
    @Column(columnDefinition = "BIT")
    private boolean global;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
