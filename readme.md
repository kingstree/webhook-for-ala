# Account Change Webhook Server

외부 시스템(APPLE / PARTNER / INTERNAL)으로부터 **계정 변경 Webhook**을 수신하여  
이벤트를 안전하게 저장(Inbox)하고, 비동기 처리 방식으로 계정 정보를 반영하는 서버입니다.

- Kotlin + Spring Boot 4.x
- SQLite + Flyway
- MyBatis
- Inbox / Idempotency 기반 이벤트 처리
- 단위 / 슬라이스 / 통합 테스트 구성

---

## 1. 실행 방법

### 1-1. 실행 환경
- Java 17 이상
- Gradle 8+
- OS: macOS / Linux / Windows

### 1-2. 환경 변수 설정 (선택)
Webhook 서명 검증을 위한 Secret 값은 환경 변수로 설정할 수 있습니다.

```bash
export WEBHOOK_SECRET_APPLE=change-me-apple
export WEBHOOK_SECRET_PARTNER=change-me-partner
export WEBHOOK_SECRET_INTERNAL=change-me-internal
```

설정하지 않을 경우 기본값이 사용됩니다.

### 1-3. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/webhook-0.0.1-SNAPSHOT.jar
```

### 1-4. 데이터베이스 초기화
- SQLite 파일 기반
- Flyway가 애플리케이션 시작 시 자동 실행
- 마이그레이션 위치

```
src/main/resources/db/migration
```

---

## 2. API 명세 요약

### 2-1. Webhook 수신
**POST** `/webhooks/account-changes`

**Headers**
- `X-Source-System` : APPLE / PARTNER / INTERNAL
- `X-Event-Id` : 이벤트 고유 ID
- `X-Timestamp` : epoch seconds
- `X-Signature` : HMAC-SHA256 서명

**Body**
```json
{
  "eventType": "EMAIL_FORWARDING_CHANGED",
  "accountKey": "external-user-123",
  "email": "user@example.com"
}
```

- 서명 검증
- Timestamp replay 방지
- 동일 eventId 멱등 처리

---

### 2-2. 이벤트 처리 트리거
**POST** `/inbox/process`

RECEIVED 상태의 이벤트를 처리합니다.

---

### 2-3. 계정 상태 조회
**GET** `/accounts/{accountKey}`

---

### 2-4. 이벤트 처리 결과 조회
**GET** `/inbox/events/{eventId}`

---

## 3. 필수 테스트 내역

### 3-1. Webhook 수신 / 멱등 처리
- 서명 검증 성공/실패
- 동일 eventId 재전송 시 DB 1건 유지

```
src/test/kotlin/aladin/webhook/slice/webhooks/WebhookReceiveSliceTest.kt
```

---

### 3-2. 이벤트 처리 결과
- EMAIL_FORWARDING_CHANGED 처리
- ACCOUNT_DELETED / APPLE_ACCOUNT_DELETED 처리
- 실패 시 FAILED + error_message 저장

```
src/test/kotlin/aladin/webhook/unit/application/InboxProcessorServiceUnitTest.kt
```

---

### 3-3. Idempotency 매핑 검증
- RECEIVED / PROCESSING / DONE / FAILED / UNKNOWN

```
src/test/kotlin/aladin/webhook/unit/application/WebhookIngestServiceUnitTest.kt
```

---

## 4. 기타 테스트 코드 정리

### 4-1. 서비스(Unit) 테스트
```
src/test/kotlin/aladin/webhook/unit/application/
```
- WebhookIngestService
- InboxProcessorService
- AccountQueryService
- InboxQueryService

### 4-2. 보안(Unit) 테스트
```
src/test/kotlin/aladin/webhook/unit/security/WebhookSignatureVerifierUnitTest.kt
```

### 4-3. 필터(Unit) 테스트
```
src/test/kotlin/aladin/webhook/unit/presentation/filter/CachedBodyFilterUnitTest.kt
```

### 4-4. DTO / Validation 테스트
```
src/test/kotlin/aladin/webhook/unit/domain/WebhookRequestDtoUnitTest.kt
```

---

## 테스트 요약
- 단위 / 슬라이스 / 통합 테스트 구성
- 멱등성, 보안, 이벤트 처리 신뢰성 검증
- 테스트 커버리지 80% 이상
