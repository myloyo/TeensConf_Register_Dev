package com.teensconf.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BirthDateValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBirthDate {
    String message() default "Неверная дата рождения";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}