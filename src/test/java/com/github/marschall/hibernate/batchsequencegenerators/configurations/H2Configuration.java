package com.github.marschall.hibernate.batchsequencegenerators.configurations;

import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
public class H2Configuration {

  private static final AtomicInteger COUNT = new AtomicInteger();

  @Autowired
  private Environment environment;

  @Bean
  public DataSource dataSource() {
    String persistenceUnitName = environment.getProperty(HibernateConfiguration.PERSISTENCE_UNIT_NAME);
    DataSource dataSource = buildH2DataSource();
    if (persistenceUnitName.endsWith("-batched")) {
      return wrapWithLogging(dataSource, "h2-test");
    } else {
      return dataSource;
    }
  }

  static DataSource wrapWithLogging(DataSource dataSource, String name) {

    SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
    loggingListener.setQueryLogEntryCreator(new DefaultQueryLogEntryCreator());
    return ProxyDataSourceBuilder
            .create(dataSource)
            .name(name)
            .listener(loggingListener)
            .build();
  }

  private DataSource buildH2DataSource() {
    return new EmbeddedDatabaseBuilder()
         // the spring test context framework keeps application contexts
         // and thus databases around for the entire VM lifetime
         // so be have to create a unique name here to avoid sharing
         // between application contexts
        .setName("H2-" + COUNT.incrementAndGet())
        .setType(H2)
        .addScript("h2-schema.sql")
        .build();
  }

}
