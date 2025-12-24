-- =========================
-- USERS 테스트 데이터
-- 내부 시스템 계정 (INTERNAL)
-- =========================
INSERT INTO users (
    external_user_key,
    email,
    status
) VALUES
      ('internal-1001', 'internal1001@test.local', 'ACTIVE'),
      ('internal-1002', 'internal1002@test.local', 'ACTIVE'),
      ('internal-1003', 'internal1003@test.local', 'ACTIVE'),
      ('internal-1004', 'internal1004@test.local', 'ACTIVE'),
      ('internal-1005', 'internal1005@test.local', 'ACTIVE');


-- =========================
-- USERS 테스트 데이터
-- 파트너 계정 (PARTNER)
-- =========================
INSERT INTO users (
    external_user_key,
    email,
    status
) VALUES
      ('partner-2001', 'partner2001@partner.com', 'ACTIVE'),
      ('partner-2002', 'partner2002@partner.com', 'ACTIVE'),
      ('partner-2003', 'partner2003@partner.com', 'ACTIVE'),
      ('partner-2004', 'partner2004@partner.com', 'ACTIVE'),
      ('partner-2005', 'partner2005@partner.com', 'ACTIVE');


-- =========================
-- USERS 테스트 데이터
-- 애플 계정 (APPLE)
-- =========================
INSERT INTO users (
    external_user_key,
    email,
    status
) VALUES
      ('apple-3001', 'apple3001@icloud.com', 'ACTIVE'),
      ('apple-3002', 'apple3002@icloud.com', 'ACTIVE'),
      ('apple-3003', 'apple3003@icloud.com', 'ACTIVE'),
      ('apple-3004', 'apple3004@icloud.com', 'ACTIVE'),
      ('apple-3005', 'apple3005@icloud.com', 'ACTIVE');
