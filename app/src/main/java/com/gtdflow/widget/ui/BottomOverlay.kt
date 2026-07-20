package com.gtdflow.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Нижний оверлей поверх домашнего экрана (Trello-паттерн): затемняющий скрим на весь
 * экран (тап по нему — закрыть), карточка-лист прижата к низу, поднимается над
 * клавиатурой (imePadding). Прозрачный фон окна даёт этому оверлею «плавать» над
 * лаунчером — включается translucent-темой Activity + WindowCompat.setDecorFitsSystemWindows(false).
 *
 * Тап внутри карточки НЕ закрывает оверлей: карточка гасит клики пустого места
 * (no-op clickable без ряби), поэтому доходят только клики по полям/кнопкам.
 */
@Composable
fun BottomOverlay(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val noRipple = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize()) {
        // Скрим на весь экран: тап закрывает оверлей.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = noRipple, indication = null) { onDismiss() }
                .background(Color.Black.copy(alpha = 0.35f)),
        )
        // Карточка-лист у нижнего края, над клавиатурой и системной навигацией.
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding()
                // гасим клики по пустому месту карточки (иначе провалятся на скрим)
                .clickable(interactionSource = noRipple, indication = null) {},
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                content = content,
            )
        }
    }
}
