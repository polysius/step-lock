package com.polysius.applocker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R
import com.polysius.applocker.formatMinutes
import com.polysius.applocker.rules.BedtimeRule
import com.polysius.applocker.rules.ScheduleKind
import com.polysius.applocker.rules.ScheduleRule
import com.polysius.applocker.rules.TimeWindow
import com.polysius.applocker.settings.AppSettings
import com.polysius.applocker.toBedtimeRule
import java.time.DayOfWeek

/** Schedule blocks, breaks and the bedtime lock. Every value shown here is editable. */
@Composable
fun ScheduleScreen(
    rules: List<ScheduleRule>,
    settings: AppSettings?,
    onSaveRule: (ScheduleRule) -> Unit,
    onDeleteRule: (ScheduleRule) -> Unit,
    onSetBedtime: (bedtimeMinutes: Int, leadMinutes: Int, endMinutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<RuleEditorTarget?>(null) }
    var editingBedtime by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { editing = RuleEditorTarget(rule = null) }) {
                Text(stringResource(R.string.schedule_add))
            }
        }
        Text(stringResource(R.string.schedule_strict_note), style = MaterialTheme.typography.bodySmall)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (settings != null) {
                item { BedtimeCard(settings.toBedtimeRule(), onEdit = { editingBedtime = true }) }
            }
            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onClick = { editing = RuleEditorTarget(rule) },
                    onToggleEnabled = { enabled -> onSaveRule(rule.copy(isEnabled = enabled)) },
                )
            }
        }
    }

    editing?.let { target ->
        RuleEditorDialog(
            existing = target.rule,
            onDismiss = { editing = null },
            onSave = { rule ->
                onSaveRule(rule)
                editing = null
            },
            onDelete = target.rule?.let { existing ->
                {
                    onDeleteRule(existing)
                    editing = null
                }
            },
        )
    }

    if (editingBedtime && settings != null) {
        BedtimeEditorDialog(
            bedtime = settings.toBedtimeRule(),
            onDismiss = { editingBedtime = false },
            onSave = { bedtime, lead, end ->
                onSetBedtime(bedtime, lead, end)
                editingBedtime = false
            },
        )
    }
}

/** Wraps the rule being edited so "add" (no rule yet) is distinguishable from "closed". */
private class RuleEditorTarget(val rule: ScheduleRule?)

@Composable
private fun BedtimeCard(bedtime: BedtimeRule, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.bedtime_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.bedtime_summary,
                    formatMinutes(bedtime.bedtimeMinutes),
                    bedtime.leadMinutes,
                    formatMinutes(bedtime.endMinutes),
                ),
            )
            Text(
                stringResource(R.string.bedtime_lock_window, formatMinutes(bedtime.startMinutes), formatMinutes(bedtime.endMinutes)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RuleCard(rule: ScheduleRule, onClick: () -> Unit, onToggleEnabled: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                val kindRes = if (rule.kind == ScheduleKind.BLOCK) R.string.kind_block else R.string.kind_break
                Text(
                    "${stringResource(kindRes)} · ${formatMinutes(rule.window.startMinutes)} to " +
                        "${formatMinutes(rule.window.endMinutes)} · ${daysSummary(rule.window.days)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = rule.isEnabled, onCheckedChange = onToggleEnabled)
        }
    }
}

@Composable
private fun RuleEditorDialog(
    existing: ScheduleRule?,
    onDismiss: () -> Unit,
    onSave: (ScheduleRule) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: ScheduleKind.BLOCK) }
    var days by remember { mutableStateOf(existing?.window?.days ?: DayOfWeek.entries.toSet()) }
    var start by remember { mutableStateOf(existing?.window?.startMinutes ?: DEFAULT_NEW_START_MINUTES) }
    var end by remember { mutableStateOf(existing?.window?.endMinutes ?: DEFAULT_NEW_END_MINUTES) }
    var enabled by remember { mutableStateOf(existing?.isEnabled ?: true) }

    val isValid = name.isNotBlank() && days.isNotEmpty() && start != end

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.rule_add_title else R.string.rule_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.rule_name)) },
                    singleLine = true,
                )
                KindChoice(kind = kind, onChange = { kind = it })
                DayChips(selected = days, onChange = { days = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(stringResource(R.string.rule_start), start) { start = it }
                    TimeField(stringResource(R.string.rule_end), end) { end = it }
                }
                if (start != end && end < start) {
                    Text(stringResource(R.string.rule_crosses_midnight), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text(stringResource(R.string.rule_enabled))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(
                        ScheduleRule(
                            id = existing?.id ?: 0,
                            name = name.trim(),
                            kind = kind,
                            window = TimeWindow(days, start, end),
                            isEnabled = enabled,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.dialog_delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
            }
        },
    )
}

@Composable
private fun KindChoice(kind: ScheduleKind, onChange: (ScheduleKind) -> Unit) {
    Column {
        listOf(ScheduleKind.BLOCK to R.string.kind_block, ScheduleKind.BREAK to R.string.kind_break)
            .forEach { (option, labelRes) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = kind == option, onClick = { onChange(option) })
                    Text(stringResource(labelRes))
                }
            }
    }
}

@Composable
private fun DayChips(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.chunked(4).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowDays.forEach { day ->
                    FilterChip(
                        selected = day in selected,
                        onClick = { onChange(if (day in selected) selected - day else selected + day) },
                        label = { Text(shortDayName(day)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BedtimeEditorDialog(
    bedtime: BedtimeRule,
    onDismiss: () -> Unit,
    onSave: (bedtimeMinutes: Int, leadMinutes: Int, endMinutes: Int) -> Unit,
) {
    var bedtimeMinutes by remember { mutableStateOf(bedtime.bedtimeMinutes) }
    var leadText by remember { mutableStateOf(bedtime.leadMinutes.toString()) }
    var endMinutes by remember { mutableStateOf(bedtime.endMinutes) }

    val lead = leadText.toIntOrNull()
    val candidate = lead?.let { BedtimeRule(bedtimeMinutes, it, endMinutes, bedtime.days) }
    val preview = candidate?.startMinutes
    val isValid = candidate?.isValid == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bedtime_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(stringResource(R.string.bedtime_time), bedtimeMinutes) { bedtimeMinutes = it }
                OutlinedTextField(
                    value = leadText,
                    onValueChange = { text -> leadText = text.filter { it.isDigit() }.take(MAX_LEAD_DIGITS) },
                    label = { Text(stringResource(R.string.bedtime_lead)) },
                    singleLine = true,
                )
                TimeField(stringResource(R.string.bedtime_end), endMinutes) { endMinutes = it }
                if (preview != null) {
                    Text(stringResource(R.string.bedtime_preview, formatMinutes(preview)))
                }
                if (candidate != null && !isValid) {
                    Text(stringResource(R.string.bedtime_invalid), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = { onSave(bedtimeMinutes, lead ?: 0, endMinutes) }) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** A labelled time button that opens a 24-hour picker. Values are minutes since midnight. */
@OptIn(ExperimentalMaterial3Api::class) // TimePicker is still marked experimental in this Material3 release.
@Composable
private fun TimeField(label: String, minutes: Int, onChange: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { picking = true }) { Text(formatMinutes(minutes)) }
    }

    if (picking) {
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.hour * 60 + state.minute)
                    picking = false
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.dialog_cancel)) }
            },
        )
    }
}

/** Start and end offered when adding a rule. Only a starting point in the form, not a rule. */
private const val DEFAULT_NEW_START_MINUTES = 9 * 60
private const val DEFAULT_NEW_END_MINUTES = 10 * 60

/** The lead text field accepts at most this many digits. */
private const val MAX_LEAD_DIGITS = 3
