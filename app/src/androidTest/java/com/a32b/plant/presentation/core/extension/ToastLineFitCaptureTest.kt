package com.a32b.plant.presentation.core.extension

import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.type.StudySaveRejection
import com.a32b.plant.presentation.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 긴 토스트 문구를 실제 시스템 토스트로 띄워 화면을 캡처한다 (#646).
 *
 * Android 12부터 텍스트 토스트는 두 줄까지만 보이고 나머지는 잘린다.
 * 잘렸는지는 코드로 알 수 없어서(토스트는 SystemUI가 그림) 캡처를 눈으로 확인한다.
 * - 캡처 위치: /sdcard/Android/data/com.a32b.plant/files/toast_check/NN.png
 * - 00번은 대조군: Android 13 실기기에서 잘렸던 #531의 예전 출석 문구. 이게 잘려 보여야 캡처가 유효하다
 * - 화면이 켜져 있어야 캡처된다 (`adb shell input keyevent KEYCODE_WAKEUP`)
 */
@RunWith(AndroidJUnit4::class)
class ToastLineFitCaptureTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private val messages = listOf(
        // 대조군 (#531, 80029a18 이전 문구)
        "인터넷 연결이 원활하지 않아 출석 결과를 받지 못했어요. 출석판을 확인한 뒤 다시 시도해주세요.",
        // StudyingRepositoryImpl — 사유를 찾지 못한 저장 거부 (UNRESOLVED_REJECTION_MESSAGE)
        "저장하지 못했어요. 기기 시간이 빠르면 잠시 후 다시 저장해 주세요.",
        // LevelUpPotUseCase — Lv.5에서 재료 5종이 모두 부족할 때 (가장 긴 경우, LevelUpCondition 순서)
        "영양제, 비료, 물, 햇빛, 정성이(가) 부족해요.",
        // SignInScreen
        "기기에 등록된 Google 계정이 없어요. 설정에서 계정을 추가해주세요.",
        // StudyingRepositoryImpl — 로컬 백업 실패
        "저장 공간이 부족하여 비정상 종료 시 데이터를 소실할 수 있어요.",
        AppError.Network().message,
        AppError.UnknownUser().message,
        AppError.Auth().message,
        // PotRepositoryImpl
        "레벨업에 실패했어요. 아이템과 학습 시간을 확인해주세요.",
        // UserRepositoryImpl
        "프로필 저장에 실패했어요. 잠시 후 다시 시도해주세요.",
        // AuthRepositoryImpl
        "탈퇴 처리 중 문제가 발생했어요. 다시 시도해주세요.",
        NETWORK_SLOW_MESSAGE,
        StudySaveRejection.POT_COMPLETED.message,
        StudySaveRejection.OVERLAPPED.message,
        // StudyingViewModel
        "저장되지 않은 학습 기록이 있어 홈으로 이동해요.",
        // CommunityRepositoryImpl — 줄바꿈 포함
        "오류가 발생했어요.\n잠시 후 다시 시도해주세요.",
        // SignInScreen — 줄바꿈 포함
        "구글 로그인에 실패했어요.\n다시 시도해주세요.",
        // DeleteActivitiesUseCase — 개수가 큰 경우
        "100개 중 100개 삭제에 실패했어요.",
        // SignInViewModel — 닉네임 최대 10자
        "가나다라마바사아자차님 환영해요.",
    )

    @Test
    fun captureLongToasts() {
        val dir = File(context.getExternalFilesDir(null), "toast_check").apply {
            deleteRecursively()
            mkdirs()
        }
        // 앱이 백그라운드면 토스트가 보이지 않으므로(Android 13 실기기에서 확인) 앱 화면을 앞에 띄운다
        ActivityScenario.launch(MainActivity::class.java).use { captureAll(dir) }
    }

    private fun captureAll(dir: File) {
        SystemClock.sleep(LAUNCH_WAIT_MS)
        messages.forEachIndexed { index, message ->
            var toast: Toast? = null
            instrumentation.runOnMainSync {
                toast = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
            }
            // 토스트가 나타나는 애니메이션이 끝날 때까지 기다린다
            SystemClock.sleep(SHOW_WAIT_MS)
            val screen: Bitmap = instrumentation.uiAutomation.takeScreenshot()
            File(dir, "%02d.png".format(index)).outputStream().use {
                screen.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            instrumentation.runOnMainSync { toast?.cancel() }
            // 취소 후 다음 토스트가 바로 뜨도록 사라지는 시간을 둔다
            SystemClock.sleep(HIDE_WAIT_MS)
        }
    }

    private companion object {
        const val LAUNCH_WAIT_MS = 3_000L
        const val SHOW_WAIT_MS = 1_000L
        const val HIDE_WAIT_MS = 700L
    }
}
