package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ControllerValidaciones {
    private List<Integer> getByUuid;
    private List<Integer> paginate;
    private List<Integer> save;
    private List<Integer> delete;
}
