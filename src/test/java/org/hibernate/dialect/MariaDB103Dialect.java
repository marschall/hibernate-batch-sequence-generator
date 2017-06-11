package org.hibernate.dialect;

public class MariaDB103Dialect extends MariaDB53Dialect {

  @Override
  public boolean supportsSequences() {
    return true;
  }

  @Override
  public String getSelectSequenceNextValString(String sequenceName) {
    return "next value for " + sequenceName;
  }

  @Override
  public String getSequenceNextValString(String sequenceName) {
    return "select " + getSelectSequenceNextValString( sequenceName );
  }

  @Override
  public String getCreateSequenceString(String sequenceName) {
    return "create sequence " + sequenceName;
  }

  @Override
  public String getDropSequenceString(String sequenceName) {
    return "drop sequence " + sequenceName;
  }

  @Override
  public boolean supportsPooledSequences() {
    return true;
  }

  @Override
  protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
    return getCreateSequenceString( sequenceName ) + " start " + initialValue + " increment " + incrementSize;
  }

  @Override
  public String getQuerySequencesString() {
    return "show full tables where Table_type = 'SEQUENCE'";
  }

}
