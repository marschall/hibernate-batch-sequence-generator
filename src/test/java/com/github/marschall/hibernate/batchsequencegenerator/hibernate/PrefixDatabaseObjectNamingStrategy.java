package com.github.marschall.hibernate.batchsequencegenerator.hibernate;

import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.service.ServiceRegistry;

/**
 * Uses <code>seq_${table_name}</code> for primary sequence names.
 */
public final class PrefixDatabaseObjectNamingStrategy implements ImplicitDatabaseObjectNamingStrategy {

  @Override
  public QualifiedName determineSequenceName(Identifier catalogName, Identifier schemaName, Map<?, ?> configValues,
      ServiceRegistry serviceRegistry) {
    Object targetColumn = configValues.get(PersistentIdentifierGenerator.PK);
    return new QualifiedSequenceName(catalogName, schemaName, Identifier.toIdentifier("seq_" + targetColumn));
  }

  @Override
  public QualifiedName determineTableName(Identifier catalogName, Identifier schemaName, Map<?, ?> configValues,
      ServiceRegistry serviceRegistry) {
    throw new UnsupportedOperationException("determineTableName");
  }

}
