package com.helger.phase4.v3;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Internal interface to act as a marker for future V3 changes.
 *
 * @author Philip Helger
 */
@Retention (RetentionPolicy.SOURCE)
@Target ({ ElementType.TYPE, ElementType.METHOD })
public @interface ChangeV3
{
  String value() default "";
}
