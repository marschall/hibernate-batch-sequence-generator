package com.github.marschall.hibernate.batchsequencegenerator;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.hibernate.batchsequencegenerator.configurations.FirebirdConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.H2Configuration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.HibernateConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.HsqlConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.MariaDbConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.PostgresConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.SqlServerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.TransactionManagerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.entities.ChildEntity;
import com.github.marschall.hibernate.batchsequencegenerator.entities.ParentEntity;

public class BatchSequenceGeneratorIntegrationTest {

  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;



  public static List<Arguments> parameters() {
    List<Arguments> parameters = new ArrayList<>();

    parameters.add(Arguments.of(MariaDbConfiguration.class, "maria-default"));
    parameters.add(Arguments.of(MariaDbConfiguration.class, "maria-batched"));
    parameters.add(Arguments.of(FirebirdConfiguration.class, "firebird-default"));
    parameters.add(Arguments.of(FirebirdConfiguration.class, "firebird-batched"));
    parameters.add(Arguments.of(HsqlConfiguration.class, "hsql-default"));
    parameters.add(Arguments.of(HsqlConfiguration.class, "hsql-batched"));
    parameters.add(Arguments.of(H2Configuration.class, "h2-default"));
    parameters.add(Arguments.of(H2Configuration.class, "h2-batched"));
    parameters.add(Arguments.of(SqlServerConfiguration.class, "sqlserver-default"));
    parameters.add(Arguments.of(SqlServerConfiguration.class, "sqlserver-batched"));
    parameters.add(Arguments.of(PostgresConfiguration.class, "postgres-default"));
    parameters.add(Arguments.of(PostgresConfiguration.class, "postgres-batched"));
    return parameters;
  }

  private void setUp(Class<?> dataSourceConfiguration, String persistenceUnitName) {
    if (isTravis()) {
      assumeTrue(isSupportedOnTravis(persistenceUnitName));
    }
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(dataSourceConfiguration, HibernateConfiguration.class, TransactionManagerConfiguration.class);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = singletonMap(HibernateConfiguration.PERSISTENCE_UNIT_NAME, persistenceUnitName);
    propertySources.addFirst(new MapPropertySource("persistence unit name", source));
    this.applicationContext.refresh();

    PlatformTransactionManager txManager = this.applicationContext.getBean(PlatformTransactionManager.class);
    TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.template = new TransactionTemplate(txManager, transactionDefinition);
    this.template.execute(status -> {
      return this.populateDatabase();
    });
  }



  private static boolean isTravis() {
    return System.getenv().getOrDefault("TRAVIS", "false").equals("true");
  }

  private static boolean isSupportedOnTravis(String persistenceUnitName) {
    // firebird and SQL server are currently not supported on travis
    return !(persistenceUnitName.contains("firebird")
            || persistenceUnitName.contains("sqlserver")
            || persistenceUnitName.contains("maria"));
  }

  private Object populateDatabase() {
    Map<String, DatabasePopulator> beans = this.applicationContext.getBeansOfType(DatabasePopulator.class);
    DataSource dataSource = this.applicationContext.getBean(DataSource.class);
    try (Connection connection = dataSource.getConnection()) {
      for (DatabasePopulator populator : beans.values()) {
        populator.populate(connection);
      }
    } catch (SQLException e) {
      throw new RuntimeException("could initialize database", e);
    }
    return null;
  }

  private void tearDown() {
    if (this.applicationContext == null) { // unsupported database on travis
      return;
    }
    this.applicationContext.close();
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void parentChildInstert(Class<?> dataSourceConfiguration, String persistenceUnitName) {
    this.setUp(dataSourceConfiguration, persistenceUnitName);
    EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
    try {
      this.template.execute(status -> {
        EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(factory);
        int parentCount = 100;
        List<ParentEntity> parents = new ArrayList<>(parentCount);
        for (int i = 0; i < parentCount; i++) {
          ParentEntity parent = new ParentEntity();

          parent.addChild(new ChildEntity());
          parent.addChild(new ChildEntity());

          parents.add(parent);
        }
        for (ParentEntity parent : parents) {
          entityManager.persist(parent);
          for (ChildEntity child : parent.getChildren()) {
            child.setParentId(parent.getParentId());
            entityManager.persist(child);
          }
        }
        status.flush();
        return null;
      });
    } finally {
      this.tearDown();
    }
  }

}
