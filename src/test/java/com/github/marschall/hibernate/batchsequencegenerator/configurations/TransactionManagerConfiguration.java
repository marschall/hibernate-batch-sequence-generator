package com.github.marschall.hibernate.batchsequencegenerator.configurations;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class TransactionManagerConfiguration {

  @Autowired
  private Environment environment;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JpaDialect jpaDialect;

  @Bean
  public PlatformTransactionManager txManager(EntityManagerFactory emf) {
    JpaTransactionManager transactionManager = new JpaTransactionManager(emf);
    transactionManager.setDataSource(this.dataSource);
    transactionManager.setJpaDialect(this.jpaDialect);
    transactionManager.setPersistenceUnitName(this.environment.getProperty(HibernateConfiguration.PERSISTENCE_UNIT_NAME));
    return transactionManager;
  }

}
