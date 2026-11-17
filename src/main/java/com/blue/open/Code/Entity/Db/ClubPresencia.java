package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Table(
        name = "club_presencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_club_presencia_company_user",
                columnNames = {"company_id", "user_id"}
        )
)
@Entity
public class ClubPresencia extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "presente", nullable = false)
    private boolean presente;

    @Column(name = "ultimo_acceso_at", nullable = false)
    private LocalDateTime ultimoAccesoAt;
}
