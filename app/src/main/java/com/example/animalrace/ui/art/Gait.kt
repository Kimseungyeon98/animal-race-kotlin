package com.example.animalrace.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.animalrace.model.Animal
import com.example.animalrace.model.Animal.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * 동물별 달리기 걸음 (4.11, 시안 "달리기 모션"). 모두 같던 다리 흔들기 대신 다섯 가지 걸음으로 나눈다.
 * 화면 표시만 바뀌고 경주 결과·확률과는 무관하다.
 */
internal enum class Gait(
    /** 한 걸음(한 주기) 길이(초), 보통 빠르기일 때 */
    val period: Float,
) {
    /** 질주: 다리를 크게 뻗고 모으며 몸이 앞뒤로 출렁 */
    GALLOP(0.30f),
    /** 깡충: 두 발을 모아 크게 뛰고 착지 때 납작 */
    HOP(0.60f),
    /** 뒤뚱: 짧은 보폭에 몸을 앞뒤로 기우뚱 */
    WADDLE(0.40f),
    /** 종종: 빠르고 짧은 발 구르기, 앞으로 숙임 */
    SCURRY(0.20f),
    /** 느릿: 크고 느린 걸음 */
    PLOD(0.60f),
}

internal fun gaitOf(animal: Animal): Gait = when (animal) {
    HORSE, CHEETAH, DOG, CAT -> Gait.GALLOP
    RABBIT, KANGAROO -> Gait.HOP
    PENGUIN, DUCK, CHICKEN -> Gait.WADDLE
    PIG, HAMSTER, HEDGEHOG, SKUNK, MONKEY -> Gait.SCURRY
    TURTLE, SLOTH, SNAIL, ELEPHANT, PANDA, CAPYBARA -> Gait.PLOD
}

/** 빠르기([pace], 1 = 보통, 스퍼트 ≈ 2, 멈춤 0)에 따라 다리 박자를 빠르게/느리게. 너무 극단적이지 않게 눌러 준다 */
internal fun gaitRate(pace: Float): Float = if (pace <= 0f) 0f else (0.45f + 0.55f * pace).coerceIn(0.55f, 1.7f)

/** 몸통 눌림·늘어남과 귀·갈기·꼬리 흔들림의 크기 (4.13, 시안 "크게" = 1.8배) */
private const val FOLLOW_AMP = 1.8f

/** [phase]는 걸음 수(1 = 한 주기). 소수 부분만 쓴다 */
internal fun gaitPose(gait: Gait, phase: Float): Pose {
    val pose = basePose(gait, phase)
    val p = phase - kotlin.math.floor(phase)
    val w = 2f * PI.toFloat() * p
    // 눌림·늘어남: 뜰수록(bob이 클수록) 세로로 살짝 길게, 땅에 닿는 순간 납작하게
    val air = (-pose.bob / 8f).coerceIn(0f, 1f)
    val (land, sq) = when (gait) {
        Gait.GALLOP -> (1f - air) to 0.07f
        Gait.HOP -> (if (p >= 0.6f) sin(PI.toFloat() * (p - 0.6f) / 0.4f) else 0f) to 0.12f
        Gait.WADDLE -> (1f - air) to 0.05f
        Gait.SCURRY -> (1f - air) to 0.05f
        Gait.PLOD -> (1f - air) to 0.04f
    }
    val q = sq * FOLLOW_AMP
    val sy = 1f - q * land + q * 0.6f * air
    val sx = 1f + q * 0.7f * land - q * 0.3f * air
    // 따라오는 움직임: 몸 출렁임보다 한 박자 늦게
    val sway = when (gait) {
        Gait.GALLOP -> 14f * sin(w - 1.3f)
        Gait.HOP -> if (p < 0.6f) -14f * sin(PI.toFloat() * p / 0.6f) else 16f * sin(PI.toFloat() * (p - 0.6f) / 0.4f)
        Gait.WADDLE -> 8f * sin(w - 1f)
        Gait.SCURRY -> 7f * sin(w - 1f)
        Gait.PLOD -> 6f * sin(w - 1.2f)
    }
    return pose.copy(sx = sx, sy = sy, sway = sway * FOLLOW_AMP)
}

private fun basePose(gait: Gait, phase: Float): Pose {
    val p = phase - kotlin.math.floor(phase)
    val w = 2f * PI.toFloat() * p
    return when (gait) {
        Gait.GALLOP -> Pose(
            legA = 34f * sin(w), legB = 34f * sin(w + 1.7f),
            bob = -7f * max(0f, sin(w + 0.4f)),
            wing = -12f * sin(w),
            tilt = -6f * sin(w + 0.9f),
            sx = 1f + 0.03f * sin(2 * w), sy = 1f - 0.03f * sin(2 * w),
        )
        Gait.HOP -> {
            // 0 ~ 0.6: 공중 (다리 뒤로 쭉), 0.6 ~ 1: 땅 (다리 앞으로 모으고 납작)
            if (p < 0.6f) {
                val a = p / 0.6f
                Pose(-30f + 8f * a, -30f + 8f * a, -20f * sin(PI.toFloat() * a), -16f, tilt = -8f + 14f * a)
            } else {
                val g = (p - 0.6f) / 0.4f
                val squash = sin(PI.toFloat() * g)
                Pose(18f - 40f * g, 18f - 40f * g, 0f, 6f, tilt = 6f - 14f * g, sx = 1f + 0.08f * squash, sy = 1f - 0.1f * squash)
            }
        }
        Gait.WADDLE -> Pose(
            legA = 16f * sin(w), legB = -16f * sin(w),
            bob = -3f * abs(sin(w)),
            wing = -18f * abs(sin(w)),
            tilt = 7f * sin(w),
        )
        Gait.SCURRY -> Pose(
            legA = 22f * sin(w), legB = -22f * sin(w),
            bob = -2.5f * abs(sin(w)),
            wing = -10f * sin(w),
            tilt = 4f,
        )
        Gait.PLOD -> Pose(
            legA = 22f * sin(w), legB = -22f * sin(w),
            bob = -2f * abs(sin(w)),
            wing = 6f * sin(w),
            tilt = 1.5f * sin(w),
            sx = 1f + 0.02f * abs(sin(w)), sy = 1f - 0.025f * abs(sin(w)),
        )
    }
}

/** 기울기·눌림까지 넣어 그린다 (200×170 좌표). 기울기는 발 근처, 눌림은 땅을 기준으로 */
internal fun DrawScope.drawPosed(animal: Animal, pose: Pose) {
    if (pose.tilt == 0f && pose.sx == 1f && pose.sy == 1f) { drawAnimal(animal, pose); return }
    withTransform({
        rotate(pose.tilt, Offset(100f, 148f))
        scale(pose.sx, pose.sy, Offset(100f, GROUND_Y))
    }) { drawAnimal(animal, pose) }
}

/** 걸음 [phase](0~1)의 자세를 바로 계산 (검증 화면용) */
internal fun gaitAt(animal: Animal, phase: Float): Pose = gaitPose(gaitOf(animal), phase)

/** 바깥(검증 화면)에서 기울기·눌림까지 넣어 그릴 때 */
internal object GaitDraw {
    fun DrawScope.draw(a: Animal, pose: Pose) = drawPosed(a, pose)
}
