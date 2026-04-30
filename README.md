# StudyCafe

Spring Boot 기반의 스터디카페 좌석 예약 관리 웹 애플리케이션입니다. 사용자는 회원가입 후 좌석을 예약하고 보유 시간을 충전하거나 예약을 취소할 수 있으며, 관리자는 전체 좌석 현황과 이용 로그를 확인하고 좌석을 강제 퇴실 처리할 수 있습니다.

## 주요 기능

- 회원가입 및 로그인
- Spring Security 기반 사용자/관리자 권한 분리
- 좌석 목록 조회 및 좌석 예약
- 이용 시간 충전 및 예약 시간 자동 연장
- 예약 취소 시 남은 시간 환불
- 예약 종료 시간이 지난 좌석 자동 퇴실 처리
- 마이페이지에서 회원 정보 및 이용 내역 조회
- 관리자 좌석 현황 대시보드
- 관리자 이용 로그 조회
- Swagger UI를 통한 API 문서 확인

## 기술 스택

- Java 21
- Spring Boot 4.0.1
- Spring MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- MySQL
- Gradle
- Lombok
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, AssertJ

## 프로젝트 구조

```text
src
├── main
│   ├── java/com/example/studycafe
│   │   ├── config        # 보안, Swagger, 초기 데이터 설정
│   │   ├── controller    # 화면 요청 처리 컨트롤러
│   │   ├── dto           # 요청/응답 DTO
│   │   ├── model         # JPA 엔티티
│   │   ├── repository    # Spring Data JPA Repository
│   │   └── service       # 좌석 예약/취소/충전 비즈니스 로직
│   └── resources
│       ├── templates     # Thymeleaf 화면
│       └── application.properties
└── test
    └── java/com/example/studycafe
        └── service       # 좌석 서비스 단위 테스트
```

## 화면 및 경로

| 경로 | 설명 | 권한 |
| --- | --- | --- |
| `/`, `/home` | 메인 화면 | 전체 |
| `/signup` | 회원가입 | 전체 |
| `/login` | 로그인 | 전체 |
| `/book` | 좌석 예약 화면 | 사용자, 관리자 |
| `/mypage` | 마이페이지 및 이용 내역 | 로그인 사용자 |
| `/member/admin` | 관리자 좌석 현황 | 관리자 |
| `/admin/logs` | 관리자 이용 로그 | 관리자 |
| `/swagger-ui/index.html` | Swagger UI | 로그인 사용자 |

## 실행 전 준비

MySQL에 데이터베이스를 생성합니다.

```sql
CREATE DATABASE studycafe_db;
```

`src/main/resources/application.properties`에서 로컬 MySQL 접속 정보를 본인 환경에 맞게 수정합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/studycafe_db?serverTimezone=Asia/Seoul
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
```

JPA 설정은 기본적으로 `spring.jpa.hibernate.ddl-auto=update`를 사용하므로, 애플리케이션 실행 시 필요한 테이블이 자동으로 갱신됩니다.

## 실행 방법

Windows:

```bash
gradlew.bat bootRun
```

macOS / Linux:

```bash
./gradlew bootRun
```

애플리케이션 실행 후 브라우저에서 아래 주소로 접속합니다.

```text
http://localhost:8080
```

## 초기 데이터

애플리케이션 시작 시 `DataInitializer`가 실행되어 기본 좌석 30개를 생성합니다. 또한 관리자 계정이 없을 경우 초기 관리자 계정을 생성하도록 구성되어 있습니다.

관리자 계정 정보는 `DataInitializer`에서 확인할 수 있으며, 실제 배포 또는 공개 저장소에서는 반드시 별도 환경 변수나 시크릿 관리 방식으로 분리하는 것을 권장합니다.

## 테스트

전체 테스트 실행:

```bash
gradlew.bat test
```

주요 테스트 대상:

- 좌석 예약 성공/실패
- 보유 시간 부족 시 예약 실패
- 이미 사용 중인 좌석 예약 실패
- 시간 충전
- 이용 중 좌석 시간 자동 연장
- 예약 취소 및 남은 시간 환불
- 관리자 강제 퇴실

## 핵심 비즈니스 규칙

- 사용자는 동시에 하나의 좌석만 예약할 수 있습니다.
- 좌석 예약 시 사용자의 보유 시간이 예약 시간만큼 차감됩니다.
- 이용 중 시간 충전을 하면 보유 시간에 쌓이지 않고 현재 좌석의 종료 시간이 연장됩니다.
- 예약 취소 또는 관리자 강제 퇴실 시 남은 시간이 사용자에게 환불됩니다.
- 예약 종료 시간이 지난 좌석은 스케줄러가 자동으로 비웁니다.
- 회원의 활동 내역은 `History` 엔티티에 저장됩니다.

## 문서

추가 포트폴리오 문서는 `docs/` 디렉터리에서 확인할 수 있습니다.

- `docs/studycafe-backend-portfolio.html`
- `docs/studycafe-backend-portfolio.pdf`

## 참고 사항

- `application.properties`에 실제 DB 비밀번호를 커밋하지 않도록 주의해야 합니다.
- 현재 프로젝트는 Thymeleaf 서버 렌더링 방식으로 화면을 제공합니다.
- 인증은 세션 기반 Spring Security 로그인 방식을 사용합니다.
