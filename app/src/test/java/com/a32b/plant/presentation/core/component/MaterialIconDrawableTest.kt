package com.a32b.plant.presentation.core.component

import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * material-icons 라이브러리를 빼면서 res/drawable로 옮긴 아이콘 7종이 원래 아이콘과 같은 모양인지 확인 (#651).
 *
 * 기대값은 material-icons-core/extended 1.7.8 소스의 materialIcon { materialPath { ... } } 명령을 그대로 옮긴 것.
 * 원래 아이콘의 공통 속성(materialIcon·materialPath 기본값): 24dp, 뷰포트 24, 검은색 채우기, NonZero 채우기 규칙.
 * 실제 그릴 때 색은 Icon의 tint가 덮어쓰므로 채우기 색은 검은색이기만 하면 된다.
 */
class MaterialIconDrawableTest {

    private fun drawable(name: String): String =
        File("src/main/res/drawable/$name.xml").readText()

    private fun attr(xml: String, name: String): String =
        Regex("""android:$name="([^"]*)"""").find(xml)?.groupValues?.get(1)
            ?: error("android:$name 속성이 없음")

    // 원래 아이콘과 같은 크기·뷰포트·채우기인지, 경로가 기대 명령과 같은지 확인
    private fun assertSameAsMaterialIcon(name: String, expected: PathBuilder.() -> Unit) {
        val xml = drawable(name)
        assertEquals("$name width", "24dp", attr(xml, "width"))
        assertEquals("$name height", "24dp", attr(xml, "height"))
        assertEquals("$name viewportWidth", "24", attr(xml, "viewportWidth"))
        assertEquals("$name viewportHeight", "24", attr(xml, "viewportHeight"))
        assertEquals("$name fillColor", "#FF000000", attr(xml, "fillColor"))
        // fillType이 없으면 기본값 nonZero → 원래 아이콘의 DefaultFillType(NonZero)과 같음
        assertFalse("$name 은 fillType을 따로 지정하지 않아야 함", xml.contains("android:fillType"))
        // path가 하나뿐이어야 원래 아이콘(materialPath 1개)과 같음
        assertEquals("$name path 개수", 1, Regex("<path").findAll(xml).count())

        val actual: List<PathNode> = addPathNodes(attr(xml, "pathData"))
        val expectedNodes: List<PathNode> = PathBuilder().apply(expected).nodes
        assertEquals("$name 경로", expectedNodes, actual)
    }

    @Test
    fun `ic_add는 Filled Add와 같다`() = assertSameAsMaterialIcon("ic_add") {
        moveTo(19.0f, 13.0f)
        horizontalLineToRelative(-6.0f)
        verticalLineToRelative(6.0f)
        horizontalLineToRelative(-2.0f)
        verticalLineToRelative(-6.0f)
        horizontalLineTo(5.0f)
        verticalLineToRelative(-2.0f)
        horizontalLineToRelative(6.0f)
        verticalLineTo(5.0f)
        horizontalLineToRelative(2.0f)
        verticalLineToRelative(6.0f)
        horizontalLineToRelative(6.0f)
        verticalLineToRelative(2.0f)
        close()
    }

    @Test
    fun `ic_check는 Filled Check와 같다`() = assertSameAsMaterialIcon("ic_check") {
        moveTo(9.0f, 16.17f)
        lineTo(4.83f, 12.0f)
        lineToRelative(-1.42f, 1.41f)
        lineTo(9.0f, 19.0f)
        lineTo(21.0f, 7.0f)
        lineToRelative(-1.41f, -1.41f)
        close()
    }

    @Test
    fun `ic_close는 Filled Close와 같다`() = assertSameAsMaterialIcon("ic_close") {
        moveTo(19.0f, 6.41f)
        lineTo(17.59f, 5.0f)
        lineTo(12.0f, 10.59f)
        lineTo(6.41f, 5.0f)
        lineTo(5.0f, 6.41f)
        lineTo(10.59f, 12.0f)
        lineTo(5.0f, 17.59f)
        lineTo(6.41f, 19.0f)
        lineTo(12.0f, 13.41f)
        lineTo(17.59f, 19.0f)
        lineTo(19.0f, 17.59f)
        lineTo(13.41f, 12.0f)
        close()
    }

    @Test
    fun `ic_arrow_back은 Filled ArrowBack과 같다`() = assertSameAsMaterialIcon("ic_arrow_back") {
        moveTo(20.0f, 11.0f)
        horizontalLineTo(7.83f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12.0f, 4.0f)
        lineToRelative(-8.0f, 8.0f)
        lineToRelative(8.0f, 8.0f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13.0f)
        horizontalLineTo(20.0f)
        verticalLineToRelative(-2.0f)
        close()
    }

    @Test
    fun `ic_arrow_back은 원래 아이콘처럼 RTL에서 뒤집히지 않는다`() {
        // 교체 전 Icons.Default.ArrowBack은 AutoMirrored가 아닌 Filled 버전이라 autoMirror = false
        assertFalse(drawable("ic_arrow_back").contains("android:autoMirrored"))
    }

    @Test
    fun `ic_arrow_drop_down은 Filled ArrowDropDown과 같다`() = assertSameAsMaterialIcon("ic_arrow_drop_down") {
        moveTo(7.0f, 10.0f)
        lineToRelative(5.0f, 5.0f)
        lineToRelative(5.0f, -5.0f)
        close()
    }

    @Test
    fun `ic_circle_filled는 Filled Circle과 같다`() = assertSameAsMaterialIcon("ic_circle_filled") {
        moveTo(12.0f, 2.0f)
        curveTo(6.47f, 2.0f, 2.0f, 6.47f, 2.0f, 12.0f)
        reflectiveCurveToRelative(4.47f, 10.0f, 10.0f, 10.0f)
        reflectiveCurveToRelative(10.0f, -4.47f, 10.0f, -10.0f)
        reflectiveCurveTo(17.53f, 2.0f, 12.0f, 2.0f)
        close()
    }

    @Test
    fun `ic_radio_button_unchecked는 Outlined RadioButtonUnchecked와 같다`() =
        assertSameAsMaterialIcon("ic_radio_button_unchecked") {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
            close()
        }
}
