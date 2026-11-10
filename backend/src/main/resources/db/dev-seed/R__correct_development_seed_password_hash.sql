-- Correct the documented development-only default password for seeded admin/teacher accounts.
-- Only accounts still carrying the original V099 hash are updated; manually changed passwords are preserved.
UPDATE users
SET password_hash = '$2a$10$lVqHihSi7//szMpQ9K/sIeLbjbEy2KlzjjMTwO24n8gf4BQoWcBMu'
WHERE school_id = 1
  AND username IN ('admin', 'teacher1', 'teacher2')
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
