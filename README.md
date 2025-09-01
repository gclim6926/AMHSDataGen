# AMHSDataGen

Spring Boot 기반 AMHS 데이터 생성/레이아웃 시각화 도구. 기존 Python 스크립트를 Java(Spring MVC) + Thymeleaf + Plotly.js로 이식/확장했습니다.

## 주요 기능

- Layout Seed: `data/input.json` 확인/수정
- add Addresses/Lines: 주소·라인 자동 생성 및 연결
- Checker: 중복/겹침 라인 점검 및 정리, 레이아웃 저장
- add Stations: 라인 구간을 분할해 스테이션 생성
- OHT Track Maker: 다중 OHT 경로(UDP 트랙) 동시 생성
- 2D/3D Viewer: Plotly.js로 레이아웃 및 OHT 시뮬레이션 시각화

## 빠른 시작

- 요구사항: JDK 17+, Gradle Wrapper 포함
- 실행
  - 개발 실행: `./gradlew bootRun`
  - 빌드: `./gradlew build -x test`
  - 접속: 브라우저에서 `http://localhost:8080/`

## 디렉토리 구조(요약)

```text
AMHSDataGen/
  ├─ src/main/java/demo/amhsdatagen
  │   ├─ controller/  # MVC Controller 및 REST API 엔드포인트
  │   ├─ model/       # 데이터 모델(POJO) - Address, Line, Position 등
  │   ├─ service/     # 비즈니스 로직 - Generator·Checker·Stations·UDP 등
  │   ├─ config/      # 초기화/설정 구성 (StartupInitializer 등)
  │   └─ DataGenApplication.java
  ├─ src/main/resources
  │   ├─ templates/   # Thymeleaf 템플릿 (index, viewer2d, viewer3d, 404 등)
  │   ├─ static/      # JS/CSS(예: static/js/actions.js)
  │   └─ application.properties
  ├─ data/            # 변경/결과 데이터 및 로그: input.json, output.json, layout.json, *.log
  ├─ build.gradle
  └─ README.md
```

## 설정(application.properties)

- `spring.application.name=layoutviz`
- `app.data.dir=data`
- `app.files.input=input.json`
- `app.files.output=output.json`
- `app.files.input-sample=input.sample.json`
- `app.files.check-log=check.log`
- `app.files.udp-log=output_oht_track_data.log`
- `app.files.layout=layout.json`

모든 파일은 기본적으로 프로젝트 루트의 `data/` 하위에 생성·갱신됩니다.

## 실행 방법(웹 UI)

- 메인: `http://localhost:8080/`
  - 좌측 메뉴에서 각 기능 실행
  - 2D/3D Viewer는 새 창으로 열리며, Layer/Component 필터 적용 가능
- 2D Viewer: `/viewer2d?layers=z6022,z4822&overlap=1&comps=lines,addresses,stations,ohts`
- 3D Viewer: `/viewer3d?layers=z6022,z4822&overlap=1&primary=z4822&comps=lines,addresses,stations,ohts`

## REST API(요약)

- `POST /api/run-generate` — 주소/라인 생성
- `POST /api/run-add-lines` — 미연결 주소/엔드포인트 연결
- `POST /api/run-check` — 데이터 무결성 점검 및 정리
- `POST /api/run-stations` — 스테이션 생성
- `POST /api/run-udp-generator` — 단일 OHT 트랙 생성
- `POST /api/run-udp-generator-bulk` — 다중 OHT 트랙 동시 생성
- `GET  /api/get-input-data` — 입력(json) 조회
- `POST /api/update-input-json` — 입력(json) 갱신
- `GET  /api/get-output-json` — 출력(json) 조회
- `GET  /api/get-udp-log` — OHT 트랙 로그 조회

## OHT Track Maker

- UI: 메뉴의 “OHT Track Maker” → OHT_0 ~ OHT_9 기본값 제공
- 체크된 OHT만 대상으로, 모든 OHT가 같은 틱에서 동시에 다음 주소로 이동하는 로그를 생성합니다.
- 로그 파일: `data/output_oht_track_data.log`
- 로그 포맷 예시(각 라인의 MCP/OHT 표기):

```text
[2025-08-25 18:02:34.828]IP:10.10.10.1, Port=3600, Descrption:DT, Message=2,OHT1,V00001,1,0,0000,1,100010,0,100451,2,1,AAAA0000,100110,00000000,0000, ,0,101,0
```

- “Message=2,OHT1,” 형태로 OHT 식별자를 기록합니다.

## 시각화(2D/3D)

- Plotly.js를 사용해 브라우저에서 렌더링
- Layer 필터: `z0`, `z4822`, `z6022` 선택, Overlap 모드 지원
- Component 필터: `lines`, `addresses`, `stations`, `ohts`
- OHT: 로그 기반 애니메이션, 2D/3D 각각 다른 마커 크기
- 2D는 초기 데이터 범위로 축을 고정하여 OHT가 가장자리로 이동해도 스케일 변화가 없도록 설정

## 내부 로직(요약)

- Generator: 입력(JSON)을 기반으로 주소/라인 생성, 레이어별 처리
- Line Endpoint: 미사용 주소·엔드포인트 연결, 최근접 탐색 및 중복/교차 방지
- Checker: 중복 주소/라인, 라인 겹침 검사 및 정리, 리포트/로그 작성
- Stations: 라인을 구간으로 나눠 선택/배치, 스테이션 엔티티 생성 및 출력 JSON 반영
- UDP Generator: 주소 그래프 구성 → BFS 최단 경로 → OHT 트랙 로그 생성(동시 틱)

## 개발 팁

- 빌드: `./gradlew build -x test`
- 실행: `./gradlew bootRun`
- 로그 확인: `data/check.log`, `data/output_oht_track_data.log`
- 데이터 초기화: 필요 시 `data/` 내 파일 교체 또는 `resources/data/input.sample.json` 사용

## GitHub

- 원격 저장소(HTTPS): `https://github.com/gclim6926/AMHSDataGen.git`
- 연결/푸시 예시:

```bash
git init
git add -A && git commit -m "Initial import"
git branch -M main
git remote add origin https://github.com/gclim6926/AMHSDataGen.git
git push -u origin main
```

## 라이선스

- 프로젝트에 맞는 라이선스를 선택해 `LICENSE` 파일로 추가하세요. (예: MIT)
