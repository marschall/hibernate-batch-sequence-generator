package com.github.marschall.hibernate.batchsequencegenerators;

import org.hibernate.dialect.Dialect;

/**
 * A sequence generator that uses a recursive common table expression
 * to fetch multiple values from a sequence.
 *
 * <h2>SQL</h2>
 * The generated SELECT will look something like this
 * <pre></code>
 * WITH RECURSIVE t(n, level_num) AS (
 *     SELECT nextval(seq_xxx) as n, 1 as level_num
 *   UNION ALL
 *     SELECT nextval(seq_xxx) as n, level_num + 1 as level_num
 *     FROM t
 *     WHERE level_num &lt; ?)
 * SELECT n FROM t;
 * </code></pre>
 */
public class WithRecursiveGenerator extends BatchIdentifierGenerator {

  @Override
  String buildSelect(String sequenceName, Dialect dialect) {
    return "WITH RECURSIVE t(n, level_num) AS ("
            + "SELECT " + dialect.getSelectSequenceNextValString(sequenceName) + " as n, 1 as level_num "
            + "UNION ALL "
            + "SELECT " + dialect.getSelectSequenceNextValString(sequenceName) + " as n, level_num + 1 as level_num "
            + " FROM t "
            + " WHERE level_num < ?) "
            + "SELECT n FROM t";
  }


}
