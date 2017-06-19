package com.github.marschall.hibernate.batchsequencegenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.marschall.hibernate.batchsequencegenerator.SqlServerTest.LocalTransactionManagerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerator.configurations.SqlServerConfiguration;

@Transactional
@ContextConfiguration(classes = {SqlServerConfiguration.class, LocalTransactionManagerConfiguration.class})
//@Ignore
@Sql("classpath:mssql-schema.sql")
public class SqlServerTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private DataSource dataSource;


  private static final String T = "SELECT NEXT VALUE FOR SEQ_PARENT_ID";


  private static final String S = "WITH t(n) AS ("
          + "SELECT 1 as n "
          + "UNION ALL "
          +"SELECT n + 1 as n FROM t WHERE n < 10) "
          + "SELECT next value for SEQ_PARENT_ID as n FROM t";

  @Test
  public void singleRowSelect() throws SQLException {
    try (Connection connection = this.dataSource.getConnection();
         Statement statement = connection.createStatement()) {
      try (ResultSet resultSet = statement.executeQuery(S)) {
        while (resultSet.next()) {
          System.out.println(resultSet.getLong(1));
        }
      }
    }
  }


  @Configuration
  static class LocalTransactionManagerConfiguration {

    @Autowired
    private DataSource dataSource;

    @Bean
    public PlatformTransactionManager txManager() {
      return new DataSourceTransactionManager(dataSource);
    }

  }

}
