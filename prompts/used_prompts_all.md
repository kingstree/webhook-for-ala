이번에 만들 과제의 내용이야 내용을 정리해줘

----

필요한 기술 스택들을 정리해보자 

  

• Spring Boot 3.x + Kotlin

• DB: SQLite

• DB 접근: JdbcTemplate + Flyway (JPA는 SQLite에서 삽질 가능성이 커서 비추)

• 테스트: spring-boot-starter-test + MockMvc (+ SQLite 임시파일로 통합테스트)



내가 생각한 스택은 위와 같은데 추가로 넣어야 할게 있을까?

----

내가 정리한 스택 + 너가 추천한 스택들을 정리해서 알려줘

----

DB 스키마 초안을 만들어 다만 조건이 있어 

1 .사용자 스키마에 pk는 1부터 오름차순으로 생성될수 있도록 해 사용자가 생성한 id는 변경이 가능한 식별자로서 사용할거야

2. 모든 스키마엔 생성시간을 넣을수 있도록 하고 사용자 스키마에만 수정시간을 추가해줘

----

Webhook 수신시 외부시스템의 요청인지 내부인지 어떻게 알수 있을까?

----

과제에서 mTLS 또는 IP allowlist 를 통해 어떤 곳에서 webhook을 요청했는지 변별을 하는게 좋을까? 아니면 별도의 방법이 좋을까?

----

외부 시스템들(예: Apple/파트너/내부 시스템)에서 전달되는 계정 변경 Webhook 이벤트를 수신

이 항목때문에 외부 어떤 시스템에서의 삭제인지를 저장해야 해서 그래


----

시스템별 secret 분리 + source_system을 서명 대상에 포함

을 통해 어느 시스템에서의 요청인지 판별하겠어

----

너의 제안들을 전부 받겠어 설계에 반영해줘 

  

다만 db 설계에서 이벤트 처리 시도 스키마까지 있으면 좋겠어


----

DDL은 아직 확정 안했어 

요청사항으론 

- inbox_events에서 received_at이 있으니 created_at은 필요 없으니 제거 

- inbox_event_attempts에서 started_at가 있으니 created_at 제거 

내 요청사항이 합당하니?

----

좋아 우선 erd 만들어줘

----

erd 보니 이거로 확정할게 

ddl 만들어줘

----

USERS에 들어갈 테스트 데이터 dml로 만들어줘

----

테스트 데이터를 내부 계정 5개, 파트너 5개, 애플 5개 만들어줘

----


inbox_events / inbox_event_attempts 테스트 DML 만들어줘

----

inbox_events / inbox_event_attempts 테스트 DML 만들어줘은 진행 안한거로 생각하고 잊어줘

----

api 별 이벤트 스토밍을 작성해줘

----

좋아 시퀸스다이어그램도 뽑아줘

----

모든 설계 내용을 확인했고 궁금한게 있어 idempotency 응답 규격을 어떻게 처리하면 좋을까 ?

----

좋아 이렇게 가자 

이제 해야할 일로는 우리가 설계한것을 바탕으로 yml에 필요한 것들을 만들어봐


----

방금 진행한건 잊어주고 우선 필요한 의존성을 작성해줘

----

좋아 이제 알겠어 

그럼 application.yml 을 작성해줘

----

write-dates-as-timestamps가 오류가 나는데 문제이유를 설명하고 수정해줘

----

ConfigurationProperties 오류 확인해줘

----

a 안으로 처리했어

----

이게 지금까지 만든 프로젝트의 소스야 (파일 전달)

----

아냐아냐 파일만 만들어뒀지 각 파일별 소스는 구현이 안되어있어 

yml 및 의존성 그리고 WebhookSecurityProperties 이 파일만 소스구현이 되어있으니 이걸 바탕으로 만들어갈거야

----

mybatis를 쓸건데 내용을 바꾸자

----

mybaties는 xml로 간다, 그리고 다른 mapper.xml들도 작성해줘

----

오토와이어링할 수 없습니다. 'InboxEventMapper' 타입의 bean을 찾을 수 없습니다. 확인해줘

----

우리 작업한걸 정리해봐 그리고 남은 작업도

----

rawBody를 실제 원문 기준으로 서명 검증을 진행하자


----

eventType별 payload validation 강화 하자

----

processReceived는 구현되어 있지 않잖아?

----

빈이 생성 안된거 같은데 확인해줘(오류내역 전달)

----

의존성 문제였어 버전을 올리니 해결되었어

----


/webhooks/account-changes를 통해 들어올 요청 값을 정리해줘

----

RuntimeException을 확장한 전역 처리기 가 필요해 예시 하나 줄게 

예시 소스들을 줄게 우리 코틀린 프로젝트에 적용하자(파일 3개 전달)

----

response status is 500 /v3/api-docs

----

의존성 문제였고 해소 했어

----

이제 테스트 코드들을 만들면 될거 같은데 어떄?

----

test 파일들 진행된거 전달할게 확인하고 어디까지 진행되었는지, 앞으로 어떤것들을 만들면 좋을지 말해줘(파일 전달)

----

이제 유닛 테스트들을 만들자 

우선 어떤 파일들을 만들어야 하는지 목록화 해줘

----

Webhook 수신

URL : POST /webhooks/account-changes

TEST 항목 : 서명 검증 성공/실패 케이스

동일 eventId 재전송 시 중복 처리 방지(DB 유니크/로직)



처리 트리거

URL : POST /inbox/process

TEST 항목: EMAIL_FORWARDING_CHANGED 처리 후 계정 조회 값이 갱신됨

ACCOUNT_DELETED 처리 후 상태가 DELETED로 변경됨

실패 케이스에서 FAILED 기록 및 error_message 저장됨 검증 케이스 : 요청 sorce ["INTERNAL", "PARTNER", "APPLE"] 별로 동작 처리 

  

계정 상태 조회 :  

URL :GET /accounts/{accountKey}

TEST 항목: 계정 상태 조회

이벤트 처리 결과 조회

URL :GET /inbox/events/{eventId}

TEST 항목 : 이벤트 처리 결과 조회



이렇게 테스트 해야 할 것들을 정리했는데 이것들을 테스트 할수 있도록 테스트 패키지를 설계해줘

----

좋아 필수 시나리오를 짜줘

----

inboxProcessorService 성공 시나리오 테스트 추가해줘

----

4) 테스트 필수 시나리오(최소)

-서명 검증 성공/실패 케이스

-동일 eventId 재전송 시 중복 처리 방지(DB 유니크/로직)

-EMAIL_FORWARDING_CHANGED 처리 후 계정 조회 값이 갱신됨

-ACCOUNT_DELETED 처리 후 상태가 DELETED로 변경됨

-실패 케이스에서 FAILED 기록 및 error_message 저장됨



해당 케이스들이 우리 테스트 케이스들에 있니?

----

우선 서명 검증 Unit Test 1개 시작하자 어떤 파일을 만들면 돼?

----

WebhookSignatureVerifierUnitTest 만들어봐

----

오류들 찾아서 수정해줘

----

(오류 코드 및 파일 전달)

----

너가 알려준 1번 방법 적용했는데 해소가 안돼 

확인해줘

----

(오류 내용 전달)

----

(의존성 파일 전달)

----

패키지 경로가 잘못되어 있었어 

문제는 해결되었어 이제 진행할거 다시 정리해봐

----

CachedBodyFilterUnitTest 만들어봐

----

content 문제 해결해봐

----

좋아 아까 필수 테스트 건들 우리가 전부 했는지 정리해줘

----

ACCOUNT_DELETED 처리 후 상태가 DELETED로 변경 완료 안된걸 진행해보자 진행해줘

----

source가 INTERNAL이 아닌 파트너랑 APPLE 일 경우도 넣어줘

----
APPLE인 경우 APPLE_DELETE로 저장되어야 할텐데 그 처리가 되어 있어?

----

if (eventStatus == "DONE") {

assertEquals("DELETED", userStatus)

} else {

assertEquals("ACTIVE", userStatus)

}



여기에서 DELETED가 아닌 APPLE_ACCOUNT_DELETED가 되는 경우도 추가해줘

----

Webhook 수신

URL : POST /webhooks/account-changes

TEST 항목 : 서명 검증 성공/실패 케이스

동일 eventId 재전송 시 중복 처리 방지(DB 유니크/로직)



처리 트리거

URL : POST /inbox/process

TEST 항목: EMAIL_FORWARDING_CHANGED 처리 후 계정 조회 값이 갱신됨

ACCOUNT_DELETED 처리 후 상태가 DELETED로 변경됨

실패 케이스에서 FAILED 기록 및 error_message 저장됨 검증 케이스 : 요청 sorce ["INTERNAL", "PARTNER", "APPLE"] 별로 동작 처리 

  

계정 상태 조회 :  

URL :GET /accounts/{accountKey}

TEST 항목: 계정 상태 조회

이벤트 처리 결과 조회

URL :GET /inbox/events/{eventId}

TEST 항목 : 이벤트 처리 결과 조회



이렇게 테스트 해야 할 것들을 정리했는데 이것들을 테스트 할수 있도록 테스트 패키지를 설계해줘

----
좋아 필수 시나리오를 짜줘

----

**실제 테스트 코드 골격(ParameterizedTest 포함)**까지 바로 만들어서 패치 형태로 만들어줘

----

it 들 Spring MockMvc matcher로 검증해줘

----

Unit 테스트(서비스/시그니처검증/필터)로 내려가자

----

CachedBodyFilter 부터 가자

----

2) 만들어야 할 Unit 테스트 파일 목록



A. 서비스(Unit)

1. aladin/webhook/unit/application/WebhookIngestServiceUnitTest.kt

2. aladin/webhook/unit/application/InboxProcessorServiceUnitTest.kt

3. aladin/webhook/unit/application/AccountQueryServiceUnitTest.kt

4. aladin/webhook/unit/application/InboxQueryServiceUnitTest.kt



B. 시그니처 검증(Unit)

5. aladin/webhook/unit/security/WebhookSignatureVerifierUnitTest.kt



C. 필터(Unit)

6. aladin/webhook/unit/presentation/filter/CachedBodyFilterUnitTest.kt



⸻



3) 각 Unit 테스트가 검증해야 하는 “핵심 포인트”



WebhookIngestServiceUnitTest

• insertReceived() 성공 → 응답 idempotency=CREATED, status=RECEIVED

• insertReceived()에서 DuplicateKeyException → idempotency=DUPLICATE_*가 DB status에 맞게 나옴

• insert 후 selectByEventId()가 null이면 BusinessException 발생



InboxProcessorServiceUnitTest

• RECEIVED 1건 pick → status를 PROCESSING → DONE/FAILED로 변경

• 처리 실패 시 error_message 저장 + status FAILED



AccountQueryServiceUnitTest

• 특정 accountKey 조회 결과 반환(없으면 null/예외는 너희 서비스 정책대로)



InboxQueryServiceUnitTest  (네가 준 코드 기준)

• 이벤트 없으면 null

• 이벤트 있으면 attempts까지 조립해서 반환 (attempt 매퍼 호출 + 매핑 필드 검증)



WebhookSignatureVerifierUnitTest

• 정상 canonical + secret + signature → 통과

• signature 불일치 → UnauthorizedException("SIGNATURE_MISMATCH")

• timestamp skew 초과 → UnauthorizedException("TIMESTAMP_SKEW_EXCEEDED")

• sourceSystem 이상 → UnauthorizedException("INVALID_SOURCE_SYSTEM")

• (옵션) HEX/BASE64 둘 다 한 번씩



CachedBodyFilterUnitTest

• request body가 filter 이후에도 읽을 수 있게 cached attribute/Wrapper가 적용되는지

• chain에서 body를 읽었을 때 원문과 동일한지

• 빈 body/null body 케이스도 안전한지



이 내용 기억하지?

----

하나식 확인 하자 

WebhookIngestServiceUnitTest.kt 검증들 필요한게 다 들어있는지 확인해줘

----

ParameterizedTest로 확장해서 RECEIVED/PROCESSING/DONE/FAILED/UNKNOWN까지 한 번에 커버하도록 패치로 바로 만들어줘
 
----

InboxProcessorServiceUnitTest.kt 검증들 필요한게 다 있는지 확인해줘

----

(오류내용 전달)

----

(오류내용 전달)

----


(오류내용 전달)

----

WebhookSignatureVerifierUnitTest.kt에서 검증이 충분한지 확인해줘

---

더 탄탄하게 테스트 하자

----
