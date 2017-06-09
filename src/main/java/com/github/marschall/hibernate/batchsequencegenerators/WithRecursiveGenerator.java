package com.github.marschall.hibernate.batchsequencegenerators;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A sequence generator that uses a recursive common table expression
 * to fetch multiple values from a sequence.
 *
 * <h2>SQL</h2>
 * The generated SELECT will look something like this
 * <pre></code>
 * </code></pre>
 */
public class WithRecursiveGenerator implements BulkInsertionCapableIdentifierGenerator, PersistentIdentifierGenerator, Configurable {

  // org.hibernate.id.enhanced.SequenceStyleGenerator

//  WITH /* RECURSIVE */ t(n, level_num) AS (
//          SELECT nextval(seq_xxx) as n, 1 as level_num
//          FROM dual
//        UNION ALL
//          SELECT nextval(seq_xxx) as n, level_num + 1 as level_num
//          FROM t
//          WHERE level_num < ?
//      )
//      SELECT n FROM t;

  /**
   * Indicates the increment size to use.  The default value is {@link #DEFAULT_FETCH_SIZE}
   */
  public static final String FETCH_SIZE_PARAM = "fetch_size";

  /**
   * The default value for {@link #FETCH_SIZE_PARAM}
   */
  public static final int DEFAULT_FETCH_SIZE = 10;

  /**
   * The sequence parameter
   */
  public static final String SEQUENCE = "sequence";

  private String sequenceName;
  private String select;
  private int fetchSize;
  private IdentifierPool identifierPool;
  private IdentifierExtractor identifierExtractor;

  private DatabaseStructure databaseStructure;

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
          throws MappingException {
    JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
    Dialect dialect = jdbcEnvironment.getDialect();
    this.sequenceName = params.getProperty(SEQUENCE);
    if (this.sequenceName == null) {
      throw new MappingException("no squence name specified");
    }
    this.select = this.buildSelect(this.sequenceName, dialect);
    this.identifierExtractor = IdentifierExtractor.getIdentifierExtractor(type.getReturnedClass());
    this.fetchSize = determineFetchSize(params);
    if (this.fetchSize <= 0) {
      throw new MappingException("fetch size must be positive");
    }
    this.identifierPool = IdentifierPool.empty();

    this.databaseStructure = buildDatabaseStructure(type, this.sequenceName, jdbcEnvironment);
  }

  private String buildSelect(String sequenceName, Dialect dialect) {
    return "WITH t(n, level_num) AS ("
            + "SELECT " + dialect.getSequenceNextValString(sequenceName) + " as n, 1 as level_num "
            + "UNION ALL "
            + "SELECT " + dialect.getSequenceNextValString(sequenceName) + " as n, level_num + 1 as level_num "
            + " FROM t "
            + " WHERE level_num < ?) "
            + "SELECT n FROM t";
  }

  private SequenceStructure buildDatabaseStructure(Type type, String sequenceName, JdbcEnvironment jdbcEnvironment) {
    return new SequenceStructure(jdbcEnvironment,
            QualifiedNameParser.INSTANCE.parse(sequenceName), 1, 1, type.getReturnedClass());
  }

  private int determineFetchSize(Properties params) {
    return ConfigurationHelper.getInt(FETCH_SIZE_PARAM, params, DEFAULT_FETCH_SIZE);
  }

  @Override
  public boolean supportsBulkInsertionIdentifierGeneration() {
    return true;
  }

  @Override
  public String determineBulkInsertionIdentifierGenerationSelectFragment(Dialect dialect) {
    return dialect.getSequenceNextValString(this.sequenceName);
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
    if (this.identifierPool.isEmpty()) {
      this.identifierPool = this.replenishIdentifierPool(session);
    }
    return this.identifierPool.next();
  }

  @Override
  public Object generatorKey() {
    return this.sequenceName;
  }

  @Override
  @Deprecated
  public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
    return dialect.getCreateSequenceStrings(this.sequenceName, 1, 1);
  }

  @Override
  @Deprecated
  public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
    return dialect.getDropSequenceStrings(this.sequenceName);
  }

  @Override
  public void registerExportables(Database database) {
    this.databaseStructure.registerExportables(database);
  }

  private IdentifierPool replenishIdentifierPool(SharedSessionContractImplementor session) throws HibernateException {
    JdbcCoordinator coordinator = session.getJdbcCoordinator();
    List<Serializable> identifiers = new ArrayList<>(this.fetchSize);
    try (PreparedStatement statement = coordinator.getStatementPreparer().prepareStatement(this.select)) {
      statement.setFetchSize(this.fetchSize);
      statement.setInt(1, this.fetchSize);
      try (ResultSet resultSet = coordinator.getResultSetReturn().extract(statement)) {
        while (resultSet.next()) {
          identifiers.add(this.identifierExtractor.extractIdentifier(resultSet));
        }
      }
    } catch (SQLException e) {
      throw session.getJdbcServices().getSqlExceptionHelper().convert(e, "could not get next sequence value", this.select);
    }
    if (identifiers.size() != this.fetchSize) {
      throw new IdentifierGenerationException("expected " + this.fetchSize + " values from " + this.sequenceName
              + " but got " + identifiers.size());
    }
    return IdentifierPool.forList(identifiers);
  }

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
      return this.iterator.hasNext();
    }

    Serializable next() {
      return this.iterator.next();
    }

  }

  enum IdentifierExtractor {
    // org.hibernate.id.IntegralDataTypeHolder

    INTEGER_IDENTIFIER_EXTRACTOR {
      @Override
      Serializable extractIdentifier(ResultSet resultSet) throws SQLException {
        int intValue = resultSet.getInt(1);
        if (resultSet.wasNull()) {
          throw new IdentifierGenerationException("sequence returned null");
        }
        return intValue;
      }
    },

    LONG_IDENTIFIER_EXTRACTOR {
      @Override
      Serializable extractIdentifier(ResultSet resultSet) throws SQLException {
        long longValue = resultSet.getLong(1);
        if (resultSet.wasNull()) {
          throw new IdentifierGenerationException("sequence returned null");
        }
        return longValue;
      }
    },

    BIG_INTEGER_IDENTIFIER_EXTRACTOR {
      @Override
      Serializable extractIdentifier(ResultSet resultSet) throws SQLException {
        BigDecimal bigDecimal = resultSet.getBigDecimal(1);
        if (resultSet.wasNull()) {
          throw new IdentifierGenerationException("sequence returned null");
        }
        return bigDecimal.setScale(0, BigDecimal.ROUND_UNNECESSARY).toBigInteger();
      }
    },

    BIG_DECIMAL_IDENTIFIER_EXTRACTOR {
      @Override
      Serializable extractIdentifier(ResultSet resultSet) throws SQLException {
        BigDecimal bigDecimal = resultSet.getBigDecimal(1);
        if (resultSet.wasNull()) {
          throw new IdentifierGenerationException("sequence returned null");
        }
        return bigDecimal;
      }
    };

    abstract Serializable extractIdentifier(ResultSet resultSet) throws SQLException;

    static IdentifierExtractor getIdentifierExtractor(Class<?> integralType) {
      if (integralType == Integer.class || integralType == int.class) {
        return INTEGER_IDENTIFIER_EXTRACTOR;
      }
      if (integralType == Long.class || integralType == long.class) {
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

}
