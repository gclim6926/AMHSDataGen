# 🏭 AMHS Data Generator

**현대적인 웹 기반 AMHS(Automated Material Handling System) 레이아웃 설계 및 시각화 도구**

Spring Boot와 현대적인 웹 기술을 활용하여 복잡한 AMHS 레이아웃을 직관적으로 설계하고 시각화할 수 있는 통합 솔루션입니다.

## ✨ 주요 기능

### 🎨 **스마트 샘플 편집기 (Layout Seed)**
- **3가지 프리셋 템플릿**: Complex (3층), Extended (2층), Basic (1층) 구조
- **직관적인 카드 UI**: 이미지 클릭만으로 샘플 로드
- **실시간 편집**: 웹 기반 폼 에디터로 즉시 수정 가능
- **자동 저장**: 데이터베이스 연동으로 작업 내용 자동 보존

### 📊 **고급 시각화 엔진**
- **3D 인터랙티브 뷰어**: Plotly.js 기반 몰입감 있는 3D 시각화
- **레이어 필터링**: z6022(상층), z4822(중층), z3000(하층) 개별 제어
- **색상 코딩 시스템**: 레이어별 직관적 구분 (빨강, 파랑, 밝은 녹색)
- **Overlap 모드**: 모든 레이어를 통합하여 전체 구조 파악

### 💾 **강력한 데이터 관리**
- **H2 인메모리 데이터베이스**: 빠른 응답속도와 세션 격리
- **사용자별 워크스페이스**: 개별 작업 환경 제공
- **REST API**: 완전한 CRUD 지원으로 확장성 보장
- **실시간 동기화**: 프론트엔드-백엔드 실시간 데이터 연동

### 🔧 **추가 도구들**
- **Address/Line Generator**: 자동 주소 및 라인 생성
- **Layout Checker**: 중복/겹침 검증 및 정리
- **Station Manager**: 라인 구간 분할 및 스테이션 생성
- **OHT Track Maker**: 다중 OHT 경로 동시 생성
- **UDP Generator**: OHT 시뮬레이션 데이터 생성

## 🚀 빠른 시작

### 요구사항
- **Java 21+** (OpenJDK 권장)
- **Gradle** (Wrapper 포함)
- **현대적인 웹 브라우저** (Chrome, Firefox, Safari, Edge)

### 로컬 개발 환경
```bash
# 저장소 클론
git clone <repository-url>
cd AMHSDataGen_bu0910

# 개발 서버 실행
./gradlew bootRun

# 브라우저에서 접속
open http://localhost:8080
```

### 프로덕션 빌드
```bash
# 일반 빌드
./gradlew build -x test

# 최적화된 프로덕션 빌드
./gradlew productionBuild

# 배포 스크립트 실행
./deploy.sh production
```

## 🐳 Docker 배포

### Docker 이미지 빌드
```bash
# 이미지 빌드
docker build -t amhs-datagen:latest .

# 컨테이너 실행
docker run -p 8080:8080 amhs-datagen:latest
```

### Docker Compose
```bash
# Docker Compose로 실행
docker-compose up -d
```

## ☁️ 클라우드 배포

### Render.com 배포
1. **GitHub 저장소 연결**
2. **빌드 설정**:
   - Build Command: `./gradlew productionBuild`
   - Start Command: `java -jar build/libs/AMHSDataGen-production.jar`
3. **환경 변수**: `SPRING_PROFILES_ACTIVE=production`

### Heroku 배포
```bash
# Heroku CLI 설치 후
heroku create your-app-name
heroku config:set SPRING_PROFILES_ACTIVE=production
git push heroku main
```

## 📁 프로젝트 구조

```
AMHSDataGen_bu0910/
├── src/main/
│   ├── java/demo/amhsdatagen/
│   │   ├── controller/          # REST API 컨트롤러
│   │   ├── service/            # 비즈니스 로직
│   │   ├── model/              # 데이터 모델
│   │   └── repository/         # 데이터 액세스
│   └── resources/
│       ├── static/
│       │   ├── css/           # 모던 스타일시트
│       │   ├── js/            # 최적화된 JavaScript
│       │   └── images/        # 샘플 이미지
│       ├── templates/         # Thymeleaf 템플릿
│       └── data/             # 샘플 JSON 파일
├── build.gradle              # 빌드 설정
├── Dockerfile                # Docker 설정
├── docker-compose.yml        # Docker Compose
└── deploy.sh                 # 배포 스크립트
```

## 🎯 사용 방법

### 1. 샘플 선택
- **Layout Seed** 메뉴 클릭
- 원하는 샘플 카드 클릭 (Complex/Extended/Basic)
- 자동으로 에디터에 로드 및 DB 저장

### 2. 레이아웃 편집
- 웹 폼에서 JSON 데이터 직접 편집
- **"update to layout_seed.input"** 버튼으로 저장

### 3. 시각화
- **2D Viewer** 또는 **3D Viewer** 선택
- 레이어 필터로 원하는 층만 표시
- Overlap 모드로 전체 구조 확인

### 4. 데이터 생성
- **Add Addresses**: 주소 데이터 자동 생성
- **Add Lines**: 라인 연결 정보 생성
- **Stations**: 스테이션 배치 최적화

## 🔧 기술 스택

### Backend
- **Spring Boot 3.4.9** - 현대적인 Java 웹 프레임워크
- **Spring MVC** - RESTful API 구축
- **Spring Data JPA** - 데이터 액세스 추상화
- **H2 Database** - 인메모리 데이터베이스
- **Jackson** - JSON 처리

### Frontend
- **Thymeleaf** - 서버사이드 템플릿 엔진
- **Plotly.js** - 고성능 시각화 라이브러리
- **Modern CSS3** - CSS Variables, Flexbox, Grid
- **Vanilla JavaScript** - 최적화된 클라이언트 코드

### DevOps
- **Gradle** - 빌드 자동화
- **Docker** - 컨테이너화
- **Multi-stage Build** - 최적화된 이미지

## 📊 성능 최적화

- **CSS Variables**: 일관된 디자인 시스템
- **JavaScript 모듈화**: 유지보수성 향상
- **AbortController**: 요청 타임아웃 처리
- **Server Compression**: Gzip 압축 활성화
- **Static Resource Caching**: 브라우저 캐싱 최적화

## 🛡️ 보안 기능

- **Non-root Container**: Docker 보안 강화
- **Input Validation**: XSS 방지
- **CORS 설정**: 크로스 오리진 요청 제어
- **Session Management**: 사용자별 데이터 격리

## 📝 API 문서

애플리케이션 실행 후 Swagger UI에서 API 문서 확인:
- **로컬**: http://localhost:8080/swagger-ui.html
- **프로덕션**: https://your-domain.com/swagger-ui.html

## 🤝 기여하기

1. Fork 프로젝트
2. Feature 브랜치 생성 (`git checkout -b feature/AmazingFeature`)
3. 변경사항 커밋 (`git commit -m 'Add some AmazingFeature'`)
4. 브랜치에 Push (`git push origin feature/AmazingFeature`)
5. Pull Request 생성

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 지원

- **이슈 리포트**: GitHub Issues
- **문의사항**: 프로젝트 관리자에게 연락
- **문서**: 프로젝트 Wiki 참조

---

**AMHS Data Generator**로 더 효율적이고 직관적인 AMHS 레이아웃 설계를 경험해보세요! 🚀