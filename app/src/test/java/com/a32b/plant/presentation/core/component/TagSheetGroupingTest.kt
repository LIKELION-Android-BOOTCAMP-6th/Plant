package com.a32b.plant.presentation.core.component

import com.a32b.plant.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 태그 시트의 상위 분류 정렬 확인 (#650).
 * 예전에는 순서 목록에 없는 분류가 indexOf = -1이 되어 맨 앞에 붙었다.
 */
class TagSheetGroupingTest {

    private fun tag(id: String, parent: String, no: Int) = Tag(id = id, name = id, parentId = parent, no = no)

    private fun List<Pair<String, List<Tag>>>.parents() = map { it.first }

    @Test
    fun `운영 Tags와 같은 순서로 받으면 중등 고등 대학교 취업준비 자기계발 순서다`() {
        // 2026-11-24 운영 Tags를 orderBy("no")로 받은 순서: no가 분류마다 0부터라 분류가 섞여 온다
        val tags = listOf(
            tag("high_korean", "고등", 0),
            tag("job_resume", "취업준비", 0),
            tag("mid_korean", "중등", 0),
            tag("self_certification", "자기계발", 0),
            tag("univ_humanities", "대학교", 0),
            tag("high_english", "고등", 1),
            tag("job_interview", "취업준비", 1),
            tag("mid_english", "중등", 1),
        )

        assertEquals(listOf("중등", "고등", "대학교", "취업준비", "자기계발"), groupTagsByParent(tags).parents())
    }

    @Test
    fun `순서 목록에 없는 새 분류는 맨 앞이 아니라 맨 뒤에 온다`() {
        val tags = listOf(
            tag("new1", "새 분류", 0),
            tag("mid_korean", "중등", 0),
            tag("self_certification", "자기계발", 0),
        )

        assertEquals(listOf("중등", "자기계발", "새 분류"), groupTagsByParent(tags).parents())
    }

    @Test
    fun `순서 목록에 없는 분류가 여럿이면 받은 목록에 먼저 나온 분류가 앞에 온다`() {
        val tags = listOf(
            tag("b1", "새 분류B", 0),
            tag("mid_korean", "중등", 0),
            tag("a1", "새 분류A", 0),
        )

        assertEquals(listOf("중등", "새 분류B", "새 분류A"), groupTagsByParent(tags).parents())
    }

    @Test
    fun `분류 안 태그 순서는 받은 목록 순서를 그대로 둔다`() {
        val tags = listOf(
            tag("mid_korean", "중등", 0),
            tag("high_korean", "고등", 0),
            tag("mid_english", "중등", 1),
            tag("mid_math", "중등", 2),
        )

        val middle = groupTagsByParent(tags).first { it.first == "중등" }.second
        assertEquals(listOf("mid_korean", "mid_english", "mid_math"), middle.map { it.id })
    }

    @Test
    fun `빈 목록이면 분류도 없다`() {
        assertEquals(emptyList<Pair<String, List<Tag>>>(), groupTagsByParent(emptyList()))
    }
}
