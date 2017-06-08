package com.github.marschall.hibernate.batchsequencegenerators;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
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
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

public class WithRecursiveGenerator implements BulkInsertionCapableIdentifierGenerator, Configurable {

  // org.hibernate.id.enhanced.SequenceStyleGenerator

//  WITH /* RECURSIVE */ t(n, level_num) AS (
//          SELECT seq_xxx.nextval as n, 1 as level_num
//          FROM dual
//        UNION ALL
//          SELECT seq_xxx.nextval as n, level_num + 1 as level_num
//          FROM t
//          WHERE level_num < 5
//      )
//      SELECT n FROM t;

  private static final Logger LOG = Logger.getLogger(MethodHandles.publicLookup().lookupClass());

  /**
   * Indicates the increment size to use.  The default value is {@link #DEFAULT_FETCH_SIZE}
   */
  public static final String FETCH_SIZE_PARAM = "increment_size";

  /**
   * The default value for {@link #FETCH_SIZE_PARAM}
   */
  public static final int DEFAULT_FETCH_SIZE = 10;

  /**
   * The sequence parameter
   */
  public static final String SEQUENCE = "sequence";

  private String sequence;
  private int fetchSize;
  private IdentifierPool identifierPool;
  private IdentifierExtractor identifierExtractor;

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
          throws MappingException {
    this.sequence = params.getProperty(SEQUENCE);
    if (this.sequence == null) {
      throw new MappingException("no squence name specified");
    }
    this.identifierExtractor = IdentifierExtractor.getIdentifierExtractor(type.getReturnedClass());
    this.fetchSize = determineFetchSize(params);
    if (this.fetchSize <= 0) {
      throw new MappingException("fetch size must be positive");
    }
    this.identifierPool = IdentifierPool.empty();
    Dialect dialect = serviceRegistry.getService(JdbcServices.class).getDialect();
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
    return dialect.getSequenceNextValString(this.sequence);
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
    if (this.identifierPool.isEmpty()) {
      this.identifierPool = this.replenishIdentifierPool(session);
    }
    return this.identifierPool.next();
  }

  private IdentifierPool replenishIdentifierPool(SharedSessionContractImplementor session) throws HibernateException {
    String sql = "";
    JdbcCoordinator coordinator = session.getJdbcCoordinator();
    List<Serializable> identifiers = new ArrayList<>(this.fetchSize);
    try (PreparedStatement statement = coordinator.getStatementPreparer().prepareStatement(sql)) {
      statement.setFetchSize(this.fetchSize);
      try (ResultSet resultSet = coordinator.getResultSetReturn().extract(statement)) {
        while (resultSet.next()) {
          identifiers.add(this.identifierExtractor.extractIdentifier(resultSet));
        }
      }
    } catch (SQLException e) {
      throw session.getJdbcServices().getSqlExceptionHelper().convert(e, "could not get next sequence value", sql);
    }
    if (identifiers.size() != this.fetchSize) {
      throw new IdentifierGenerationException("expected " + this.fetchSize + " values from " + this.sequence + " but got " + identifiers.size());
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
