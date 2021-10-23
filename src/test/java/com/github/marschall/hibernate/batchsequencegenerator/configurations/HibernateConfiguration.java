package com.github.marschall.hibernate.batchsequencegenerator.configurations;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class HibernateConfiguration {

  public static final String PERSISTENCE_UNIT_NAME =  "persistence.unit.name";

  @Autowired
  private Environment environment;

  @Autowired
  private DataSource dataSource;


//  @Bean
//  public LocalContainerEntityManagerFactoryBean entityManager() {
//    LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
//    bean.setPersistenceUnitName(environment.getProperty(PERSISTENCE_UNIT_NAME));
//    bean.setJpaDialect(jpaDialect());
//    bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
//    bean.setDataSource(dataSource);
//    return bean;
//  }
//
//  @Bean
//  public JpaDialect jpaDialect() {
//    return new HibernateJpaDialect();
//  }

}
