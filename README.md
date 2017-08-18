Hibernate Batch Sequence Generator
==================================

A batch sequence generator for Hibernate that uses [recursive queries](https://en.wikipedia.org/wiki/Hierarchical_and_recursive_queries_in_SQL) to preallocate multiple values in a single database access.

This sequence generator combines the advantages of several existing sequence generators and avoids their disadvantages. .

- [hi/lo](https://vladmihalcea.com/2014/06/23/the-hilo-algorithm/), all database access has to be aware of it, there is no clear relationship from the sequence to the database value
- pooled and pooledlo, require to set the increment value on the database sequence, direct use of the sequence causes a lot of waste
- `IDENTITY` does not support JDBC batching
- `TABLE` has bad write performance

cache value of the sequence configuring how many values are reallocated

Limitations are:
- limited database support (see below)
- if you're using hbm2ddl then the `CACHE` value on the sequence is not set

Usage
-----

You can use this generator like this

```java
@Id
@GenericGenerator(name = "some_column_name_id_generator", strategy = "com.github.marschall.hibernate.batchsequencegenerator.BatchSequenceGenerator",
        parameters = {
            @Parameter(name = SEQUENCE_PARAM, value = "SOME_SEQUENCE_NAME"),
            @Parameter(name = FETCH_SIZE_PARAM, value = "FETCH_VALUE")
        })
@GeneratedValue(generator = "some_column_name_id_generator")
@Column(name = "SOME_COLUMN_NAME")
private Long someColumnName;
```

You need to configure the following things:

<dl>
<dt>SOME_SEQUENCE_NAME</dt>
<dd>the SQL name of the sequence from which the values should be fetched</dd>
<dt>FETCH_VALUE</dt>
<dd>integer, how many values should be fetched at once, this is be equals to the <code>CACHE</code> value fo the sequence</dd>
<dt>SOME_COLUMN_NAME</dt>
<dd>the SQL name of the column for which the value should be generated</dd>
<dt>some_column_name_id_generator</dt>
<dd>unique if of the generator</dd>
</dl>


Database Support
----------------

The following RDBMS have been verified to work

- Firebird
- H2
- HSQLDB
- Oracle
- Postgres
- SQL Sever
- In theory any RDBMS that supports `WITH RECURSIVE` and sequences is supported.

Unfortunately these RDBMS are currently not supported

- due to the lack of sequences MySQL can not be supported
- MariaDB 10.3 works in theory but needs a [pull request](https://github.com/hibernate/hibernate-orm/pull/1930)

DDL
---

For the best possible performance set the `CACHE` value of the databse sequence to the same value as how many values from the sequences should be fetched from the database.

Hibernate Versions
------------------

The project has been developed and tested against Hibernate 5.2.

Dependencies
------------

The project has no dependencies other than Hibernate.

Further Reading
---------------

- https://vladmihalcea.com/2015/03/18/how-to-batch-insert-and-update-statements-with-hibernate/
- https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html#configuration-optional-properties
- https://vladmihalcea.com/2014/07/08/hibernate-identity-sequence-and-table-sequence-generator/
- https://dzone.com/articles/how-batch-insert-and-update
