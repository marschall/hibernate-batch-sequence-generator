package com.github.marschall.hibernate.batchsequencegenerator.configurations;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
public class HibernateConfiguration {

  public static final String PERSISTENCE_UNIT_NAME =  "persistence.unit.name";

  @Autowired
  private Environment environment;

  @Autowired
  private DataSource dataSource;


  @Bean
  public LocalContainerEntityManagerFactoryBean entityManager() {
    LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
    bean.setPersistenceUnitName(this.environment.getProperty(PERSISTENCE_UNIT_NAME));
    bean.setJpaDialect(this.jpaDialect());
    bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
//    Map<String, Object> jpaProperties = Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY, new CamelCaseToUnderscoresNamingStrategy());
//    bean.setJpaPropertyMap(jpaProperties);
    bean.setDataSource(this.dataSource);
    return bean;
  }

  @Bean
  public JpaDialect jpaDialect() {
    return new HibernateJpaDialect();
  }

}
