-- ---------------------------------------------------------------------------
-- DESTRUCTIVE: deletes the whole campus_complaint_db database.
--
-- Only for development, when you want a clean demo again:
--   mysql -u root -p < src/main/resources/reset-database.sql
--   mysql -u root -p < src/main/resources/schema.sql
--   mysql -u root -p < src/main/resources/sample-data.sql
--
-- The normal setup (schema.sql + sample-data.sql) never drops anything.
-- ---------------------------------------------------------------------------

DROP DATABASE IF EXISTS campus_complaint_db;
