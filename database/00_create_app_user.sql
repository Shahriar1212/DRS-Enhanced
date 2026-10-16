-- =====================================================================
-- DRS-Enhanced: one-off MySQL account set-up (run ONCE as root)
-- In MySQL Workbench: open this file, then Query > Execute All (lightning icon).
-- Creates the least-privilege account the server uses (see drs.properties).
-- drs_test is used only by the automated integration tests.
-- =====================================================================
CREATE USER IF NOT EXISTS 'drs_user'@'localhost' IDENTIFIED BY 'Drs@2026';
GRANT ALL PRIVILEGES ON drs_enhanced.* TO 'drs_user'@'localhost';
GRANT ALL PRIVILEGES ON drs_test.* TO 'drs_user'@'localhost';
FLUSH PRIVILEGES;
SELECT user, host FROM mysql.user WHERE user = 'drs_user';
