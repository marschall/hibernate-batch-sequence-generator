package com.github.marschall.hibernate.batchsequencegenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/**
 * Meta annotation to use {@link BatchSequenceGenerator} as an identifier generator.
 */
@IdGeneratorType(BatchSequenceGenerator.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface BatchSequence {

  /**
   * Returns the name of the sequence to use.
   * 
   * @return the name of the sequence to use
   */
  String name();

  /**
   *  Returns how many sequence values to fetch at once.
   * 
   * @return how many sequence values to fetch at once, must be positive
   */
  int fetchSize() default BatchSequenceGenerator.DEFAULT_FETCH_SIZE;

}
