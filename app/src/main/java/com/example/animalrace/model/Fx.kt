package com.example.animalrace.model

/**
 * 캐릭터 연출 종류.
 * [SKILL]·[SKILL_ALT]는 자기 스킬(치타 질주/헉헉, 고양이 질주/그루밍, 돼지 먹기/돌진처럼 두 단계인 경우 ALT가 두 번째),
 * [SLIP]·[QUAKE]·[SCARED]는 남에게 당했을 때(바나나·지진·꽥), [BLOCK]은 거북이 방어,
 * [RALLY]는 고유 가속 스킬이 없는 동물의 공통 분발, [TRIP]은 혼자 발이 꼬여 넘어짐,
 * [STINK]는 스컹크 방귀를 맞음, [SPIKE]는 고슴도치가 공격을 튕겨냄, [ZEN]은 카피바라가 방해를 무시,
 * [FLAW]는 동물별 딴짓으로 멈춤(햄스터 해바라기씨·판다 대나무·나무늘보 하품),
 * [HIT_CYCLE]은 도감·시트용으로 피격 반응을 번갈아 보여 준다.
 */
enum class Fx { NONE, SKILL, SKILL_ALT, SLIP, QUAKE, SCARED, BLOCK, RALLY, TRIP, STINK, SPIKE, ZEN, FLAW, HIT_CYCLE }
