package com.github.marschall.hibernate.batchsequencegenerator.configurations;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class Db2Configuration {

  @Bean
  public DataSource dataSource() {
    try {
      Class.forName("com.ibm.db2.jcc.DB2Driver");
    } catch (ClassNotFoundException e) {
      throw new BeanCreationException("could not register driver", e);
    }
    SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
    dataSource.setSuppressClose(true);
    dataSource.setUrl("jdbc:db2://localhost:50000/jdbc");
    dataSource.setUsername("db2inst1");
    dataSource.setPassword("Cent-Quick-Space-Bath-8");
    return dataSource;
  }

  @Bean
  public DatabasePopulator databasePopulator() {
    return new ResourceDatabasePopulator(new ClassPathResource("db2-schema.sql"));
  }

}
