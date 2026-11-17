package com.blue.open.Code.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidClubRegistrationValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidClubRegistration {

    String message() default "Datos del administrador inválidos.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
