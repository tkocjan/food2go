-- Create table
create table CHANGELOG
(
  CHANGE_NUMBER INTEGER not null,
  DELTA_SET     VARCHAR2(10),
  START_DT      TIMESTAMP(6),
  COMPLETE_DT   TIMESTAMP(6),
  APPLIED_BY    VARCHAR2(100),
  DESCRIPTION   VARCHAR2(500)
);