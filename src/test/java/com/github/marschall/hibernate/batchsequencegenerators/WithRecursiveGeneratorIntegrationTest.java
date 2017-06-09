package com.github.marschall.hibernate.batchsequencegenerators;

import static java.util.Collections.singletonMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

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

import com.github.marschall.hibernate.batchsequencegenerators.configurations.HibernateConfiguration;
import com.github.marschall.hibernate.batchsequencegenerators.configurations.PostgresConfiguration;
import com.github.marschall.hibernate.batchsequencegenerators.configurations.TransactionManagerConfiguration;

@RunWith(Parameterized.class)
public class WithRecursiveGeneratorIntegrationTest {

  // https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html#configuration-optional-properties
  // https://vladmihalcea.com/2014/07/08/hibernate-identity-sequence-and-table-sequence-generator/
  // hibernate.order_inserts
  // hibernate.order_updates
  // hibernate.jdbc.batch_size

  private final Class<?> databaseConfiguration;
  private final String persistenceUnitName;
  private AnnotationConfigApplicationContext applicationContext;
  private TransactionTemplate template;

  public WithRecursiveGeneratorIntegrationTest(Class<?> datasourceConfiguration, String persistenceUnitName) {
    this.databaseConfiguration = datasourceConfiguration;
    this.persistenceUnitName = persistenceUnitName;
  }

  @Parameters(name = "{1}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(
        new Object[]{PostgresConfiguration.class, "postgres-default"},
        new Object[]{PostgresConfiguration.class, "postgres-default"}
        );
  }

  @Before
  public void setUp() {
    this.applicationContext = new AnnotationConfigApplicationContext();
    this.applicationContext.register(this.databaseConfiguration, HibernateConfiguration.class, TransactionManagerConfiguration.class);
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    MutablePropertySources propertySources = environment.getPropertySources();
    Map<String, Object> source = singletonMap(HibernateConfiguration.PERSISTENCE_UNIT_NAME, this.persistenceUnitName);
    propertySources.addFirst(new MapPropertySource("persistence unit name", source));
    this.applicationContext.refresh();

    PlatformTransactionManager txManager = this.applicationContext.getBean(PlatformTransactionManager.class);
    this.template = new TransactionTemplate(txManager);

    this.template.execute(status -> {
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
    });
  }

  @After
  public void tearDown() {
    this.applicationContext.close();
  }

  @Test
  public void insert() {

//    EntityManagerFactory factory = this.applicationContext.getBean(EntityManagerFactory.class);
//    EntityManager entityManager = factory.createEntityManager();
//    try {
//      this.template.execute((s) -> {
//        int parentCount = 100;
//        List<ParentEntity> parents = new ArrayList<>(parentCount);
//        for (int i = 0; i < parentCount; i++) {
//          ParentEntity parent = new ParentEntity();
//          parent.addChild(new ChildEntity());
//          parent.addChild(new ChildEntity());
//        }
//        for (ParentEntity parent : parents) {
//          entityManager.persist(parent);
//        }
//        return null;
//      });
//    } finally {
//      entityManager.close();
//    }
  }

}
