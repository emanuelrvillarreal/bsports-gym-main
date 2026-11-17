package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RepositoryImplQuery {
    private String select;
    private String count;
}
