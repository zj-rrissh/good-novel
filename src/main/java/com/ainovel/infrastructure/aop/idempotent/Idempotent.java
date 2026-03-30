package com.ainovel.infrastructure.aop.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    IdempotentStrategy strategy();

    String fingerprint() default "";

    String tokenParam() default "idempotencyKey";

    long ttl() default 10;

    TimeUnit timeUnit() default TimeUnit.MINUTES;

    boolean optional() default false;

    String message() default "";
}
