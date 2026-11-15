package com.example.animalrace.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animalrace.ui.ads.LocalAds
import com.example.animalrace.ui.components.Icons2
import com.example.animalrace.ui.components.InkIcon
import com.example.animalrace.ui.theme.Pal

/** 개인정보처리방침 주소 (스토어 등록정보와 같은 곳) */
const val PRIVACY_URL = "https://github.com/Kimseungyeon98/animal-race-kotlin/blob/main/docs/privacy.md"

/** 앱 버전 이름 (예: "6.1"). 안드로이드에서만 채워 주고, 비어 있으면 정보 창에 버전 줄을 그리지 않는다 */
val LocalAppVersion = staticCompositionLocalOf { "" }

/**
 * 정보 창 (6.1, 캔버스 "다음 업데이트 후보" 3): 메인 제목 아래 ⓘ 버튼으로 연다.
 * 공정성 안내 · 개인정보처리방침 · 광고 개인정보 설정(동의를 다시 고를 수 있어야 하는 지역에서만).
 */
@Composable
internal fun InfoSheet(onClose: () -> Unit) {
    val ads = LocalAds.current
    val uri = LocalUriHandler.current
    val version = LocalAppVersion.current
    Sheet(onClose, fill = false) {
        Text("정보", fontSize = 22.sp, color = Pal.Ink)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Pal.Mint)
                .border(2.5.dp, Pal.Ink, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InkIcon(Icons2.Scale, size = 26.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("완전 공정해요", fontSize = 16.sp, color = Pal.Ink)
                Text(
                    "순위는 출발하는 순간 무작위로 정해져요.\n동물·이름·광고는 결과에 영향이 없어요.",
                    fontSize = 12.5.sp, lineHeight = 16.sp, color = Pal.InkSoft,
                )
            }
        }
        // 브라우저가 없는 기기에서도 앱이 죽지 않게
        InfoRow("개인정보처리방침", "브라우저로 열려요") { runCatching { uri.openUri(PRIVACY_URL) } }
        if (ads.privacyOptionsRequired) {
            InfoRow("광고 개인정보 설정", "동의 창을 다시 열어요") { ads.showPrivacyOptions() }
        }
        if (version.isNotBlank()) {
            Text("동물 레이스 $version", fontSize = 12.sp, color = Pal.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InfoRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Pal.Paper)
            .border(2.5.dp, Pal.Ink, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 17.sp, color = Pal.Ink)
            Text(sub, fontSize = 12.sp, color = Pal.InkSoft)
        }
        InkIcon(Icons2.Right, size = 18.dp)
    }
}
