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
- if you're using hbm2ddl then the `CACHE` value is not set

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


- Unfortunately due to the lack of sequences MySQL can not be supported.
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
