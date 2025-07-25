# FINZ API Swagger 문서화 가이드

## 🚀 Swagger 설정 완료!

Spring Legacy 환경에서 Swagger를 활용한 API 자동 문서화가 완료되었습니다.

## 📋 설정된 내용

### 1. Swagger 의존성 (build.gradle)

```gradle
// swagger
implementation 'io.springfox:springfox-swagger2:2.9.2'
implementation 'io.springfox:springfox-swagger-ui:2.9.2'
```

### 2. SwaggerConfig 설정

- API 정보 및 메타데이터 구성
- 컨트롤러 패키지 스캔 설정
- API 경로 필터링 (`/api/.*`)

### 3. Spring MVC 설정 (ServletConfig)

- Swagger UI 리소스 핸들러 추가
- WebJars 리소스 매핑

### 4. API 문서화 완료

- **StockController**: 주식 가격 조회 및 모의 매수 API
- **ChatBotController**: AI 챗봇 대화 API

## 🌐 Swagger UI 접근 방법

### 프로젝트 실행 후 다음 URL로 접근:

```
http://localhost:8080/swagger-ui.html
```

또는 단축 URL:

```
http://localhost:8080/api-docs
```

## 📖 API 문서 내용

### 1. 주식 모의투자 API (`/api/mock`)

#### GET `/api/mock/price/{code}`

- **기능**: 주식 가격 조회
- **파라미터**:
  - `code` (경로): 주식 종목코드 (예: 005930)
- **응답**: JSON 형태의 주가 정보

#### POST `/api/mock/buy`

- **기능**: 모의 매수 주문
- **요청 Body**:
  ```json
  {
    "stockCode": "005930",
    "quantity": 10,
    "price": 75000,
    "orderType": "LIMIT"
  }
  ```

### 2. AI 챗봇 API (`/api/chat`)

#### POST `/api/chat`

- **기능**: 챗봇 대화
- **요청 Body**:
  ```json
  {
    "message": "삼성전자 주식에 대해 알려주세요"
  }
  ```
- **응답**:
  ```json
  {
    "content": "AI 챗봇 응답 내용"
  }
  ```

## 🔧 Swagger 어노테이션 활용

### 컨트롤러 레벨

```java
@Api(tags = "API 그룹명", description = "API 설명")
```

### 메서드 레벨

```java
@ApiOperation(value = "API 제목", notes = "상세 설명")
@ApiResponses({
    @ApiResponse(code = 200, message = "성공"),
    @ApiResponse(code = 400, message = "실패")
})
```

### 파라미터

```java
@ApiParam(value = "파라미터 설명", required = true, example = "예시값")
```

### DTO 모델

```java
@ApiModel(description = "모델 설명")
@ApiModelProperty(value = "필드 설명", required = true, example = "예시값")
```

## 📝 테스트 방법

1. **프로젝트 실행**

   ```bash
   gradlew bootRun
   ```

2. **Swagger UI 접속**

   - http://localhost:8080/swagger-ui.html

3. **API 테스트**
   - 각 API의 "Try it out" 버튼 클릭
   - 파라미터 입력
   - "Execute" 버튼으로 실행

## 🎯 장점

- **자동 문서화**: 코드 변경 시 문서 자동 업데이트
- **대화형 테스트**: Swagger UI에서 직접 API 테스트 가능
- **명확한 스펙**: 요청/응답 형태를 명확하게 제시
- **팀 협업**: 프론트엔드 개발자와의 원활한 소통

## 🚨 주의사항

- 프로덕션 환경에서는 Swagger UI 접근을 제한하는 것을 권장
- API 문서가 최신 상태를 유지하도록 어노테이션 관리 필요
- 민감한 정보는 example 값에 포함하지 않도록 주의
