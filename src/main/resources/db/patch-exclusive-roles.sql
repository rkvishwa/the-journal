-- Ensure admin and creator roles are mutually exclusive.
DELETE FROM user_roles
WHERE role = 'MEMBER'
  AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'ADMIN');

DELETE FROM user_roles
WHERE role = 'ADMIN'
  AND user_id IN (SELECT user_id FROM user_roles WHERE role = 'MEMBER');
