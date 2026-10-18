-- DEMO ONLY: admin@gmail.com / 123123. Change before public deployment.
-- Select the intended database in phpMyAdmin before importing this file.
-- Only update an existing admin. Never promote an employee/customer.
START TRANSACTION;
UPDATE users
SET password = '$2a$10$.VIihhdGDaZOJY9xzjwzVeepgVxwx2EkZaoMrCVoWtBIbltUJouWa',
    actived = b'1', deleted = b'0', activation_status = b'1',
    account_non_locked = b'1', failed_attempts = 0, lock_time = NULL,
    remember_key = NULL, is_google_user = b'0'
WHERE email = 'admin@gmail.com' AND role = 'ROLE_ADMIN';
COMMIT;

SELECT email, role, actived, account_non_locked
FROM users WHERE email = 'admin@gmail.com' AND role = 'ROLE_ADMIN';
