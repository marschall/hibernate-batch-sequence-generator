package com.github.marschall.hibernate.batchsequencegenerators;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.junit.ClassRule;
import org.junit.Ignore;
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

import com.github.marschall.hibernate.batchsequencegenerators.H2Test.LocalTransactionManagerConfiguration;
import com.github.marschall.hibernate.batchsequencegenerators.configurations.H2Configuration;

@Transactional
@ContextConfiguration(classes = {H2Configuration.class, LocalTransactionManagerConfiguration.class})
@Ignore
@Sql("classpath:h2-schema.sql")
public class H2Test {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  private DataSource dataSource;


  private static final String T = "SELECT NEXT VALUE FOR SEQ_PARENT_ID";


  private static final String S = "WITH t(n) AS ("
 + "   SELECT 1 as n FROM dual"
 + "   "
 + "   UNION ALL"
 + "   "
 + "   SELECT n + 1 as n FROM t WHERE n < 10)"
 + " SELECT SEQ_PARENT_ID.nextval FROM t";

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
