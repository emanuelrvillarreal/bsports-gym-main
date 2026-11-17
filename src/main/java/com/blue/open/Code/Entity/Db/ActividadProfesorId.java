package com.blue.open.Code.Entity.Db;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class ActividadProfesorId implements Serializable {

    private Long actividadId;
    private Long profesorId;
}
