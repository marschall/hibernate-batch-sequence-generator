package com.github.marschall.hibernate.batchsequencegenerator.configurations;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;

@Configuration
public class SqlServerConfiguration {

  @Bean
  public DataSource dataSource() {
    if (!SQLServerDriver.isRegistered()) {
      try {
        SQLServerDriver.register();
      } catch (SQLException e) {
        throw new BeanCreationException("could not register driver", e);
      }
    }
    SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
    dataSource.setSuppressClose(true);
    // https://github.com/microsoft/mssql-jdbc/issues/1182
    dataSource.setUrl("jdbc:sqlserver://localhost:1433;databaseName=master;sendTimeAsDatetime=false;encrypt=false");
    dataSource.setUsername("sa");
    dataSource.setPassword("Cent-Quick-Space-Bath-8");
    return dataSource;
  }

  @Bean
  public DatabasePopulator databasePopulator() {
    return new ResourceDatabasePopulator(new ClassPathResource("mssql-schema.sql"));
  }

}
