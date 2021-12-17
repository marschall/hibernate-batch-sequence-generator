package com.github.marschall.hibernate.batchsequencegenerator;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.hibernate.batchsequencegenerator.configurations.FirebirdConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.H2Configuration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.HibernateConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.HsqlConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.MariaDbConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.OracleConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.PostgresConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.SqlServerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.entities.ChildEntity;
import com.github.marschall.hibernate.batchsequencegenerator.entities.ParentEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

public class BatchSequenceGeneratorIntegrationTest {

  private AnnotationConfigApplicationContext applicationContext;
  private EntityManagerFactory entityManagerFactory;

  public static List<Arguments> parameters() {
    List<Arguments> parameters = new ArrayList<>();

//    parameters.add(Arguments.of(Db2Configuration.class, "db2-default"));
//    parameters.add(Arguments.of(Db2Configuration.class, "db2-batched"));
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
    parameters.add(Arguments.of(OracleConfiguration.class, "oracle-default"));
    parameters.add(Arguments.of(OracleConfiguration.class, "oracle-batched"));
    return parameters;
  }

  private void setUp(Class<?> dataSourceConfiguration, String persistenceUnitName) {
    if (isTravis()) {
      assumeTrue(isSupportedOnTravis(persistenceUnitName));
    }
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(dataSourceConfiguration);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = Collections.singletonMap(HibernateConfiguration.PERSISTENCE_UNIT_NAME, persistenceUnitName);
    propertySources.addFirst(new MapPropertySource("persistence unit name", source));
    this.applicationContext.refresh();

    URL persistenceXml = BatchSequenceGeneratorIntegrationTest.class.getClassLoader().getResource("META-INF/persistence.xml");
    DataSource dataSource = this.applicationContext.getBean(DataSource.class);

    Map<String, Object> integrationSettings = new HashMap<>();
    integrationSettings.put(AvailableSettings.DATASOURCE, dataSource);
    this.entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(persistenceXml, persistenceUnitName, PersistenceUnitTransactionType.RESOURCE_LOCAL, integrationSettings)
            .withDataSource(dataSource)
            .build();

    this.populateDatabase(dataSource);
  }

  private void populateDatabase(DataSource dataSource) {
    //  PlatformTransactionManager txManager = this.applicationContext.getBean(PlatformTransactionManager.class);
    PlatformTransactionManager txManager = new DataSourceTransactionManager(dataSource);
    TransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    TransactionOperations template = new TransactionTemplate(txManager, transactionDefinition);
    template.execute(status -> {
      return this.populateDatabaseInTransaction();
    });
  }

  private Object populateDatabaseInTransaction() {
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


  private static boolean isTravis() {
    return System.getenv().getOrDefault("TRAVIS", "false").equals("true");
  }

  private static boolean isSupportedOnTravis(String persistenceUnitName) {
    // DB2, Firebird, SQL Server, MariaDB and Oracle are currently not supported on Travis CI
    return !(persistenceUnitName.contains("firebird")
            || persistenceUnitName.contains("sqlserver")
            || persistenceUnitName.contains("maria")
            || persistenceUnitName.contains("oracle")
            || persistenceUnitName.contains("db2"));
  }

  private void tearDown() {
    if (this.applicationContext == null) { // unsupported database on travis
      return;
    }
    this.entityManagerFactory.close();
    this.applicationContext.close();
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void parentChildInstert(Class<?> dataSourceConfiguration, String persistenceUnitName) {
    this.setUp(dataSourceConfiguration, persistenceUnitName);
    try {
      EntityManager entityManager = this.entityManagerFactory.createEntityManager();
      try {
        this.runInTransaction(entityManager, () -> this.verifyParentChildInstert(entityManager));
      } finally {
        entityManager.close();
      }
    } finally {
      this.tearDown();
    }
  }

  private void runInTransaction(EntityManager entityManager, Runnable callback) {
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();
    try {
      callback.run();
      if (transaction.getRollbackOnly()) {
        transaction.rollback();
      } else {
        transaction.commit();
      }
    } catch (Throwable t) {
      if (transaction.isActive()) {
        transaction.rollback();
      }
      throw t;
    }
  }

  private void verifyParentChildInstert(EntityManager entityManager) {
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
  }

}
