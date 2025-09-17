#!/bin/bash

# AMHS Data Generator 배포 스크립트
set -e

echo "🚀 AMHS Data Generator 배포 시작..."

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
print_step() {
    echo -e "${BLUE}==== $1 ====${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 환경 변수 설정
PROFILE=${1:-production}
BUILD_TYPE=${2:-production}

print_step "환경 설정 확인"
echo "Profile: $PROFILE"
echo "Build Type: $BUILD_TYPE"

# Java 버전 확인
print_step "Java 버전 확인"
if java -version 2>&1 | grep -q "21"; then
    print_success "Java 21이 설치되어 있습니다."
else
    print_error "Java 21이 필요합니다."
    exit 1
fi

# 기존 빌드 정리
print_step "기존 빌드 정리"
if [ -d "build" ]; then
    rm -rf build
    print_success "기존 빌드 디렉토리 삭제 완료"
fi

# 의존성 다운로드
print_step "의존성 다운로드"
./gradlew dependencies --no-daemon
print_success "의존성 다운로드 완료"

# 테스트 실행
print_step "테스트 실행"
./gradlew test --no-daemon
if [ $? -eq 0 ]; then
    print_success "모든 테스트 통과"
else
    print_warning "일부 테스트 실패 - 계속 진행합니다"
fi

# 빌드 실행
print_step "애플리케이션 빌드"
if [ "$BUILD_TYPE" = "production" ]; then
    ./gradlew productionBuild --no-daemon
    JAR_FILE="build/libs/AMHSDataGen-production.jar"
else
    ./gradlew bootJar --no-daemon
    JAR_FILE="build/libs/AMHSDataGen-0.0.1-SNAPSHOT.jar"
fi

if [ -f "$JAR_FILE" ]; then
    print_success "빌드 완료: $JAR_FILE"
else
    print_error "빌드 실패: JAR 파일을 찾을 수 없습니다"
    exit 1
fi

# JAR 파일 정보 출력
print_step "빌드 결과"
ls -lh "$JAR_FILE"
echo "파일 크기: $(du -h "$JAR_FILE" | cut -f1)"

# Docker 이미지 빌드 (옵션)
if command -v docker &> /dev/null; then
    read -p "Docker 이미지를 빌드하시겠습니까? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_step "Docker 이미지 빌드"
        docker build -t amhs-datagen:latest .
        docker build -t amhs-datagen:$PROFILE .
        print_success "Docker 이미지 빌드 완료"
        
        # 이미지 크기 출력
        echo "Docker 이미지 정보:"
        docker images | grep amhs-datagen
    fi
fi

# 배포 준비 완료
print_step "배포 준비 완료"
print_success "배포 준비가 완료되었습니다!"

echo ""
echo "🎯 배포 방법:"
echo "1. 로컬 실행:"
echo "   java -jar $JAR_FILE"
echo ""
echo "2. Docker 실행 (이미지가 빌드된 경우):"
echo "   docker run -p 8080:8080 amhs-datagen:$PROFILE"
echo ""
echo "3. 클라우드 배포:"
echo "   - JAR 파일을 클라우드 플랫폼에 업로드"
echo "   - 환경 변수 SPRING_PROFILES_ACTIVE=$PROFILE 설정"
echo ""

print_success "배포 스크립트 실행 완료! 🎉"
