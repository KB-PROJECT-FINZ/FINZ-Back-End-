# 🧠 FINZIE Backend

AI와 금융을 결합한 MZ세대 대상 투자 학습 및 모의투자 플랫폼 **FINZIE**의 백엔드 저장소입니다.  
GPT 기반의 챗봇, 실시간 주식 시세 연동 모의투자, 성향 맞춤형 콘텐츠 추천, 랭킹 시스템 등을 제공합니다.

---

## 📁 프로젝트 구조

finzie-backend/
│
├── config/ # Spring 설정
├── controller/ # REST API 컨트롤러
├── domain/ # DTO, VO 등 도메인 객체
├── service/ # 비즈니스 로직
├── repository/ # MyBatis Mapper 인터페이스
├── scheduler/ # 자동 피드백 생성 스케줄러
├── utils/ # 공용 유틸리티
└── resources/
├── mapper/ # MyBatis XML
└── application.yml # 환경 설정

yaml
복사
편집

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 8+ |
| Framework | Spring Framework (4.x) |
| ORM | MyBatis |
| 인증 | Spring Security (세션, JWT) |
| AI 연동 | OpenAI GPT API |
| 주식 시세 | 한국투자증권 Open API |
| DB | MySQL |
| 배포 | AWS EC2, Docker |
| CI/CD | GitHub Actions, SSH Deploy |

---

## 🔑 주요 기능

### 1. 사용자 및 인증
- 카카오/구글/네이버 소셜 로그인
- JWT 기반 인증 (Vue 연동 시)

### 2. 투자 성향 테스트
- 2지선다형 질문 기반 유형 분류
- 총 16종 유형 + 재진단 가능
- 유형 기반 콘텐츠 및 종목 추천 연계

### 3. 개념 학습
- 성향에 맞는 콘텐츠 추천 (텍스트/영상/카드)
- 콘텐츠 열람 + 퀴즈 응시 + 크레딧 지급
- 학습 이력, 크레딧 누적 관리

### 4. 모의투자 시스템
- 초기 시드머니 지급, 종목 매수/매도 기능
- 한국투자증권 API를 통한 시세 반영
- 포트폴리오 저장, 수익률 분석
- AI 기반 투자 리포트 생성 (주간 피드백)

### 5. 챗봇 (GPT)
- 성향 기반 종목 추천 / 키워드 기반 추천
- 종목 분석 요청 (시세 + 재무정보 + 뉴스 요약)
- 투자 용어 설명 및 대화 저장
- 추천 종목에 대한 사용자 피드백 기록

### 6. 랭킹 시스템
- 전체 랭킹, 성향별 랭킹, 주간 랭킹 제공
- 인기 종목 Top5, 유사 성향 인기 종목 집계
- 자동 갱신 스케줄링 (1일 or 1주 단위)

---

## 📌 API 명세 예시

| 기능 | Method | URL |
|------|--------|-----|
| 투자 성향 기반 종목 추천 | POST | `/api/chatbot/recommend/profile` |
| 키워드 기반 추천 | POST | `/api/chatbot/recommend/keyword` |
| 퀴즈 정답 제출 | POST | `/api/learning/quiz/submit` |
| 가상 주식 주문 | POST | `/api/trade/order` |
| 랭킹 조회 | GET | `/api/rankings/total` |

※ Swagger 또는 Postman 문서 연동 예정

---

## 🧾 데이터베이스 ERD

- [ERD 확인하기](https://www.erdcloud.com/d/Hyv5npmaceLrCr4kH)
- [DB 테이블 명세서](https://docs.google.com/spreadsheets/d/1Hq5XLG8044kCkO1DYgRZBnSOsUc9_5bjqr21Cf_5ny4)

---

## 🚀 배포 및 실행

```bash
# 로컬 빌드
./gradlew build

# 실행
java -jar build/libs/finzie-backend.jar

# 도커 실행
docker-compose up --build
⚙️ CI/CD 자동화
yaml
복사
편집
# .github/workflows/deploy.yml 예시
name: Deploy to EC2
on:
  push:
    branches: [ main ]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Build
        run: ./gradlew build
      - name: Deploy
        uses: easingthemes/ssh-deploy@main
        with:
          SSH_PRIVATE_KEY: ${{ secrets.EC2_SSH_KEY }}
          REMOTE_HOST: ${{ secrets.EC2_HOST }}
          REMOTE_USER: ec2-user
          TARGET: /home/ec2-user/finzie
👥 팀 역할 (예시)
이름	역할
김OO	인증 및 유저 관리 API
박OO	모의투자 매매 로직 + 시세 연동
이OO	챗봇 GPT 프롬프트 설계 및 피드백
정OO	투자 성향 테스트 및 콘텐츠 큐레이션
홍OO	랭킹 시스템 및 분석 API 개발

📎 참고 링크
📈 한국투자증권 Open API
https://apiportal.koreainvestment.com/apiservice

🤖 OpenAI GPT API
https://platform.openai.com/docs

📝 개발 위키 & 기능 명세서
기능명세서 노션
