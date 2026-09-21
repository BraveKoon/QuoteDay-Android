package com.quoteday.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.ui.ClayChip
import com.quoteday.app.ui.ClayEmptyState
import com.quoteday.app.ui.ClayRadius
import com.quoteday.app.ui.ClayScreenHeader
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.app.ui.QuoteCard
import com.quoteday.app.ui.TAB_BAR_INSET
import com.quoteday.core.AppCategory

/** 명언 전체를 훑어보고 찾는 화면. */
@Composable
fun QuoteBrowserScreen(
    app: QuoteDayApplication,
    onOpenQuote: (String) -> Unit,
) {
    val colors = ClayTheme.colors
    var term by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<AppCategory?>(null) }

    val results = remember(term, category) {
        val base = if (term.isBlank()) app.library.quotes else app.library.search(term)
        val filtered = category?.let { wanted -> base.filter { it.matches(wanted) } } ?: base
        filtered.map { app.library.presentation(it) }
    }

    // 목록에 보이는 것만 하트 수를 받아 온다. 201편을 한 번에 물어볼 이유가 없다.
    LaunchedEffect(results) {
        app.hearts.refresh(results.take(30).map { it.quote.slug })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = ClaySpacing.m,
            end = ClaySpacing.m,
            top = ClaySpacing.m,
            bottom = TAB_BAR_INSET,
        ),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.s),
    ) {
        item {
            ClayScreenHeader(
                title = "명언",
                subtitle = "${results.size}편",
                modifier = Modifier.padding(bottom = ClaySpacing.xs),
            )
        }

        item {
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(ClayRadius.control),
                placeholder = { Text("문장, 인물로 찾기", color = colors.textSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textSecondary) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.separator,
                    cursorColor = colors.accent,
                ),
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
            ) {
                ClayChip(
                    text = "전체",
                    tint = if (category == null) colors.accent else colors.surfaceRaised,
                    textColor = if (category == null) colors.textOnAccent else colors.textSecondary,
                    onClick = { category = null },
                )
                for (item in AppCategory.selectableForQuotes) {
                    val isOn = category == item
                    ClayChip(
                        text = item.displayName,
                        tint = if (isOn) colors.tint(item) else colors.surfaceRaised,
                        textColor = if (isOn) colors.textOnTint else colors.textSecondary,
                        onClick = { category = if (isOn) null else item },
                    )
                }
            }
        }

        if (results.isEmpty()) {
            item {
                ClayEmptyState(
                    message = "찾는 명언이 없습니다",
                    detail = "다른 낱말로 찾아보거나 카테고리를 풀어 보세요.",
                    modifier = Modifier.padding(top = ClaySpacing.m),
                )
            }
        }

        items(results, key = { it.quote.slug }) { item ->
            QuoteCard(
                presentation = item,
                heartCount = app.hearts.count(item.quote.slug),
                isHearted = app.hearts.isHearted(item.quote.slug),
                onHeart = { app.hearts.toggle(item.quote.slug) },
                onClick = { onOpenQuote(item.quote.slug) },
            )
        }
    }
}
