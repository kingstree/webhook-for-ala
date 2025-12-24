
-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
                                     user_id            INTEGER PRIMARY KEY AUTOINCREMENT, -- 1부터 증가
                                     external_user_key  TEXT NOT NULL,                      -- accountKey (변경 가능 식별자)
                                     email              TEXT,
                                     status             TEXT NOT NULL DEFAULT 'ACTIVE',     -- ACTIVE / DELETED / APPLE_DELETED ...
                                     created_at         TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
    updated_at         TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),

    CONSTRAINT uq_users_external_user_key UNIQUE (external_user_key)
    );

-- users.updated_at 자동 갱신
CREATE TRIGGER IF NOT EXISTS trg_users_updated_at
AFTER UPDATE ON users
                            FOR EACH ROW
BEGIN
UPDATE users
SET updated_at = CURRENT_TIMESTAMP
WHERE user_id = OLD.user_id;
END;


-- =========================
-- INBOX EVENTS
-- =========================
CREATE TABLE IF NOT EXISTS inbox_events (
                                            inbox_event_id     INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Idempotency
                                            event_id           TEXT NOT NULL,                       -- X-Event-Id

    -- 발신 시스템 식별 (서명 검증 기반 신뢰)
                                            source_system      TEXT NOT NULL,                       -- APPLE | PARTNER | INTERNAL

    -- 이벤트 정보
                                            event_type         TEXT NOT NULL,                       -- EMAIL_FORWARDING_CHANGED / ACCOUNT_DELETED / ...
                                            account_key        TEXT NOT NULL,                       -- users.external_user_key (논리적 FK)

    -- 보안 메타
                                            signature          TEXT NOT NULL,                       -- X-Signature
                                            request_timestamp  INTEGER NOT NULL,                    -- X-Timestamp (epoch seconds)

    -- Payload
                                            payload_json       TEXT NOT NULL,

    -- 상태 관리
                                            status             TEXT NOT NULL DEFAULT 'RECEIVED',     -- RECEIVED / PROCESSING / DONE / FAILED
                                            error_message      TEXT,

    -- 시간
                                            received_at        TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
    processed_at       TEXT,

    CONSTRAINT uq_inbox_events_event_id UNIQUE (event_id)
    );

-- 조회/처리 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_inbox_events_account_key
    ON inbox_events(account_key);

CREATE INDEX IF NOT EXISTS idx_inbox_events_status
    ON inbox_events(status);

CREATE INDEX IF NOT EXISTS idx_inbox_events_source_system
    ON inbox_events(source_system);

CREATE INDEX IF NOT EXISTS idx_inbox_events_received_at
    ON inbox_events(received_at);


-- =========================
-- INBOX EVENT ATTEMPTS
-- =========================
CREATE TABLE IF NOT EXISTS inbox_event_attempts (
                                                    attempt_id         INTEGER PRIMARY KEY AUTOINCREMENT,

                                                    event_id           TEXT NOT NULL,                       -- inbox_events.event_id
                                                    attempt_no         INTEGER NOT NULL,                    -- 1,2,3...
                                                    attempt_status     TEXT NOT NULL,                       -- STARTED / SUCCEEDED / FAILED

                                                    started_at         TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP),
    ended_at           TEXT,
    error_message      TEXT,

    CONSTRAINT fk_attempts_event_id
    FOREIGN KEY (event_id)
    REFERENCES inbox_events(event_id)
    ON DELETE CASCADE,

    CONSTRAINT uq_attempts_event_attempt
    UNIQUE (event_id, attempt_no)
    );

CREATE INDEX IF NOT EXISTS idx_attempts_event_id
    ON inbox_event_attempts(event_id);

CREATE INDEX IF NOT EXISTS idx_attempts_status
    ON inbox_event_attempts(attempt_status);

CREATE INDEX IF NOT EXISTS idx_attempts_started_at
    ON inbox_event_attempts(started_at);
