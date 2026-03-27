package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import androidx.activity.SystemBarStyle.Companion.dark
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.NicknameRepository
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** �곗씠�곕쿋�댁뒪�먯꽌 媛믪쓣 諛쏆븘���� �섎뒗 寃쎌슦
_蹂��섎챸 : �몃��먯꽌 媛믪쓣 紐� 嫄대뱾�닿쾶 �섍린 �꾪빐 private�쇰줈 �좎뼵
蹂��섎챸 : �몃��먯꽌 �쎈뒗 �곗씠��.
_蹂��섎챸�� 諛붾�뚮㈃ �먮룞�쇰줈 媛믪씠 �낅뜲�댄듃媛� �섍쾶 �섍린 �꾪빐 .asStaeFlow() 遺숈씠湲�
 */
data class MyPageUiState(
    val nickname: String = "",
    val profileImg: String = "Lv.1",
    val isUpdateSuccess: Boolean = false,
    val levelList: List<String> = emptyList(), // �꾨줈�� �몄쭛 - �붾텇 �대�吏� �꾩슦湲� �꾪빐 �곗씠�� �덈꺼 由ъ뒪��
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val nicknameError: String? = null,
    val totalStudyTime: String = "0�쒓컙 0遺�",
    val completedPotCount: Int = 0,
)

sealed class MyPageEvent {
    object SuccessUpdate : MyPageEvent()
}


class MyPageViewModel(
    private val userRepository: UserRepository,
    private val potRepository: PotRepository,
    private val nicknameRepository: NicknameRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            CurrentUser.uid = "cf2MtNfq0lN5b0agyNSVqeoKuDc2"
            // �꾩옱 濡쒓렇�몃맂 �좎� ID
            //private val currentUid: String get() = CurrentUser.uid
            // �뚯뒪�몄슜 UID
//            private val currentUid: String = "cf2MtNfq0lN5b0agyNSVqeoKuDc2"

//            CurrentUser.uid = "RVmMPR05kVYeLyWYknUbGdmDnGG2"
            userRepository.getUserProfile(CurrentUser.uid).collectLatest { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            nickname = profile.nickname ?: "�대쫫�놁쓬",
                            profileImg = profile.profileImg ?: "Lv.1",
                            isDarkMode = profile.isDarkMode ?: true,
                            totalStudyTime = formatToDigitalClock(profile.totalStudyTime ?: 0L)
                        )
                    }
                    getCompletedPotCount()

                } else {
                    Log.e("error", "-----------�ъ슜�� �뺣낫 �놁쓬")
                }
            }
        }
    }

    // �ъ슜�먯쓽 �꾨즺 �붾텇 媛쒖닔 援ы빐 _uiState.completedPotCount �� �ｊ린
    fun getCompletedPotCount() {
        viewModelScope.launch {
            try {
                val myPotList = userRepository.getUsersPots(CurrentUser.uid)
                _uiState.update { it ->
                    it.copy(
                        completedPotCount = myPotList.count { it.isCompleted }
                    )
                }
            } catch (e: Exception) {
                Log.e("error", e.message.toString())
            }
        }
    }

    // 蹂댁쑀�� �덈꺼 以묐났 �쒓굅 �덈꺼 由ъ뒪�� 媛��몄삤湲�
    fun getImageLevelList() {
        viewModelScope.launch {
            val result = potRepository.getDuplicationLevelList(CurrentUser.uid)
            _uiState.update { it.copy(levelList = result) }
        }
    }

    // �됰꽕�� 寃��ъ슜 2~10湲��� �덉슜
    private fun checkNicknameValidation(text: String): String? {
        val len = text.length
        return if (len !in 2..10) {
            "�됰꽕�꾩� 2�� �댁긽 10�� �댄븯濡� �낅젰�댁＜�몄슂"
        } else {
            null
        }
    }

    // 寃��촲, 異붽�o, ��젣, �낅뜲�댄듃?
    // 寃��촲, 異붽�o, �낅뜲�댄듃, ��젣?
    fun updateProfile(nickname: String, imageLevel: String) {
        val validationResult = checkNicknameValidation(nickname)
        if (validationResult != null) {
            _uiState.update {
                it.copy(
                    isUpdateSuccess = false,
                    nicknameError = validationResult
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                val currentNickname = _uiState.value.nickname
                // �됰꽕�� 媛숈쑝硫� �꾨줈�� �ъ쭊留� 蹂�寃쏀븯�ㅻ뒗 �섎룄濡� �먮떒
                if (nickname != currentNickname) {
                    // �됰꽕�� 以묐났 寃���
                    if (nicknameRepository.isNicknameTaken(nickname)) {
                        _uiState.update {
                            it.copy(
                                isUpdateSuccess = false,
                                nicknameError = "�대� �ъ슜以묒씤 �됰꽕�꾩엯�덈떎"
                            )
                        }
                        return@launch
                    }
                    nicknameRepository.registerNickname(nickname)
                    nicknameRepository.deleteNickname(currentNickname)
                }

                userRepository.updateNicknameAndImage(
                    CurrentUser.uid,
                    nickname,
                    imageLevel
                )
                _uiState.update {
                    it.copy(
                        nickname = nickname,
                        profileImg = imageLevel,
                        isUpdateSuccess = true,
                        nicknameError = null
                    )
                }
            } catch (e: Exception) {
                Log.e("error", e.message.toString())
                _uiState.update { it.copy(isUpdateSuccess = false) }
            }
        }
    }

    fun resetIsUpdateSuccess() {
        _uiState.update { it.copy(isUpdateSuccess = false) }
    }


    fun toggleDarkMode() {
        val state = !uiState.value.isDarkMode
        viewModelScope.launch {
            try {
                Log.d("plantLog", "----------3")
                userRepository.updateIsDarkMode(
                    uid = CurrentUser.uid,
                    state = state
                )
                _uiState.update { it.copy(isDarkMode = state) }

            } catch (e: Exception) {
                Log.e("error", e.message.toString())
            }
        }
    }


    //�곗씠�곕쿋�댁뒪�먯꽌 媛믪쓣 �� 媛��몄��� �섎뒗 寃쎌슦
    fun getTag() = "�먭꺽利�"
}