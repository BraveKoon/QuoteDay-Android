# QuoteDay Android

[QuoteDay](https://github.com/BraveKoon/QuoteDay) 의 안드로이드판. Kotlin + Jetpack Compose.

## 이 저장소가 iOS 저장소와 맺는 관계

앱 코드는 공유하지 않는다. SwiftUI 앱을 안드로이드로 "포팅"할 방법은 없고,
실제로 옮겨지는 것은 **데이터와 알고리즘**뿐이다.

    옮겨지는 것    명언 201편, 인물 116명, 비하인드 41편, 외국어 이름 360개
                  결정적 해시, 오늘의 명언 선택, 빈칸 만들기, 점수·구간·시즌 계산
    다시 쓰는 것   화면 전부, 위젯, 알림, 저장소, 캘린더 연동

데이터는 손으로 베끼지 않는다. `tools/extract_from_swift.py` 가 iOS 저장소의
Swift 원본을 읽어 JSON 으로 옮기고, 개수와 참조 무결성을 검사한다.

```bash
python3 tools/extract_from_swift.py ../QuoteDay
```

## 왜 `:core` 가 순수 Kotlin 인가

이 저장소를 만드는 환경에는 Android SDK 가 없다(네트워크 정책에 막혀 있다).
그래서 **가장 틀리기 쉬운 부분을 안드로이드 밖으로 빼** 두었다.

    :core   순수 Kotlin/JVM. 데이터와 알고리즘. 어디서나 컴파일하고 테스트한다.
    :app    안드로이드. 화면·위젯·저장소. CI 가 컴파일한다.

`:core` 의 테스트는 iOS 와 **같은 값**이 나오는지 확인한다. 해시가 한 비트라도
다르면 같은 날 두 플랫폼이 서로 다른 명언을 보여 주는데, 그 어느 것도 앱을
죽이지 않아서 아무도 눈치채지 못한다. 그래서 기대값을 테스트에 박아 두었다.

```bash
gradle :core:test
```

`:app` 은 이 환경에서 빌드되지 않는다. `settings.gradle.kts` 가 Android SDK 를
찾지 못하면 `:app` 을 아예 빼고 설정하는데, 그러지 않으면 AGP 플러그인 마커를
받으러 나가다가 `:core:test` 까지 같이 죽기 때문이다. `:app` 은 CI 가 빌드한다.

## 지금 어디까지 되어 있나

- [x] 데이터 추출(명언·인물·비하인드·이름표) + 무결성 검사
- [x] `:core` — 모델, 라이브러리, 결정적 해시, 오늘의 명언 선택, 이름 정규화
- [x] 챌린지(빈칸 만들기 · 문제 생성 · 채점 · 점수)
- [x] 테스트 43개 (iOS 대조 + 챌린지)
- [x] 일정 — 반복 규칙, 회차 계산, 검증
- [x] `:app` — 오늘 · 일정 · 명언 · 챌린지 · 설정 화면
- [x] 알림 — 일정 회차와 매일 정해진 시각
- [x] 위젯 — 홈 화면의 오늘의 명언
- [x] CI — `:core` 테스트, `:app` 디버그 APK, 릴리스 빌드
- [x] 릴리스 워크플로 — 서명된 AAB
- [ ] 하트 서버 (지금은 기기 안에만 쌓인다)
- [ ] 랭킹

## 하트에 대해

iOS 는 하트를 CloudKit 공개 데이터베이스에 넣는다. **안드로이드는 CloudKit 에
접근할 수 없다** — 애플 전용이다.

그래서 당분간 하트 수는 플랫폼마다 따로 쌓인다. 두 쪽을 합치려면 공용 백엔드로
옮겨야 하고, 그것은 iOS 앱도 고쳐야 한다는 뜻이다. iOS 쪽은 `HeartSyncing`
프로토콜 뒤에 구현이 하나 있을 뿐이라 교체 자체는 파일 하나 분량이다.
여기서도 같은 모양의 인터페이스를 두어, 나중에 합칠 때 양쪽이 같은 구현을
보도록 한다.

랭킹은 이번 버전에 넣지 않는다.

## 안드로이드 출시

### 1. 키스토어 만들기

한 번 만들면 **그 앱의 평생 신분증**이다. 잃어버리면 같은 앱의 업데이트를 낼 수
없다(Play 앱 서명을 쓰면 복구 경로가 있지만, 업로드 키를 다시 등록해야 한다).
저장소에 커밋하지 말고, 따로 안전한 곳에 보관한다.

```bash
keytool -genkeypair -v \
  -keystore quoteday.jks \
  -alias quoteday \
  -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 저장소 시크릿에 넣기

`Settings → Secrets and variables → Actions` 에 네 개를 만든다.

    QUOTEDAY_KEYSTORE_BASE64   base64 -w0 quoteday.jks 의 출력
    QUOTEDAY_STORE_PASSWORD    키스토어 비밀번호
    QUOTEDAY_KEY_ALIAS         quoteday
    QUOTEDAY_KEY_PASSWORD      키 비밀번호

로컬에서 서명해 보려면 `keystore.properties` 를 저장소 뿌리에 두면 된다
(gitignore 되어 있다).

    storeFile=/절대/경로/quoteday.jks
    storePassword=...
    keyAlias=quoteday
    keyPassword=...

### 3. AAB 만들기

Actions → **릴리스 AAB** → Run workflow. 버전을 비워 두면 `build.gradle.kts` 의
값을 쓴다. 스토어는 같은 `versionCode` 를 두 번 받지 않으므로 올릴 때마다 올린다.

산출물 두 개가 나온다.

    quoteday-release-aab       Play Console 에 올리는 파일
    quoteday-release-mapping   난독화 해제용. 크래시 리포트를 읽으려면 함께 올린다.

### 4. Play Console

1. 개발자 계정 등록(1회, 25달러)
2. 앱 만들기 → 이름 "오늘의 명언", 언어 한국어, 무료
3. 앱 콘텐츠 — 개인정보처리방침 URL, 데이터 보안 설문, 광고 없음, 콘텐츠 등급
4. 프로덕션 → 새 버전 → AAB 업로드 → 출시 노트
5. 심사 (보통 며칠)

개인정보처리방침은 iOS 저장소의 `docs/PRIVACY.md` 를 그대로 쓸 수 있다. 다만
**하트가 어디에 쌓이는지**가 두 플랫폼에서 다르다 — 안드로이드판은 아직 기기
안에만 쌓이므로, 그 문장은 고쳐야 한다.

