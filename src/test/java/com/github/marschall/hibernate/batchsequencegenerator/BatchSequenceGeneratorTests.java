package com.github.marschall.hibernate.batchsequencegenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class BatchSequenceGeneratorTests {

  @Test
  void firebirdDialectClass() {
    assertNotNull(BatchSequenceGenerator.FIREBIRD_DIALECT);
  }

}
