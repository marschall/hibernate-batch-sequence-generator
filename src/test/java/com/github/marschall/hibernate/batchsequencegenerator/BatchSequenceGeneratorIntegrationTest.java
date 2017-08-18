package com.github.marschall.hibernate.batchsequencegenerator;

import static java.util.Collections.singletonMap;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.marschall.hibernate.batchsequencegenerator.configurations.FirebirdConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.H2Configuration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.HibernateConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.HsqlConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.PostgresConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.SqlServerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.TransactionManagerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.entities.ChildEntity;
import com.github.marschall.hibernate.batchsequencegenerator.entities.ParentEntity;

@RunWith(Parameterized.class)
public class BatchSequenceGeneratorIntegrationTest {

  private final Class<?> databaseConfiguration;
  private final String persistenceUnitName;
  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public BatchSequenceGeneratorIntegrationTest(Class<?> datasourceConfiguration, String persistenceUnitName) {
    this.databaseConfiguration = datasourceConfiguration;
    this.persistenceUnitName = persistenceUnitName;
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
//            new Object[]{MariaConfiguration.class, "maria-default"},
//            new Object[]{MariaConfiguration.class, "maria-batched"},
            new Object[]{FirebirdConfiguration.class, "firebird-default"},
            new Object[]{FirebirdConfiguration.class, "firebird-batched"},
            new Object[]{HsqlConfiguration.class, "hsql-default"},
            new Object[]{HsqlConfiguration.class, "hsql-batched"},
            new Object[]{H2Configuration.class, "h2-default"},
            new Object[]{H2Configuration.class, "h2-batched"},
            new Object[]{SqlServerConfiguration.class, "sqlserver-default"},
            new Object[]{SqlServerConfiguration.class, "sqlserver-batched"},
            new Object[]{PostgresConfiguration.class, "postgres-default"},
            new Object[]{PostgresConfiguration.class, "postgres-batched"});
  }

  @Before
  public void setUp() {
    if (isTravis()) {
      assumeTrue(isSupportedOnTravis(this.persistenceUnitName));
    }
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(this.databaseConfiguration, HibernateConfiguration.class, TransactionManagerConfiguration.class);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = singletonMap(HibernateConfiguration.PERSISTENCE_UNIT_NAME, this.persistenceUnitName);
    propertySources.addFirst(new MapPropertySource("persistence unit name", source));
    this.applicationContext.refresh();

    PlatformTransactionManager txManager = this.applicationContext.getBean(PlatformTransactionManager.class);
    this.template = new TransactionTemplate(txManager);
    this.template.setPropagationBehavior(0);

    this.template.execute(status -> {
      return populateDatabase();
    });
  }



  private static boolean isTravis() {
    return System.getenv().getOrDefault("TRAVIS", "false").equals("true");
  }

  private static boolean isSupportedOnTravis(String persistenceUnitName) {
    // firebird and SQL server are currently not supported on travis
    return !(persistenceUnitName.contains("firebird")
            || persistenceUnitName.contains("sqlserver"));
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

  @After
  public void tearDown() {
    this.applicationContext.close();
  }

  @Test
  public void parentChildInstert() {
    EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
    EntityManager entityManager = factory.createEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    try {
      //      this.template.execute((s) -> {
      transaction.begin();
      //        this.populateDatabase();
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
      //        entityManager.flush();
      //        s.flush();
      //        return null;
      //      });
      transaction.commit();
    } finally {
      entityManager.close();
    }
  }

}
