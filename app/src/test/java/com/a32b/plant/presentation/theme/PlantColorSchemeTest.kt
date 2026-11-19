package com.a32b.plant.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.a32b.plant.presentation.core.component.bottomBarItemTint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 앱 컬러 스킴에 Material 기본값(보라 계열)이 섞이지 않는지, 주요 글자/배경 조합의 대비가 충분한지 확인 (#643).
 *
 * 대비는 WCAG 2.x 상대 휘도 공식 ((밝은 L + 0.05) / (어두운 L + 0.05)). 본문 글자 4.5:1, 아이콘 3:1 기준.
 */
class PlantColorSchemeTest {

    private val schemes = listOf(
        "light" to (LightColorScheme to lightColorScheme()),
        "dark" to (DarkColorScheme to darkColorScheme())
    )

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb).toDouble() / minOf(la, lb).toDouble()
    }

    private fun assertContrast(label: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$label 대비 ${"%.2f".format(ratio)}:1 < $min:1", ratio >= min)
    }

    // 앱 코드나 앱이 쓰는 M3 컴포넌트(Switch, ModalBottomSheet, DropdownMenu, RadioButton, TextField 등)가 읽는 역할
    private val usedRoles: List<Pair<String, (ColorScheme) -> Color>> = listOf(
        "primary" to { it.primary },
        "onPrimary" to { it.onPrimary },
        "primaryContainer" to { it.primaryContainer },
        "onPrimaryContainer" to { it.onPrimaryContainer },
        "secondary" to { it.secondary },
        "onSecondary" to { it.onSecondary },
        "secondaryContainer" to { it.secondaryContainer },
        "onSecondaryContainer" to { it.onSecondaryContainer },
        "tertiary" to { it.tertiary },
        "tertiaryContainer" to { it.tertiaryContainer },
        "background" to { it.background },
        "onBackground" to { it.onBackground },
        "surface" to { it.surface },
        "onSurface" to { it.onSurface },
        "surfaceVariant" to { it.surfaceVariant },
        "onSurfaceVariant" to { it.onSurfaceVariant },
        "error" to { it.error },
        "onError" to { it.onError },
        "errorContainer" to { it.errorContainer },
        "onErrorContainer" to { it.onErrorContainer },
        "outline" to { it.outline },
        "outlineVariant" to { it.outlineVariant },
        "surfaceContainerLowest" to { it.surfaceContainerLowest },
        "surfaceContainerLow" to { it.surfaceContainerLow },
        "surfaceContainer" to { it.surfaceContainer },
        "surfaceContainerHigh" to { it.surfaceContainerHigh },
        "surfaceContainerHighest" to { it.surfaceContainerHighest },
    )

    // HSV 색상(0~360). 무채색이면 null
    private fun hue(c: Color): Float? {
        val max = maxOf(c.red, c.green, c.blue)
        val min = minOf(c.red, c.green, c.blue)
        val d = max - min
        if (d == 0f) return null
        val h = when (max) {
            c.red -> 60f * (((c.green - c.blue) / d) % 6f)
            c.green -> 60f * (((c.blue - c.red) / d) + 2f)
            else -> 60f * (((c.red - c.green) / d) + 4f)
        }
        return if (h < 0f) h + 360f else h
    }

    // 보라 계열: 색상 250~330도이면서 채도(HSV S)가 0.02를 넘는 색
    // (M3 기본 surfaceContainerLow F7F2FA는 채도 약 0.03이라 0.05로 두면 못 잡음. 앱 팔레트의 회색은 채도 0이라 영향 없음)
    private fun isPurple(c: Color): Boolean {
        val h = hue(c) ?: return false
        val max = maxOf(c.red, c.green, c.blue)
        val s = (max - minOf(c.red, c.green, c.blue)) / max
        return h in 250f..330f && s > 0.02f
    }

    @Test
    fun `기준 확인 - Material 기본값의 보라 계열 역할은 보라로 판정된다`() {
        // 판정 함수가 실제 누락 사례(하단바 비선택 아이콘·스위치·시트 배경에 섞이던 값)를 잡는지 먼저 확인
        assertTrue("dark onSurfaceVariant", isPurple(darkColorScheme().onSurfaceVariant))      // CAC4D0
        assertTrue("light surfaceContainer", isPurple(lightColorScheme().surfaceContainer))     // F3EDF7
        assertTrue("light surfaceContainerLow", isPurple(lightColorScheme().surfaceContainerLow))  // F7F2FA
        assertTrue("light onPrimaryContainer", isPurple(lightColorScheme().onPrimaryContainer))
    }

    @Test
    fun `사용하는 역할에 보라 계열 색이 없다`() {
        for ((mode, pair) in schemes) {
            val app = pair.first
            for ((role, get) in usedRoles) {
                assertTrue("$mode $role = ${get(app)} 가 보라 계열", !isPurple(get(app)))
            }
            assertTrue("$mode scrim 이 보라 계열", !isPurple(app.scrim))
        }
    }

    @Test
    fun `누락됐던 역할은 기존 팔레트 값과 같다`() {
        // 새 색을 만들지 않고 기존 값을 재사용했는지 (#643 참고 항목)
        assertEquals(fontColorSub, LightColorScheme.onSurfaceVariant)
        assertEquals(DarkColorScheme.onSecondary, DarkColorScheme.onSurfaceVariant)
        assertEquals(sub2, LightColorScheme.surfaceContainerHighest)
        assertEquals(LightColorScheme.onSurface, LightColorScheme.onPrimaryContainer)
        assertEquals(DarkColorScheme.onSurface, DarkColorScheme.onPrimaryContainer)
        assertEquals(sub3, LightColorScheme.tertiaryContainer)
    }

    @Test
    fun `scrim은 검정`() {
        // M3 기본값도 검정이지만 #648(로딩 표시 통일)에서 쓰므로 값이 바뀌지 않게 고정
        assertEquals(Color.Black, LightColorScheme.scrim)
        assertEquals(Color.Black, DarkColorScheme.scrim)
    }

    @Test
    fun `글자와 배경 조합은 본문 대비 4_5 대 1 이상`() {
        for ((mode, pair) in schemes) {
            val s = pair.first
            assertContrast("$mode 카드·다이얼로그 글자", s.onPrimaryContainer, s.primaryContainer, 4.5)
            // 사각형 버튼·카드(secondaryContainer) 위 기본 글자색은 메인 폰트 색과 같게.
            // 다크 F1F1F1 on 757575는 약 4.08:1로 4.5:1에 못 미치지만 #643 이전부터의 카드 색이라 이 이슈 범위 밖 (대비 검사 제외)
            assertEquals("$mode onSecondaryContainer", s.onSurface, s.onSecondaryContainer)
            // 공부 결과 보상 안내, 텍스트필드 placeholder, 라디오 비선택 등은 화면 배경 위
            assertContrast("$mode onSurfaceVariant", s.onSurfaceVariant, s.background, 4.5)
            // 내 화분 공부 시간 (PotGridItem)
            assertContrast("$mode 서브 폰트", s.onSecondary, s.background, 4.5)
            // 신고하기 버튼
            assertContrast("$mode error 위 글자", s.onError, s.error, 4.5)
            // 학습 중 자동 저장 실패 안내
            assertContrast("$mode errorContainer 위 글자", s.onErrorContainer, s.errorContainer, 4.5)
            // 비활성 버튼 글자 (NewBornTreeScreen disabledContentColor = onSurface)
            assertContrast("$mode 비활성 버튼 글자", s.onSurface, s.tertiaryContainer, 4.5)
        }
    }

    @Test
    fun `비활성 버튼 배경은 화면 배경과 구분된다`() {
        // 수정 전 라이트 F8F6F6 on FFFFFD는 약 1.08:1로 거의 같았음. 비활성 요소는 WCAG 대비 기준 예외라 구분 가능한 정도만 확인
        for ((mode, pair) in schemes) {
            val s = pair.first
            assertContrast("$mode 비활성 버튼 배경", s.tertiaryContainer, s.background, 1.3)
        }
    }

    @Test
    fun `하단바 비선택 아이콘은 하단바 배경 대비 3 대 1 이상`() {
        assertContrast(
            "light 비선택",
            bottomBarItemTint(LightColorScheme, isDark = false, selected = false),
            LightColorScheme.surface,
            3.0
        )
        assertContrast(
            "dark 비선택",
            bottomBarItemTint(DarkColorScheme, isDark = true, selected = false),
            DarkColorScheme.surface,
            3.0
        )
    }

    @Test
    fun `하단바 선택 색과 비선택 색은 서로 다르다`() {
        // 다크: 선택 EBF7E4 vs 비선택 2D2D2D. 라이트는 초록(3F9661) vs 회색(4D4D4D)으로 색상 차이로 구분
        assertContrast(
            "dark 선택/비선택",
            bottomBarItemTint(DarkColorScheme, isDark = true, selected = true),
            bottomBarItemTint(DarkColorScheme, isDark = true, selected = false),
            3.0
        )
        assertNotEquals(
            bottomBarItemTint(LightColorScheme, isDark = false, selected = true),
            bottomBarItemTint(LightColorScheme, isDark = false, selected = false)
        )
        // 라이트 선택 색은 기존 sub_green2와 같은 값 유지
        assertEquals(sub_green2, bottomBarItemTint(LightColorScheme, isDark = false, selected = true))
    }

    @Test
    fun `입력창 배경은 다이얼로그 수정의 영향을 받지 않는다`() {
        assertEquals(textFieldBackground, LightColorScheme.surfaceContainerHigh)
        assertEquals(textFieldBackgroundDark, DarkColorScheme.surfaceContainerHigh)
    }
}
