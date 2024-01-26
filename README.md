Hibernate Batch Sequence Generator [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.marschall/hibernate-batch-sequence-generator/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.marschall/hibernate-batch-sequence-generator) [![Javadocs](https://www.javadoc.io/badge/com.github.marschall/hibernate-batch-sequence-generator.svg)](https://www.javadoc.io/doc/com.github.marschall/hibernate-batch-sequence-generator)  [![Build Status](https://travis-ci.org/marschall/hibernate-batch-sequence-generator.svg?branch=master)](https://travis-ci.org/marschall/hibernate-batch-sequence-generator)
==================================

A batch sequence generator for Hibernate that uses [recursive queries](https://en.wikipedia.org/wiki/Hierarchical_and_recursive_queries_in_SQL) to preallocate multiple values in a single database access.

The code is also present in [Hibernate Types](https://github.com/vladmihalcea/hibernate-types) starting with version 2.13.1.

```xml
<dependency>
  <groupId>com.github.marschall</groupId>
  <artifactId>hibernate-batch-sequence-generator</artifactId>
  <version>2.1.0</version>
</dependency>
```

Versions 2.1.x support Hibernate 6.4.

Versions 2.0.x support Hibernate 6.x.

Versions 1.x support Hibernate 5.6.

This sequence generator combines the advantages of several existing sequence generators and avoids their disadvantages:

- [hi/lo](https://vladmihalcea.com/2014/06/23/the-hilo-algorithm/)
  - all database access has to be aware of it
  - there is no clear relationship between the current sequence value and the column value
- `pooled` and `pooledlo`
  - `INCREMENT BY` has to be set on the database sequence
  - direct use of the sequence can cause a lot of identifier waste
  - the pool size and the `INCREMENT BY` value need to match
- `IDENTITY`
  - does not support JDBC batch inserts
- `TABLE`
  - has bad write performance

The limitations of this sequence generator are:

- limited database dialect support (see below)
- if you're using hbm2ddl then the `CACHE` value on the sequence is not set

Usage
-----

You can use this sequence generator like this

```java
@Id
@GenericGenerator(
        name = "some_column_name_id_generator",
        strategy = "com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator",
        parameters = {
            @Parameter(name = "sequence", value = "SOME_SEQUENCE_NAME"),
            @Parameter(name = "fetch_size", value = "SOME_FETCH_SIZE_VALUE")
        })
@GeneratedValue(generator = "some_column_name_id_generator")
@Column(name = "SOME_COLUMN_NAME")
private Long someColumnName;
```

You need to configure the following things

<dl>
<dt>SOME_SEQUENCE_NAME</dt>
<dd>the SQL name of the sequence from which the values should be fetched</dd>
<dt>SOME_FETCH_SIZE_VALUE</dt>
<dd>integer, how many values should be fetched at once, this should be equal to the <code>CACHE</code> value of the sequence</dd>
<dt>SOME_COLUMN_NAME</dt>
<dd>the SQL name of the column for which the value should be generated</dd>
<dt>some_column_name_id_generator</dt>
<dd>unique if of the generator</dd>
</dl>


Database Support
----------------

The following RDBMS have been verified to work

- DB2
- Firebird
- H2
- HSQLDB
- MariaDB 10.3 with Hibernate 5.2.17 or later
- Oracle
- Postgres
- SQL Sever
- In theory any RDBMS that supports `WITH RECURSIVE` and sequences is supported.

Unfortunately these RDBMS are currently not supported

- MySQL due to the lack of sequence support

DDL
---

For the best possible performance the `CACHE` value of the database sequence should be set to the same value as the `"fetch_size"` parameter.

Hibernate Versions
------------------

The project has been developed and tested against Hibernate 5.6.

Dependencies
------------

The project has no dependencies other than Hibernate.

Further Reading
---------------

- https://vladmihalcea.com/hibernate-batch-sequence-generator/
- https://vladmihalcea.com/how-to-batch-insert-and-update-statements-with-hibernate/
- https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html#configuration-optional-properties
- https://vladmihalcea.com/hibernate-identity-sequence-and-table-sequence-generator/
- https://dzone.com/articles/how-batch-insert-and-update
