package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

fun defaultConfettiParties(): List<Party> = listOf(
    Party(
        speed = 0f,
        maxSpeed = 20f, //퍼지는 속도. 클수록 빠르게 큰 범위로 퍼짐
        damping = 0.95f, //감속 정도. 작을수록 빨리 느려짐
        spread = 360, //발사 각도 범위
        timeToLive = 15000L, //화면에 살아있는 시간
        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100), //컨페티 개수
        position = Position.Relative(0.5, 0.5) //위치, (0.5,0.5) 정중앙
    )
)

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    parties: List<Party> = remember { defaultConfettiParties() },
    onFinished: () -> Unit = {},
) {
    KonfettiView(
        modifier = modifier.fillMaxSize(),
        parties = parties,
        updateListener = object : OnParticleSystemUpdateListener {
            override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                if (activeSystems == 0) onFinished()
            }
        }
    )
}
