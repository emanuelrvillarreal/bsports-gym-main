package com.blue.open.Core.Entity.Db;

import com.blue.open.Core.Entity.DTO.AbstractEntityDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    protected Long id;

    @Column(name = "status", columnDefinition = "TINYINT DEFAULT 0", nullable = false)
    @JdbcTypeCode(Types.TINYINT)
    protected Integer status = 0;

    // UUID: Gestionado por Java (sin default en BD para evitar errores de exportación)
    @Column(name = "uuid", length = 36, unique = true, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    protected String uuid;

    @JsonIgnore
    protected Long idUserCreated;
    @JsonIgnore
    protected Long idUserUpdated;
    @JsonIgnore
    protected Long idUserDeleted;

    @CreationTimestamp
    @Column(name = "date_created", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    protected Timestamp dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    protected Timestamp dateUpdated;

    @Column(name = "date_deleted")
    protected Timestamp dateDeleted;

    @PrePersist
    protected void onCreate() {
        if (this.uuid == null || this.uuid.isEmpty()) {
            this.uuid = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = 0;
        }
    }

    public void setAbstract(AbstractEntityDTO dto) {
        final int ACTIVE = 0;
        final int DELETED = -1;

        if (this.status == ACTIVE && dto.getStatus() == DELETED) {
            this.status = DELETED;
        } else if (this.status == DELETED && dto.getStatus() == ACTIVE) {
            throw new CustomException("No se puede activar un elemento que fue eliminado",
                    "AbstractEntity | setAbstract | error uuid: " + uuid);
        } else if (dto.getStatus() != ACTIVE && dto.getStatus() != DELETED) {
            throw new CustomException("Error status",
                    "AbstractEntity | setAbstract | error status: " + dto.getStatus());
        }
    }
}