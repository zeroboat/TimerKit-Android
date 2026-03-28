# TimerKit Android

## 프로젝트 개요

TimerKit은 Android 전용 멀티 타이머 애플리케이션입니다. 스톱워치, 타바타, 러닝, 쿠킹 4가지 타이머를 하나의 앱에서 제공하며, GPS 경로 기록과 Google Maps 연동을 지원합니다.

## 주요 기능

### 1. 스톱워치
- MM:SS.cc 형식의 정밀 시간 측정
- 랩 타임 기록 및 목록 표시

### 2. Tabata 타이머
- PREPARE → WORK → REST 상태머신 기반 인터벌 트레이닝
- 세트 수, 운동 시간, 휴식 시간 자유 설정
- 페이즈 전환 시 진동 알림

### 3. 러닝 타이머
- WARMUP → (RUN → REST) × N 인터벌 러닝 구조
- GPS 위치 권한 허용 시 달린 거리 및 페이스 실시간 표시
- 완료 후 Google Maps에서 경로 폴리라인 확인

### 4. 쿠킹 타이머
- 여러 타이머 동시 실행 (이름, 분, 초 개별 설정)
- 타이머 추가/삭제, 독립 시작/일시정지/리셋

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose (Material3, NavigationSuiteScaffold)
- **아키텍처**: MVVM (ViewModel + StateFlow)
- **백그라운드**: Foreground Service (타이머 실행 중 알림 유지)
- **위치**: FusedLocationProviderClient (play-services-location)
- **지도**: Google Maps Compose (maps-compose)
- **광고**: Google AdMob (play-services-ads)
- **저장**: DataStore Preferences (타이머 설정 영속 저장)
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 36

## 프로젝트 구조

```
app/src/main/java/com/zeroboat/timerkit/
├── MainActivity.kt              # 네비게이션, 홈 화면, 배너 광고
├── common/
│   ├── AdHelper.kt              # AdMob 초기화
│   ├── AppDataStore.kt          # DataStore 싱글턴 + 키 정의
│   ├── GpsPoint.kt              # GPS 좌표 데이터 클래스
│   ├── IntervalTimerHelper.kt   # 공통 100ms 코루틴 ticker
│   ├── LocationTracker.kt       # GPS 경로 누적 추적
│   ├── TimerService.kt          # Foreground Service
│   └── VibrationHelper.kt       # 진동 패턴 유틸
├── stopwatch/
│   ├── StopwatchViewModel.kt
│   └── StopwatchScreen.kt
├── tabata/
│   ├── TabataViewModel.kt
│   └── TabataScreen.kt
├── running/
│   ├── RunningViewModel.kt
│   ├── RunningScreen.kt
│   └── RunningMapScreen.kt      # 완료 후 지도 결과 화면
└── cooking/
    ├── CookingViewModel.kt
    └── CookingScreen.kt
```

## 빌드 및 실행

### 요구사항
- Android Studio Hedgehog 이상
- JDK 11
- 최소 Android 7.0 (API 24) 기기 또는 에뮬레이터

### 빌드 방법

1. 저장소를 클론합니다.
   ```bash
   git clone https://github.com/your-username/TimerKit-Android.git
   ```

2. Android Studio에서 프로젝트를 열고 Gradle 동기화를 수행합니다.

3. 에뮬레이터 또는 실제 기기에서 앱을 실행합니다.

### 프로덕션 배포 전 필수 설정

| 파일 | 플레이스홀더 | 교체 내용 |
|------|------------|---------|
| `AndroidManifest.xml` | `YOUR_MAPS_API_KEY` | Google Cloud Console에서 발급한 Maps API Key |
| `AndroidManifest.xml` | `ca-app-pub-3940256099942544~3347511713` | AdMob 실제 App ID |
| `MainActivity.kt` | `YOUR_BANNER_AD_UNIT_ID` | AdMob 배너 광고 단위 ID |
| `RunningMapScreen.kt` | `YOUR_INTERSTITIAL_AD_UNIT_ID` | AdMob 전면 광고 단위 ID |

> 디버그 빌드에서는 `BuildConfig.DEBUG` 분기로 테스트 광고 ID가 자동 사용됩니다.

## 권한

| 권한 | 용도 |
|------|------|
| `FOREGROUND_SERVICE` | 타이머 백그라운드 실행 |
| `POST_NOTIFICATIONS` | 타이머 실행 중 알림 표시 |
| `VIBRATE` | 페이즈 전환 진동 |
| `ACCESS_FINE_LOCATION` | 러닝 GPS 경로 기록 (선택) |

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.
