# 📋 [상세 가이드] 1단계: 화면의 기둥 세우기 (Scaffold 완벽 정복)

이 문서는 Jetpack Compose의 꽃인 **Scaffold(스캐폴드)**를 사용하여 커뮤니티 화면의 전체적인 틀을 잡는 방법을 비전공자도 이해하기 쉽게 상세히 설명합니다.

---

## 🍱 왜 Scaffold를 '식판'이라고 부를까요?

우리가 식당에 가면 국 칸, 밥 칸, 반찬 칸이 나뉜 식판을 받습니다. `Scaffold`도 마찬가지입니다. 구글이 이미 **"여기는 지붕(상단 바), 여기는 바닥(메뉴 바), 여기는 동동 버튼 자리"**라고 칸을 다 나눠두었습니다.

우리는 그 칸에 우리가 원하는 부품(레고 블록)을 쏙쏙 집어넣기만 하면 됩니다.

---

## 🔍 1단계 상세 분석: 4가지 핵심 칸 채우기

### 1. topBar (상단 지붕 영역)
화면 맨 위에서 스크롤을 내려도 고정되어 있는 부분입니다.
- **주로 넣는 것**: 앱 이름, 뒤로가기 버튼, 검색창.
- **상세 팁**: `TopAppBar`라는 부품을 사용합니다. 이 부품의 `title = { }` 안에는 무엇이든 넣을 수 있습니다.

---

## 🆘 긴급 조치: topBar 괄호 오류 (Unresolved reference: modifier)

비전공자가 가장 많이 실수하는 부분입니다! `topBar = { ... }`는 **중괄호`{ }`**를 써야 하는데, 코드가 꼬이면 빨간 줄이 생깁니다.

### 🚨 무엇이 잘못되었나요?
- **코드 상태**: `topBar = ( modifier = Modifier.statusBarsPadding() ) { ... }` ❌
- **문제점**: 
  1. `topBar`는 칸의 이름일 뿐이고, 바로 중괄호 `{ }`를 써야 합니다.
  2. `modifier`(옷)는 칸 자체가 입는 게 아니라, 그 안에 들어온 **부품(`TopAppBar`)**이 입어야 합니다.

### ✅ 올바른 조립 순서 (이대로 따라하세요!)
```kotlin
topBar = {  // <-- 시작은 무조건 중괄호 {
    TopAppBar(
        modifier = Modifier.statusBarsPadding(), // <-- 옷(modifier)은 여기에!
        title = {
            OutlinedTextField( ... )
        }
    )
} // <-- 마지막은 무조건 중괄호 }
```

---

## 📱 상단 바가 카메라나 상태표시줄을 가릴 때 (Safe Area)

### 🚨 "수동으로 천장 여백 주기" (statusBarsPadding)
- **해결법**: `TopAppBar`의 `modifier`에 `.statusBarsPadding()`을 추가하세요.

---

## 🎨 검색창을 사진처럼 예쁘게 조절하는 법 (중요!)

### 🚨 1. "가로로 꽉 채워라!" (Modifier.fillMaxWidth)
### 🚨 2. "힌트 글자와 아이콘 넣기" (placeholder)

---

## 🆘 긴급 조치: innerPadding 빨간 줄 해결하기

### 🚨 범인 1: "선물을 줬는데 왜 안 쓰니?" (Unused Parameter)
- **해결법**: `LazyColumn` 괄호 안에 `modifier = Modifier.padding(innerPadding)`을 꼭 써주세요.

---

## 💡 한눈에 보는 Scaffold 조립 도면 (최종본)

```kotlin
Scaffold(
    topBar = { 
        TopAppBar(
            modifier = Modifier.statusBarsPadding(), // 상단 여백 보호!
            title = {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    placeholder = { Text("검색") }
                )
            }
        )
    }
) { innerPadding -> 
    LazyColumn(modifier = Modifier.padding(innerPadding)) { 
        /* 리스트 내용 */ 
    }
}
```
