package com.github.marschall.hibernate.batchsequencegenerators.configurations;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class MariaConfiguration {

  @Bean
  public DataSource dataSource() {
    SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
    dataSource.setSuppressClose(true);
    String userName = "jdbc";
    String database = "jdbc";
    // https://mariadb.com/kb/en/mariadb/about-mariadb-connector-j/
    dataSource.setUrl("jdbc:mariadb://localhost:3306/" + database);
    dataSource.setUsername(userName);
    String password = this.isTravis() ? "" : "Cent-Quick-Space-Bath-8";
    dataSource.setPassword(password);
    return dataSource;
  }

  private boolean isTravis() {
    return System.getenv().getOrDefault("TRAVIS", "false").equals("true");
  }

  @Bean
  public DatabasePopulator databasePopulator() {
    return new ResourceDatabasePopulator(new ClassPathResource("maria-schema.sql"));
  }

}
