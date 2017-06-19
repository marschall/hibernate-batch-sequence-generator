Hibernate Batch Sequence Generator
==================================

- [hi/lo](https://vladmihalcea.com/2014/06/23/the-hilo-algorithm/), all access was to be aware of it
- pooled and pooledlo, require to set the increment value on the database sequence, direct use of the sequence causes a lot of waste

- IDENTITY does not support JDBC batching
- TABLE has bad write performance

cache value of the sequence configuring how many values are reallocated

uses [recursive queries](https://en.wikipedia.org/wiki/Hierarchical_and_recursive_queries_in_SQL) to preallocate multiple values in a single database access

Hibernate Versions
------------------

The project has been developed and tested against Hibernate 5.2.

Dependencies
------------

The project has no dependencies other than Hibernate.

Unfortunately due to the lack of sequences MySQL can not be supported.

hbm2ddl no cache support

https://vladmihalcea.com/2015/03/18/how-to-batch-insert-and-update-statements-with-hibernate/
https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html#configuration-optional-properties
https://vladmihalcea.com/2014/07/08/hibernate-identity-sequence-and-table-sequence-generator/
https://dzone.com/articles/how-batch-insert-and-update