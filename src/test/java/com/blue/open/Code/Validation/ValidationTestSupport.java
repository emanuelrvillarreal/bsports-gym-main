package com.blue.open.Code.Validation;

import jakarta.validation.ConstraintValidatorContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

final class ValidationTestSupport {

    private ValidationTestSupport() {
    }

    static ConstraintValidatorContext mockContext() {
        return mock(ConstraintValidatorContext.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
    }
}
