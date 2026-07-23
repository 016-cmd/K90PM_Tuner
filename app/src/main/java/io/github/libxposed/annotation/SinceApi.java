package io.github.libxposed.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates the minimum API version required for the annotated element.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface SinceApi {
    int value();
}