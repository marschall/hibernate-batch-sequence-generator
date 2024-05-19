package com.github.marschall.hibernate.batchsequencegenerator.configurations;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import oracle.jdbc.OracleConnection;

@Configuration
public class OracleConfiguration {

  @Bean
  public DataSource dataSource() {
    oracle.jdbc.OracleDriver.isDebug();
    SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
    dataSource.setSuppressClose(true);
    dataSource.setUrl("jdbc:oracle:thin:@localhost:1521/FREEPDB1");
    dataSource.setUsername("jdbc");
    dataSource.setPassword("Cent-Quick-Space-Bath-8");
    Properties connectionProperties = new Properties();
    connectionProperties.setProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_DISABLE_OUT_OF_BAND_BREAK, "true");
    dataSource.setConnectionProperties(connectionProperties);
    return dataSource;
  }

  @Bean
  public DatabasePopulator databasePopulator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource("oracle-schema.sql"));
    populator.setSeparator("!!");
    return populator;
  }

}
