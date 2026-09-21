package com.quoteday.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.ui.ClayChip
import com.quoteday.app.ui.ClayRadius
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.core.AppCategory
import com.quoteday.core.RecurrenceFrequency
import com.quoteday.core.RecurrenceRule
import com.quoteday.core.ScheduleValidator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 일정 편집기.
 *
 * 날짜·시각 고르기는 안드로이드 기본 대화상자를 쓴다. Compose 쪽 피커는 아직
 * 실험 API 라서, 이 저장소처럼 **화면을 눈으로 확인할 수 없는** 상황에서는
 * 검증된 것을 쓰는 편이 낫다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    app: QuoteDayApplication,
    target: ScheduleEditorTarget,
    onDismiss: () -> Unit,
) {
    val colors = ClayTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val existing = (target as? ScheduleEditorTarget.Edit)?.schedule
    val initialStart = existing?.start
        ?: (target as ScheduleEditorTarget.New).date.atTime(DEFAULT_HOUR, 0)

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var memo by remember { mutableStateOf(existing?.memo.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: AppCategory.DAILY) }
    var start by remember { mutableStateOf(initialStart) }
    var durationMinutes by remember {
        mutableStateOf(existing?.duration?.toMinutes()?.toInt() ?: DEFAULT_DURATION_MINUTES)
    }
    var frequency by remember {
        mutableStateOf(existing?.recurrence?.frequency ?: RecurrenceFrequency.NONE)
    }
    var notify by remember { mutableStateOf(existing?.isQuoteNotificationEnabled ?: true) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ClaySpacing.m)
                .padding(bottom = ClaySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.m),
        ) {
            Text(
                if (existing == null) "새 일정" else "일정 고치기",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("제목") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = editorFieldColors(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                Text("카테고리", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                ) {
                    for (item in AppCategory.entries) {
                        val isOn = item == category
                        ClayChip(
                            text = item.displayName,
                            tint = if (isOn) colors.tint(item) else colors.surfaceRaised,
                            textColor = if (isOn) colors.textOnTint else colors.textSecondary,
                            onClick = { category = item },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                Text("시작", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                    ClayChip(
                        text = "${start.year}. ${start.monthValue}. ${start.dayOfMonth}",
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    start = LocalDateTime.of(
                                        LocalDate.of(year, month + 1, day),
                                        start.toLocalTime(),
                                    )
                                },
                                start.year,
                                start.monthValue - 1,
                                start.dayOfMonth,
                            ).show()
                        },
                    )
                    ClayChip(
                        text = start.format(timeFormatter),
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    start = LocalDateTime.of(
                                        start.toLocalDate(),
                                        LocalTime.of(hour, minute),
                                    )
                                },
                                start.hour,
                                start.minute,
                                false,
                            ).show()
                        },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                Text("길이", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                ) {
                    for (minutes in DURATION_CHOICES) {
                        val isOn = minutes == durationMinutes
                        ClayChip(
                            text = durationLabel(minutes),
                            tint = if (isOn) colors.accent else colors.surfaceRaised,
                            textColor = if (isOn) colors.textOnAccent else colors.textSecondary,
                            onClick = { durationMinutes = minutes },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                Text("반복", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                ) {
                    for (item in RecurrenceFrequency.entries) {
                        val isOn = item == frequency
                        ClayChip(
                            text = item.shortTitle,
                            tint = if (isOn) colors.accent else colors.surfaceRaised,
                            textColor = if (isOn) colors.textOnAccent else colors.textSecondary,
                            onClick = { frequency = item },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("메모") },
                minLines = 2,
                colors = editorFieldColors(),
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("알림", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        "시작할 때 그 일정에 어울리는 명언을 보내 드립니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                Switch(
                    checked = notify,
                    onCheckedChange = { notify = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.textOnAccent,
                        checkedTrackColor = colors.accent,
                    ),
                )
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.danger)
            }

            Button(
                onClick = {
                    val end = start.plusMinutes(durationMinutes.toLong())
                    val recurrence = RecurrenceRule(frequency)
                    val failure = ScheduleValidator.validate(title, start, end, recurrence)
                    if (failure != null) {
                        error = failure.message
                        return@Button
                    }
                    if (existing == null) {
                        app.schedules.add(
                            title = title,
                            start = start,
                            end = end,
                            category = category,
                            memo = memo,
                            recurrence = recurrence,
                            isQuoteNotificationEnabled = notify,
                        )
                    } else {
                        app.schedules.update(
                            existing.copy(
                                title = title,
                                start = start,
                                end = end,
                                category = category,
                                memo = memo,
                                recurrence = recurrence,
                                isQuoteNotificationEnabled = notify,
                            )
                        )
                    }
                    app.notifications.reschedule()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ClayRadius.control),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.textOnAccent,
                ),
            ) {
                Text("저장", style = MaterialTheme.typography.labelLarge)
            }

            if (existing != null) {
                Button(
                    onClick = {
                        app.schedules.remove(existing.id)
                        app.notifications.reschedule()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ClayRadius.control),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceRaised,
                        contentColor = colors.danger,
                    ),
                ) {
                    Text("일정 지우기", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ClayTheme.colors.surface,
    unfocusedContainerColor = ClayTheme.colors.surface,
    focusedTextColor = ClayTheme.colors.textPrimary,
    unfocusedTextColor = ClayTheme.colors.textPrimary,
    focusedBorderColor = ClayTheme.colors.accent,
    unfocusedBorderColor = ClayTheme.colors.separator,
    focusedLabelColor = ClayTheme.colors.accent,
    unfocusedLabelColor = ClayTheme.colors.textSecondary,
    cursorColor = ClayTheme.colors.accent,
)

private const val DEFAULT_HOUR = 9
private const val DEFAULT_DURATION_MINUTES = 60
private val DURATION_CHOICES = listOf(15, 30, 60, 90, 120, 180, 1440)

private fun durationLabel(minutes: Int): String = when {
    minutes >= 1440 -> "하루"
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}시간"
    minutes > 60 -> "${minutes / 60}시간 ${minutes % 60}분"
    else -> "${minutes}분"
}
