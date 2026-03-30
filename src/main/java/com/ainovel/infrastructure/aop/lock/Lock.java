package com.ainovel.infrastructure.aop.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {

    String key();

    LockType type() default LockType.REENTRANT;

    long waitTime() default -1;

    long leaseTime() default -1;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    String failMessage() default "";
}
