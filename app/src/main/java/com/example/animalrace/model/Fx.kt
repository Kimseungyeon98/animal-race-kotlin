package com.example.animalrace.model

/**
 * 캐릭터 연출 종류.
 * [SKILL]·[SKILL_ALT]는 자기 스킬(치타 질주/헉헉, 고양이 질주/그루밍, 돼지 먹기/돌진처럼 두 단계인 경우 ALT가 두 번째),
 * [SLIP]·[QUAKE]·[SCARED]는 남에게 당했을 때(바나나·지진·꽥), [BLOCK]은 거북이 방어,
 * [RALLY]는 고유 가속 스킬이 없는 동물의 공통 분발, [TRIP]은 혼자 발이 꼬여 넘어짐,
 * [HIT_CYCLE]은 도감·시트용으로 세 가지 피격 반응을 번갈아 보여 준다.
 */
enum class Fx { NONE, SKILL, SKILL_ALT, SLIP, QUAKE, SCARED, BLOCK, RALLY, TRIP, HIT_CYCLE }
