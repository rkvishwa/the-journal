-- Remove dual admin/creator roles so each account has a single role.
-- Run:
--   ./sql/migrate.sh sql/005_exclusive_roles.sql
DELETE FROM user_roles
WHERE role = 'MEMBER'
  AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'ADMIN');

DELETE FROM user_roles
WHERE role = 'ADMIN'
  AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'MEMBER');
