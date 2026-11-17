package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleValue {
    private String string;
    private Integer integer;
    private Date date;
    private LocalDateTime datetime;

    public SimpleValue(String string) {
        this.string = string;
    }

    public SimpleValue(Integer integer) {
        this.integer = integer;
    }

    public SimpleValue(Date date) {
        this.date = date;
    }
}