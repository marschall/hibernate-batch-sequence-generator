package com.github.marschall.hibernate.batchsequencegenerators;

import org.hibernate.dialect.Dialect;

/**
 * A sequence generator that uses Oracle CONNECT BY syntax
 * to fetch multiple values from a sequence.
 * <p>
 * Oracle does not support using recursive common table expressions in
 * order to fetch multiple values from a sequence.
 * <h2>SQL</h2>
 * The generated SELECT will look something like this
 * <pre></code>
 * SELECT seq_xxx.nextval
 * FROM dual
 * CONNECT BY rownum <= ?
 * </code></pre>
 */
public class ConnectByGenerator extends BatchIdentifierGenerator {

  @Override
  String buildSelect(String sequenceName, Dialect dialect) {
    return "SELECT " + dialect.getSelectSequenceNextValString(sequenceName) + " FROM dual CONNECT BY rownum <= ?";
  }

}
