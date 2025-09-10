# AMHS Data Generator - Docker 배포 가이드

## 개요
AMHS Data Generator 애플리케이션을 Docker를 사용하여 배포하는 방법을 설명합니다.

## 주요 기능
- 🚀 AMHS 데이터 자동 생성
- 📊 2D/3D 시각화
- 🗄️ H2 Console (수퍼유저 전용)
- 👤 사용자 관리 시스템
- 🔐 수퍼유저 권한 관리

## 사전 요구사항
- Docker 20.10+
- Docker Compose 2.0+
- 최소 2GB RAM
- 최소 1GB 디스크 공간

## 빠른 시작

### 1. 저장소 클론
```bash
git clone <repository-url>
cd AMHSDataGen
```

### 2. Docker Compose로 실행
```bash
# 애플리케이션 빌드 및 실행
docker-compose up --build

# 백그라운드에서 실행
docker-compose up -d --build
```

### 3. 애플리케이션 접근
- **메인 애플리케이션**: http://localhost:8080
- **API 문서**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console (수퍼유저만 접근 가능)

## 기본 계정 정보
- **수퍼유저**: admin / admin
- **일반 사용자**: 자동 회원가입 가능

## Docker 명령어

### 빌드
```bash
# Docker 이미지 빌드
docker build -t amhs-data-generator .

# 특정 태그로 빌드
docker build -t amhs-data-generator:latest .
```

### 실행
```bash
# 단일 컨테이너 실행
docker run -d \
  --name amhs-data-generator \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  amhs-data-generator

# 환경 변수와 함께 실행
docker run -d \
  --name amhs-data-generator \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e JAVA_OPTS="-Xmx1024m -Xms512m" \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  amhs-data-generator
```

### 관리
```bash
# 컨테이너 상태 확인
docker ps

# 로그 확인
docker logs amhs-data-generator

# 실시간 로그 확인
docker logs -f amhs-data-generator

# 컨테이너 중지
docker stop amhs-data-generator

# 컨테이너 제거
docker rm amhs-data-generator

# 이미지 제거
docker rmi amhs-data-generator
```

## Docker Compose 명령어

### 기본 명령어
```bash
# 서비스 시작
docker-compose up

# 백그라운드에서 시작
docker-compose up -d

# 서비스 중지
docker-compose down

# 서비스 재시작
docker-compose restart

# 로그 확인
docker-compose logs

# 특정 서비스 로그 확인
docker-compose logs amhs-data-generator
```

### 고급 명령어
```bash
# 강제 재빌드
docker-compose up --build --force-recreate

# 볼륨까지 제거
docker-compose down -v

# 이미지까지 제거
docker-compose down --rmi all
```

## 환경 변수

### 주요 환경 변수
| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SPRING_PROFILES_ACTIVE` | production | Spring 프로파일 |
| `JAVA_OPTS` | -Xmx1024m -Xms512m | JVM 옵션 |
| `TZ` | Asia/Seoul | 시간대 |
| `PORT` | 8080 | 서버 포트 |

### 설정 예시
```bash
# docker-compose.yml에서 설정
environment:
  - SPRING_PROFILES_ACTIVE=production
  - JAVA_OPTS=-Xmx2048m -Xms1024m
  - TZ=Asia/Seoul
```

## 볼륨 마운트

### 데이터 디렉토리
- `./data:/app/data` - 애플리케이션 데이터
- `./logs:/app/logs` - 로그 파일
- `amhs_data:/app/data/persistent` - 영구 데이터 (Docker 볼륨)

### 로그 파일 위치
- 애플리케이션 로그: `/app/logs/amhs-data-generator.log`
- Docker 로그: `docker logs amhs-data-generator`

## 헬스 체크

### 자동 헬스 체크
Docker는 30초마다 애플리케이션 상태를 확인합니다:
```bash
# 헬스 체크 상태 확인
docker inspect amhs-data-generator | grep -A 10 Health
```

### 수동 헬스 체크
```bash
# HTTP 헬스 체크
curl http://localhost:8080/actuator/health

# 컨테이너 내부에서 확인
docker exec amhs-data-generator curl -f http://localhost:8080/actuator/health
```

## 보안 설정

### H2 Console 접근
- H2 Console은 수퍼유저(admin)만 접근 가능
- 외부 접근 차단: `spring.h2.console.settings.web-allow-others=false`

### 네트워크 보안
```yaml
# docker-compose.yml에서 네트워크 설정
networks:
  amhs-network:
    driver: bridge
    internal: false  # 외부 접근 허용
```

## 모니터링

### 리소스 사용량 확인
```bash
# 컨테이너 리소스 사용량
docker stats amhs-data-generator

# 디스크 사용량
docker system df
```

### 로그 모니터링
```bash
# 실시간 로그 모니터링
docker-compose logs -f

# 특정 시간대 로그
docker-compose logs --since="2024-01-01T00:00:00"
```

## 문제 해결

### 일반적인 문제

#### 1. 포트 충돌
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8080

# 다른 포트로 실행
docker run -p 8081:8080 amhs-data-generator
```

#### 2. 메모리 부족
```bash
# JVM 힙 크기 조정
docker run -e JAVA_OPTS="-Xmx2048m -Xms1024m" amhs-data-generator
```

#### 3. 권한 문제
```bash
# 볼륨 권한 설정
sudo chown -R 1000:1000 ./data ./logs
```

### 로그 분석
```bash
# 에러 로그 필터링
docker logs amhs-data-generator 2>&1 | grep ERROR

# 특정 시간대 로그
docker logs --since="1h" amhs-data-generator
```

## 프로덕션 배포

### 1. 환경 설정
```bash
# 프로덕션 환경 변수 설정
export SPRING_PROFILES_ACTIVE=production
export JAVA_OPTS="-Xmx2048m -Xms1024m"
```

### 2. 보안 강화
- 방화벽 설정
- SSL/TLS 인증서 적용
- 정기적인 보안 업데이트

### 3. 백업 전략
```bash
# 데이터 백업
docker run --rm -v amhs_data:/data -v $(pwd):/backup alpine tar czf /backup/amhs-backup.tar.gz -C /data .

# 데이터 복원
docker run --rm -v amhs_data:/data -v $(pwd):/backup alpine tar xzf /backup/amhs-backup.tar.gz -C /data
```

## 업데이트

### 애플리케이션 업데이트
```bash
# 최신 코드 가져오기
git pull origin main

# 이미지 재빌드
docker-compose up --build -d

# 이전 이미지 정리
docker image prune -f
```

## 지원

문제가 발생하면 다음을 확인하세요:
1. Docker 로그: `docker logs amhs-data-generator`
2. 애플리케이션 로그: `./logs/amhs-data-generator.log`
3. 시스템 리소스: `docker stats amhs-data-generator`
4. 네트워크 연결: `curl http://localhost:8080/actuator/health`
