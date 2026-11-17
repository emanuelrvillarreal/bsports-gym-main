package com.blue.open.Core.Entity.DTO;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class AbstractEntityDTO {
    protected String uuid;
    protected int status;
    protected Timestamp dateCreated;

    public void setAbstract(AbstractEntity objeto) {
        this.uuid = objeto.getUuid();
        this.status = objeto.getStatus();
        this.dateCreated = objeto.getDateCreated();
    }
}
