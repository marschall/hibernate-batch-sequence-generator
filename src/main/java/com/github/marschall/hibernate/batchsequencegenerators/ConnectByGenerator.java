package com.github.marschall.hibernate.batchsequencegenerators;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
//import org.hibernate.exception.spi.Configurable;
import org.hibernate.id.Configurable;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * A sequence generator that uses Oracle CONNECT BY syntax
 * to fetch multiple values from a sequence.
 * <p>
 * Oracle does not support using recursive common table expressions in
 * order to fetch multiple values from a sequence.
 */
public class ConnectByGenerator implements BulkInsertionCapableIdentifierGenerator, Configurable {

//  SELECT seq_xxx.nextval FROM dual CONNECT BY rownum <= 5;/

  /**
   * The sequence parameter
   */
  public static final String SEQUENCE = "sequence";

  private String sequence;

  @Override
  public void configure(Type type, Properties params, ServiceRegistry serviceRegistry)
          throws MappingException {
    this.sequence = params.getProperty(SEQUENCE);
    if (this.sequence == null) {
      throw new MappingException("no squence name specified");
    }
  }

  @Override
  public boolean supportsBulkInsertionIdentifierGeneration() {
    return true;
  }

  @Override
  public String determineBulkInsertionIdentifierGenerationSelectFragment(Dialect dialect) {
    return null;
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
    String sql = "";
    try (PreparedStatement st = session.getJdbcCoordinator().getStatementPreparer().prepareStatement(sql);) {

    } catch (SQLException e) {
      throw session.getJdbcServices().getSqlExceptionHelper().convert(e, "could not get next sequence value", sql);
    }
    return null;
  }

}
