# TimerKit Android

## 프로젝트 개요

TimerKit은 Android 전용 타이머 애플리케이션입니다. 다양한 상황에 맞는 4가지 타이머 기능을 제공하여 사용자들이 시간을 효율적으로 관리할 수 있도록 도와줍니다.

## 주요 기능

### 1. Tabata 타이머
- 고강도 인터벌 트레이닝을 위한 타이머
- 준비, 운동, 휴식 시간을 설정하여 자동으로 진행

### 2. 스톱워치
- 기본적인 시간 측정 기능
- 시작, 정지, 리셋 기능 제공

### 3. 쿠킹 타이머
- 요리 시간을 정확하게 측정
- 다중 타이머 지원으로 여러 요리 과정을 동시에 관리

### 4. 러닝 타이머
- 달리기 훈련을 위한 인터벌 타이머
- 워밍업, 러닝, 휴식 시간을 설정하여 운동 루틴 관리

## 기술 스택

- **언어**: Kotlin
- **플랫폼**: Android
- **빌드 도구**: Gradle
- **아키텍처**: MVVM (예정)

## 설치 및 실행

### 요구사항
- Android Studio Arctic Fox 이상
- 최소 Android API 21 (Android 5.0)

### 빌드 방법
1. 프로젝트를 클론합니다.
   ```bash
   git clone https://github.com/your-username/TimerKit-Android.git
   ```

2. Android Studio에서 프로젝트를 엽니다.

3. Gradle 동기화를 수행합니다.

4. 에뮬레이터 또는 실제 기기에서 앱을 실행합니다.

## 프로젝트 구조

```
app/
├── src/main/java/com/zeroboat/timerkit/
│   ├── MainActivity.kt          # 메인 액티비티
│   └── ui/
│       └── theme/               # UI 테마 관련 파일들
└── src/main/res/                # 리소스 파일들
```

## 개발 계획

- [ ] 기본 UI 디자인 구현
- [ ] 각 타이머 기능 개발
- [ ] 알림 및 소리 기능 추가
- [ ] 다크 모드 지원
- [ ] 데이터 저장 기능 (SharedPreferences/SQLite)

## 기여 방법

1. 이 저장소를 포크합니다.
2. 새로운 브랜치를 생성합니다 (`git checkout -b feature/AmazingFeature`).
3. 변경사항을 커밋합니다 (`git commit -m 'Add some AmazingFeature'`).
4. 브랜치에 푸시합니다 (`git push origin feature/AmazingFeature`).
5. Pull Request를 생성합니다.

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

## 연락처

프로젝트에 대한 질문이나 제안사항이 있으시면 [이슈](https://github.com/your-username/TimerKit-Android/issues)를 통해 연락주세요.