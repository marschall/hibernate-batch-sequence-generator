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
public class MariaDbConfiguration {

  @Bean
  public DataSource dataSource() {
    try {
      Class.forName("org.mariadb.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new BeanCreationException("could not register driver", e);
    }
    SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
    dataSource.setSuppressClose(true);
    // https://mariadb.com/kb/en/mariadb/about-mariadb-connector-j/
    dataSource.setUrl("jdbc:mariadb://localhost:3307/jdbc");
    dataSource.setUsername("jdbc");
    dataSource.setPassword("Cent-Quick-Space-Bath-8");
    return dataSource;
  }

  @Bean
  public DatabasePopulator databasePopulator() {
    return new ResourceDatabasePopulator(new ClassPathResource("mariadb-schema.sql"));
  }

}
