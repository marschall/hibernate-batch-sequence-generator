package com.github.marschall.hibernate.batchsequencegenerator;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A sequence generator that uses a recursive query to fetch multiple
 * values from a sequence in a single database access.
 *
 * <h2>Configuration</h2>
 * <pre><code>
 * &commat;Id
 * &commat;BatchSequence(name = "SOME_SEQUENCE_NAME", fetch_size = SOME_FETCH_SIZE_VALUE)
 * private Long someColumnName;
 * </code></pre>
 *
 * <h2>SQL</h2>
 * Per default the generated SELECT will look something like this
 * <pre><code>
 * WITH RECURSIVE t(n) AS (
 *   SELECT 1
 *     UNION ALL
 *   SELECT n + 1
 *   FROM t
 *   WHERE n &lt; ?)
 * SELECT nextval(seq_xxx)
 *   FROM t;
 * </code></pre>
 *
 * <h3>DB2</h3>
 * For DB2 the generated SELECT will look something like this
 * <pre><code>
 * WITH t(n) AS (
 *   SELECT 1 as n
 *     FROM (VALUES 1)
 *       UNION ALL
 *     SELECT n + 1 as n
 *       FROM t
 *      WHERE n &lt; ?)
 * SELECT next value for SEQ_CHILD_ID as n
 *   FROM t;
 * </code></pre>
 *
 * <h3>HSQLDB</h3>
 * For HSQLDB the generated SELECT will look something like this
 * <pre><code>
 * SELECT next value for seq_parent_id
 *   FROM UNNEST(SEQUENCE_ARRAY(1, ?, 1));
 * </code></pre>
 *
 * <h3>Oracle</h3>
 * For Oracle the generated SELECT will look something like this
 * because Oracle does not support using recursive common table
 * expressions to fetch multiple values from a sequence.
 * <pre><code>
 * SELECT seq_xxx.nextval
 * FROM dual
 * CONNECT BY level &lt;= ?
 * </code></pre>
 *
 * <h3>SQL Server</h3>
 * For SQL Server the generated SELECT will look something like this
 * <pre><code>
 * WITH t(n) AS (
 *   SELECT 1 as n
 *     UNION ALL
 *   SELECT n + 1 as n
 *     FROM t
 *    WHERE n &lt; ?)
 * SELECT NEXT VALUE FOR seq_xxx as n
 *   FROM t
 * </code></pre>
 *
 * <h3>Firebird</h3>
 * For Firebird the generated SELECT will look something like this
 * <pre><code>
 * WITH RECURSIVE t(n, level_num) AS (
 *   SELECT NEXT VALUE FOR seq_xxx as n, 1 as level_num
 *   FROM rdb$database
 *     UNION ALL
 *   SELECT NEXT VALUE FOR seq_xxx as n, level_num + 1 as level_num
 *     FROM t
 *    WHERE level_num &lt; ?)
 * SELECT n
 *   FROM t
 * </code></pre>
 *
 * <h2>Database Support</h2>
 * The following RDBMS have been verified to work
 * <ul>
 *  <li>DB2</li>
 *  <li>Firebird</li>
 *  <li>Oracle</li>
 *  <li>H2</li>
 *  <li>HSQLDB</li>
 *  <li>MariaDB</li>
 *  <li>Postgres</li>
 *  <li>SQL Sever</li>
 * </ul>
 * In theory any RDBMS that supports {@code WITH RECURSIVE} and
 * sequences is supported.
 */
public final class BatchSequenceGenerator implements BulkInsertionCapableIdentifierGenerator, IdentifierGenerator {

  /**
   * Indicates the name of the sequence to use, mandatory.
   * 
   * @deprecated use {@link BatchSequence}
   */
  @Deprecated
  public static final String SEQUENCE_PARAM = "sequence";

  /**
   * Indicates how many sequence values to fetch at once. The default value is {@link #DEFAULT_FETCH_SIZE}.
   * 
   * @deprecated use {@link BatchSequence}
   */
  @Deprecated
  public static final String FETCH_SIZE_PARAM = "fetch_size";

  /**
   * The default value for {@link #FETCH_SIZE_PARAM}.
   */
  public static final int DEFAULT_FETCH_SIZE = 10;

  private final Lock lock = new ReentrantLock();

  private String select;
  private int fetchSize;
  private IdentifierPool identifierPool;
  private IdentifierExtractor identifierExtractor;
  private DatabaseStructure databaseStructure;

  private String sequenceName;

  /**
   * Called if {@link BatchSequence} is used.
   * 
   * @param annotation the annotation
   * @param annotatedMember the annoated field or getter
   * @param context the context
   */
  public BatchSequenceGenerator(BatchSequence annotation,
          Member annotatedMember,
          CustomIdGeneratorCreationContext context) {
    JdbcEnvironment jdbcEnvironment = context.getServiceRegistry().getService(JdbcEnvironment.class);
    this.sequenceName = determineSequenceName(annotation);
    this.fetchSize = annotation.fetchSize();
    
    Class<?> type = getType(annotatedMember);
    this.identifierExtractor = IdentifierExtractor.getIdentifierExtractor(type);
    this.databaseStructure = this.buildDatabaseStructure(type, sequenceName, jdbcEnvironment, "orm");
  }

  private static Class<?> getType(Member annotatedMember) {
    if (annotatedMember instanceof Field) {
      return ((Field) annotatedMember).getType();
    } else if (annotatedMember instanceof Method) {
      return ((Method) annotatedMember).getReturnType();
    } else {
      throw new IllegalArgumentException("unknown member type: " + annotatedMember);
    }
  }

  /**
   * Called if {@link GenericGenerator} is used.
   */
  public BatchSequenceGenerator() {
    super();
  }

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
    if (this.sequenceName == null) {
      // GenericGenerator is used, default constructor was called
      JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
      this.sequenceName = determineSequenceName(params);
      this.fetchSize = determineFetchSize(params);
      Class<?> returnedClass = type.getReturnedClass();
      this.identifierExtractor = IdentifierExtractor.getIdentifierExtractor(returnedClass);
      String contributor = this.determineContributor(params);
      this.databaseStructure = this.buildDatabaseStructure(type.getReturnedClass(), sequenceName, jdbcEnvironment, contributor);
    }
  }

  @Override
  public void initialize(SqlStringGenerationContext context) {
    this.identifierPool = IdentifierPool.empty();
    this.databaseStructure.initialize(context);
    this.select = buildSelect(context, sequenceName);
  }

  private static String buildSelect(SqlStringGenerationContext context, String sequenceName) {
    Dialect dialect = context.getDialect();
    Identifier identifier = context.toIdentifier(sequenceName);
    String nextValString = dialect.getSequenceSupport().getSelectSequenceNextValString(identifier.render(dialect));
    if (dialect instanceof org.hibernate.dialect.OracleDialect) {
      return "SELECT " + nextValString + " FROM dual CONNECT BY level <= ?";
    }
    if (dialect instanceof org.hibernate.dialect.SQLServerDialect) {
      // No RECURSIVE
      return "WITH t(n) AS ( "
          + "SELECT 1 as n "
          + "UNION ALL "
          +"SELECT n + 1 as n FROM t WHERE n < ?) "
          // sequence generation outside of WITH
          + "SELECT " + nextValString + " as n FROM t";
    }
    if (dialect instanceof org.hibernate.dialect.DB2Dialect) {
      // No RECURSIVE
      return "WITH t(n) AS ( "
          + "SELECT 1 as n "
          // difference
          + "FROM (VALUES 1) "
          + "UNION ALL "
          +"SELECT n + 1 as n FROM t WHERE n < ?) "
      // sequence generation outside of WITH
          + "SELECT " + nextValString + " as n FROM t";
    }
    if (dialect instanceof org.hibernate.dialect.HSQLDialect) {
      // https://stackoverflow.com/questions/44472280/cte-based-sequence-generation-with-hsqldb/52329862
      return "SELECT " + nextValString + " FROM UNNEST(SEQUENCE_ARRAY(1, ?, 1))";
    }
    if (dialect instanceof org.hibernate.community.dialect.FirebirdDialect) {
      return "WITH RECURSIVE t(n, level_num) AS ("
              + "SELECT " + nextValString + " as n, 1 as level_num "
              // difference
              + " FROM rdb$database "
              + "UNION ALL "
              + "SELECT " + nextValString + " as n, level_num + 1 as level_num "
              + " FROM t "
              + " WHERE level_num < ?) "
              + "SELECT n FROM t";
    }
    return "WITH RECURSIVE t(n) AS ("
            + "SELECT 1 "
            + "UNION ALL "
            + "SELECT n + 1 "
            + " FROM t "
            + " WHERE n < ?) "
            + "SELECT " + nextValString + " FROM t";
  }

  private SequenceStructure buildDatabaseStructure(Class<?> type, String sequenceName, JdbcEnvironment jdbcEnvironment, String contributor) {
    Identifier identifier = jdbcEnvironment.getIdentifierHelper().toIdentifier(sequenceName);
    QualifiedName qualifiedSequenceName = new QualifiedNameParser.NameParts(null, null, identifier);
    return new SequenceStructure(jdbcEnvironment, contributor,
            qualifiedSequenceName, 1, 1, type);
  }

  private String determineContributor(Properties params) {
    final String contributor = params.getProperty(IdentifierGenerator.CONTRIBUTOR_NAME);
    return contributor == null ? "orm" : contributor;
  }

  private static String determineSequenceName(Properties params) {
    String sequenceName = params.getProperty(SEQUENCE_PARAM);
    if (sequenceName == null) {
      throw new MappingException("no squence name specified");
    }
    return sequenceName;
  }
  
  private static String determineSequenceName(BatchSequence annotation) {
    String sequenceName = annotation.name();
    if (sequenceName == null || sequenceName.isBlank()) {
      throw new MappingException("no squence name specified");
    }
    return sequenceName;
  }

  private static int determineFetchSize(Properties params) {
    int fetchSize = ConfigurationHelper.getInt(FETCH_SIZE_PARAM, params, DEFAULT_FETCH_SIZE);
    if (fetchSize <= 0) {
      throw new MappingException("fetch size must be positive");
    }
    return fetchSize;
  }

  @Override
  public boolean supportsBulkInsertionIdentifierGeneration() {
    return true;
  }

  @Override
  public boolean supportsJdbcBatchInserts() {
    return true;
  }

  @Override
  public String determineBulkInsertionIdentifierGenerationSelectFragment(SqlStringGenerationContext context) {
    return context.getDialect().getSequenceSupport().getSelectSequenceNextValString(this.getSequenceName());
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) {
    this.lock.lock();
    try {
      if (this.identifierPool.isEmpty()) {
        this.identifierPool = this.replenishIdentifierPool(session);
      }
      return this.identifierPool.next();
    } finally {
      this.lock.unlock();
    }
  }

  private String getSequenceName() {
    return this.databaseStructure.getPhysicalName().render();
  }

  @Override
  public void registerExportables(Database database) {
    this.databaseStructure.registerExportables(database);
  }

  private IdentifierPool replenishIdentifierPool(SharedSessionContractImplementor session)
          throws HibernateException {
    JdbcCoordinator coordinator = session.getJdbcCoordinator();
    List<Serializable> identifiers = new ArrayList<>(this.fetchSize);
    try (PreparedStatement statement = coordinator.getStatementPreparer().prepareStatement(this.select)) {
      statement.setFetchSize(this.fetchSize);
      statement.setInt(1, this.fetchSize);
      try (ResultSet resultSet = coordinator.getResultSetReturn().extract(statement, this.select)) {
        while (resultSet.next()) {
          identifiers.add(this.identifierExtractor.extractIdentifier(resultSet));
        }
      }
    } catch (SQLException e) {
      throw session.getJdbcServices().getSqlExceptionHelper().convert(
              e, "could not get next sequence value", this.select);
    }
    if (identifiers.size() != this.fetchSize) {
      throw new IdentifierGenerationException("expected " + this.fetchSize + " values from " + this.getSequenceName()
              + " but got " + identifiers.size());
    }
    return IdentifierPool.forList(identifiers);
  }

  /**
   * Holds a number of prefetched identifiers.
   */
  static final class IdentifierPool {

    private final Iterator<Serializable> iterator;

    private IdentifierPool (List<Serializable> identifiers) {
      this.iterator = identifiers.iterator();
    }

    static IdentifierPool forList(List<Serializable> identifiers) {
      return new IdentifierPool(identifiers);
    }

    static IdentifierPool empty() {
      return new IdentifierPool(Collections.emptyList());
    }

    boolean isEmpty() {
      return !this.iterator.hasNext();
    }

    Serializable next() {
      return this.iterator.next();
    }

  }

  /**
   * Extracts a {@link Serializable} identifier from a {@link ResultSet}.
   *
   * @see org.hibernate.id.IntegralDataTypeHolder
   */
  @FunctionalInterface
  interface IdentifierExtractor {

    IdentifierExtractor SHORT_IDENTIFIER_EXTRACTOR = (ResultSet resultSet) -> {
      short shortValue = resultSet.getShort(1);
      if (resultSet.wasNull()) {
        throw new IdentifierGenerationException("sequence returned null");
      }
      return shortValue;
    };

    IdentifierExtractor INTEGER_IDENTIFIER_EXTRACTOR = (ResultSet resultSet) -> {
      int intValue = resultSet.getInt(1);
      if (resultSet.wasNull()) {
        throw new IdentifierGenerationException("sequence returned null");
      }
      return intValue;
    };

    IdentifierExtractor LONG_IDENTIFIER_EXTRACTOR = (ResultSet resultSet) -> {
      long longValue = resultSet.getLong(1);
      if (resultSet.wasNull()) {
        throw new IdentifierGenerationException("sequence returned null");
      }
      return longValue;
    };

    IdentifierExtractor BIG_INTEGER_IDENTIFIER_EXTRACTOR  = (ResultSet resultSet) -> {
      BigDecimal bigDecimal = resultSet.getBigDecimal(1);
      if (resultSet.wasNull()) {
        throw new IdentifierGenerationException("sequence returned null");
      }
      return bigDecimal.setScale(0, RoundingMode.UNNECESSARY).toBigInteger();
    };

    IdentifierExtractor BIG_DECIMAL_IDENTIFIER_EXTRACTOR  = (ResultSet resultSet) -> {
      BigDecimal bigDecimal = resultSet.getBigDecimal(1);
      if (resultSet.wasNull()) {
        throw new IdentifierGenerationException("sequence returned null");
      }
      return bigDecimal;
    };

    Serializable extractIdentifier(ResultSet resultSet) throws SQLException;

    static IdentifierExtractor getIdentifierExtractor(Class<?> integralType) {
      if ((integralType == Short.class) || (integralType == short.class)) {
        return SHORT_IDENTIFIER_EXTRACTOR;
      }
      if ((integralType == Integer.class) || (integralType == int.class)) {
        return INTEGER_IDENTIFIER_EXTRACTOR;
      }
      if ((integralType == Long.class) || (integralType == long.class)) {
        return LONG_IDENTIFIER_EXTRACTOR;
      }
      if (integralType == BigInteger.class) {
        return BIG_INTEGER_IDENTIFIER_EXTRACTOR;
      }
      if (integralType == BigDecimal.class) {
        return BIG_DECIMAL_IDENTIFIER_EXTRACTOR;
      }
      throw new IdentifierGenerationException("unsupported integral type: " + integralType);
    }

  }

  @Override
  public String toString() {
    // for debugging only
    return this.getClass().getSimpleName() + '(' + this.getSequenceName() + ')';
  }

}
