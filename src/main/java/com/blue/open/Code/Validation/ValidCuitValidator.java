package com.blue.open.Code.Validation;

import com.blue.open.Code.Service.CuitValidatorService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidCuitValidator implements ConstraintValidator<ValidCuit, String> {

    @Autowired
    private CuitValidatorService cuitValidatorService;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return cuitValidatorService.isValid(value.trim());
    }
}
