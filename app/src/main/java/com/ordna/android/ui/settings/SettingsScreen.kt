package com.ordna.android.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.ordna.android.R
import com.ordna.android.data.repository.CompletionMethod
import com.ordna.android.data.repository.LayoutDensity
import com.ordna.android.data.repository.WidgetBackground
import com.ordna.android.data.repository.WidgetSorting
import com.ordna.android.ui.theme.AppTheme
import com.ordna.android.ui.theme.appThemeColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val language by viewModel.language.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val groupByList by viewModel.groupByList.collectAsState()
    val completionMethod by viewModel.completionMethod.collectAsState()
    val appLayoutDensity by viewModel.appLayoutDensity.collectAsState()
    val shareListTitle by viewModel.shareListTitle.collectAsState()
    val createListTitle by viewModel.createListTitle.collectAsState()
    val availableLists by viewModel.availableLists.collectAsState()
    val listsLoading by viewModel.listsLoading.collectAsState()

    val orderedLists by viewModel.orderedLists.collectAsState()

    // Load lists for ordering
    LaunchedEffect(Unit) { viewModel.loadAvailableLists() }

    val widgetBg by viewModel.widgetBackground.collectAsState()
    val widgetOpacity by viewModel.widgetOpacity.collectAsState()
    val widgetShowCompleted by viewModel.widgetShowCompleted.collectAsState()
    val widgetLayoutDensity by viewModel.widgetLayoutDensity.collectAsState()
    val widgetSorting by viewModel.widgetSorting.collectAsState()
    val streak by viewModel.streak.collectAsState()

    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val morningEnabled by viewModel.reminderMorningEnabled.collectAsState()
    val morningHour by viewModel.reminderMorningHour.collectAsState()
    val morningMinute by viewModel.reminderMorningMinute.collectAsState()
    val middayEnabled by viewModel.reminderMiddayEnabled.collectAsState()
    val middayHour by viewModel.reminderMiddayHour.collectAsState()
    val middayMinute by viewModel.reminderMiddayMinute.collectAsState()
    val eveningEnabled by viewModel.reminderEveningEnabled.collectAsState()
    val eveningHour by viewModel.reminderEveningHour.collectAsState()
    val eveningMinute by viewModel.reminderEveningMinute.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permissionDeniedMsg = stringResource(R.string.reminder_permission_needed)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setRemindersEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar(permissionDeniedMsg) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // ── App ──
            SettingSectionHeader(stringResource(R.string.settings_section_display))

            // Language
            SettingLabel(
                title = stringResource(R.string.settings_language_title),
                subtitle = stringResource(R.string.settings_language_subtitle),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = language == "system",
                    onClick = { viewModel.setLanguage("system") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) { Text(stringResource(R.string.settings_language_system)) }
                SegmentedButton(
                    selected = language == "en",
                    onClick = { viewModel.setLanguage("en") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) { Text(stringResource(R.string.settings_language_en)) }
                SegmentedButton(
                    selected = language == "sv",
                    onClick = { viewModel.setLanguage("sv") },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) { Text(stringResource(R.string.settings_language_sv)) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme (most impactful — first)
            SettingLabel(
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(R.string.settings_theme_subtitle),
            )
            ThemePicker(
                selected = appTheme,
                onSelect = { viewModel.setAppTheme(it) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Layout density
            SettingLabel(
                title = stringResource(R.string.settings_layout_title),
                subtitle = stringResource(R.string.settings_layout_subtitle),
            )
            LayoutDensityPicker(
                selected = appLayoutDensity,
                onSelect = { viewModel.setAppLayoutDensity(it) },
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Grouping
            SettingLabel(
                title = stringResource(R.string.settings_grouping_title),
                subtitle = stringResource(R.string.settings_grouping_subtitle),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !groupByList,
                    onClick = { viewModel.setGroupByList(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.settings_grouping_flat)) }
                SegmentedButton(
                    selected = groupByList,
                    onClick = { viewModel.setGroupByList(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.settings_grouping_by_list)) }
            }

            if (!groupByList) {
                Spacer(modifier = Modifier.height(20.dp))

                // List order
                SettingLabel(
                    title = stringResource(R.string.settings_list_order_title),
                    subtitle = stringResource(R.string.settings_list_order_subtitle),
                )
                if (orderedLists.isNotEmpty()) {
                    ListOrderPicker(
                        lists = orderedLists,
                        onMoveUp = { viewModel.moveListInOrder(it.id, -1) },
                        onMoveDown = { viewModel.moveListInOrder(it.id, 1) },
                    )
                } else if (listsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Completion method
            SettingLabel(
                title = stringResource(R.string.settings_completion_title),
                subtitle = stringResource(R.string.settings_completion_subtitle),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = completionMethod == CompletionMethod.CHECKBOX,
                    onClick = { viewModel.setCompletionMethod(CompletionMethod.CHECKBOX) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                ) { Text(stringResource(R.string.settings_completion_tap)) }
                SegmentedButton(
                    selected = completionMethod == CompletionMethod.SWIPE,
                    onClick = { viewModel.setCompletionMethod(CompletionMethod.SWIPE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                ) { Text(stringResource(R.string.settings_completion_swipe)) }
                SegmentedButton(
                    selected = completionMethod == CompletionMethod.BOTH,
                    onClick = { viewModel.setCompletionMethod(CompletionMethod.BOTH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                ) { Text(stringResource(R.string.settings_completion_both)) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Reminders ──
            SettingSectionHeader(stringResource(R.string.settings_section_reminders))

            SettingToggle(
                title = stringResource(R.string.reminder_master_toggle),
                subtitle = stringResource(R.string.reminder_master_subtitle),
                checked = remindersEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.setRemindersEnabled(true)
                        } else {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.setRemindersEnabled(enabled)
                    }
                },
            )

            ReminderSlotRow(
                label = stringResource(R.string.reminder_morning),
                enabled = remindersEnabled && morningEnabled,
                masterEnabled = remindersEnabled,
                hour = morningHour,
                minute = morningMinute,
                onToggle = { viewModel.setReminderMorningEnabled(it) },
                onTimeChange = { h, m -> viewModel.setReminderMorningTime(h, m) },
            )

            ReminderSlotRow(
                label = stringResource(R.string.reminder_midday),
                enabled = remindersEnabled && middayEnabled,
                masterEnabled = remindersEnabled,
                hour = middayHour,
                minute = middayMinute,
                onToggle = { viewModel.setReminderMiddayEnabled(it) },
                onTimeChange = { h, m -> viewModel.setReminderMiddayTime(h, m) },
            )

            ReminderSlotRow(
                label = stringResource(R.string.reminder_evening),
                enabled = remindersEnabled && eveningEnabled,
                masterEnabled = remindersEnabled,
                hour = eveningHour,
                minute = eveningMinute,
                onToggle = { viewModel.setReminderEveningEnabled(it) },
                onTimeChange = { h, m -> viewModel.setReminderEveningTime(h, m) },
            )

            // Default creation list
            Spacer(modifier = Modifier.height(20.dp))

            SettingLabel(
                title = stringResource(R.string.settings_create_list_title),
                subtitle = stringResource(R.string.settings_create_list_subtitle),
            )
            ShareListPicker(
                selectedTitle = createListTitle,
                availableLists = availableLists,
                isLoading = listsLoading,
                onExpand = { viewModel.loadAvailableLists() },
                onSelect = { viewModel.setCreateList(it.id, it.title) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick share ──
            SettingSectionHeader(stringResource(R.string.settings_section_share))

            SettingLabel(
                title = stringResource(R.string.settings_share_list_title),
                subtitle = stringResource(R.string.settings_share_list_subtitle),
            )
            ShareListPicker(
                selectedTitle = shareListTitle,
                availableLists = availableLists,
                isLoading = listsLoading,
                onExpand = { viewModel.loadAvailableLists() },
                onSelect = { viewModel.setShareList(it.id, it.title) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Widget ──
            SettingSectionHeader(stringResource(R.string.settings_section_widget))

            // Background + opacity (only when using System theme — custom themes set their own)
            if (appTheme == "SYSTEM") {
                SettingLabel(
                    title = stringResource(R.string.settings_widget_bg_title),
                    subtitle = stringResource(R.string.settings_widget_bg_subtitle),
                )
                ColorPicker(
                    selected = widgetBg,
                    onSelect = { viewModel.setWidgetBackground(it) },
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            SettingLabel(
                title = stringResource(R.string.settings_widget_opacity_title),
                subtitle = stringResource(R.string.settings_widget_opacity_subtitle),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Slider(
                    value = widgetOpacity,
                    onValueChange = { viewModel.setWidgetOpacity(it) },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(widgetOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Widget layout density
            SettingLabel(
                title = stringResource(R.string.settings_layout_title),
                subtitle = stringResource(R.string.settings_layout_subtitle),
            )
            LayoutDensityPicker(
                selected = widgetLayoutDensity,
                onSelect = { viewModel.setWidgetLayoutDensity(it) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Widget sorting
            SettingLabel(
                title = stringResource(R.string.settings_widget_sorting_title),
                subtitle = stringResource(R.string.settings_widget_sorting_subtitle),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = widgetSorting == WidgetSorting.FLAT,
                    onClick = { viewModel.setWidgetSorting(WidgetSorting.FLAT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.settings_grouping_flat)) }
                SegmentedButton(
                    selected = widgetSorting == WidgetSorting.BY_LIST,
                    onClick = { viewModel.setWidgetSorting(WidgetSorting.BY_LIST) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.settings_grouping_by_list)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show completed toggle (last — least changed)
            SettingToggle(
                title = stringResource(R.string.settings_widget_show_completed_title),
                subtitle = stringResource(R.string.settings_widget_show_completed_subtitle),
                checked = widgetShowCompleted,
                onCheckedChange = { viewModel.setWidgetShowCompleted(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Stats ──
            SettingSectionHeader(stringResource(R.string.settings_section_stats))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_streak_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_streak_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (streak > 0) "\uD83D\uDD25 ${stringResource(R.string.settings_streak_value, streak)}"
                    else stringResource(R.string.settings_streak_none),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (streak > 0) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val uriHandler = LocalUriHandler.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_made_by),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_github),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/klppl") },
                    )
                    Text(
                        text = stringResource(R.string.settings_buy_coffee),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://buymeacoffee.com/klippel") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ColorPicker(
    selected: WidgetBackground,
    onSelect: (WidgetBackground) -> Unit,
) {
    val options = listOf(
        WidgetBackground.AUTO to Pair(null, stringResource(R.string.settings_widget_color_auto)),
        WidgetBackground.WHITE to Pair(Color.White, stringResource(R.string.settings_widget_color_white)),
        WidgetBackground.DARK to Pair(Color(0xFF2D2D2D), stringResource(R.string.settings_widget_color_dark)),
        WidgetBackground.BLACK to Pair(Color.Black, stringResource(R.string.settings_widget_color_black)),
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for ((bg, pair) in options) {
            val (color, label) = pair
            val isSelected = selected == bg

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .then(
                            if (color != null) Modifier.background(color, CircleShape)
                            else Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape,
                            )
                        )
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            )
                            else Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                        )
                        .clickable { onSelect(bg) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        val checkColor = when (bg) {
                            WidgetBackground.DARK, WidgetBackground.BLACK -> Color.White
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = checkColor,
                            modifier = Modifier.size(20.dp),
                        )
                    } else if (bg == WidgetBackground.AUTO) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutDensityPicker(
    selected: LayoutDensity,
    onSelect: (LayoutDensity) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selected == LayoutDensity.COMFORTABLE,
            onClick = { onSelect(LayoutDensity.COMFORTABLE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
        ) { Text(stringResource(R.string.settings_layout_comfortable)) }
        SegmentedButton(
            selected = selected == LayoutDensity.DEFAULT,
            onClick = { onSelect(LayoutDensity.DEFAULT) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
        ) { Text(stringResource(R.string.settings_layout_default)) }
        SegmentedButton(
            selected = selected == LayoutDensity.COMPACT,
            onClick = { onSelect(LayoutDensity.COMPACT) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
        ) { Text(stringResource(R.string.settings_layout_compact)) }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingLabel(title: String, subtitle: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun ReminderSlotRow(
    label: String,
    enabled: Boolean,
    masterEnabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeText = String.format("%02d:%02d", hour, minute)
    val alpha = if (masterEnabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { this.alpha = alpha },
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(enabled = masterEnabled) { showTimePicker = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .graphicsLayer { this.alpha = alpha },
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            enabled = masterEnabled,
        )
    }

    if (showTimePicker) {
        ReminderTimePicker(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                onTimeChange(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cd_back))
            }
        },
        text = {
            TimePicker(state = timePickerState)
        },
    )
}

@Composable
private fun ListOrderPicker(
    lists: List<TaskListOption>,
    onMoveUp: (TaskListOption) -> Unit,
    onMoveDown: (TaskListOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp),
            )
            .padding(vertical = 4.dp),
    ) {
        lists.forEachIndexed { index, list ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(list.color), RoundedCornerShape(3.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = list.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onMoveUp(list) },
                    enabled = index > 0,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (index > 0) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    )
                }
                IconButton(
                    onClick = { onMoveDown(list) },
                    enabled = index < lists.size - 1,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (index < lists.size - 1) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareListPicker(
    selectedTitle: String?,
    availableLists: List<TaskListOption>,
    isLoading: Boolean,
    onExpand: () -> Unit,
    onSelect: (TaskListOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = {
                onExpand()
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_share_list_loading))
            } else {
                Text(selectedTitle ?: stringResource(R.string.settings_share_list_none))
            }
        }

        DropdownMenu(
            expanded = expanded && availableLists.isNotEmpty(),
            onDismissRequest = { expanded = false },
        ) {
            availableLists.forEach { list ->
                DropdownMenuItem(
                    text = { Text(list.title) },
                    onClick = {
                        onSelect(list)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemePicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    data class ThemeOption(
        val key: String,
        val labelRes: Int,
        val primaryColor: Color,
        val bgColor: Color,
    )

    val themes = listOf(
        ThemeOption("SYSTEM", R.string.theme_system, Color(0xFF6750A4), Color(0xFF1C1B1F)),
        ThemeOption("CATPPUCCIN", R.string.theme_catppuccin, Color(0xFFCBA6F7), Color(0xFF1E1E2E)),
        ThemeOption("ROSE_PINE", R.string.theme_rose_pine, Color(0xFFC4A7E7), Color(0xFF191724)),
        ThemeOption("GRUVBOX", R.string.theme_gruvbox, Color(0xFFD79921), Color(0xFF282828)),
        ThemeOption("TOKYO_NIGHT", R.string.theme_tokyo_night, Color(0xFF7AA2F7), Color(0xFF1A1B26)),
        ThemeOption("DRACULA", R.string.theme_dracula, Color(0xFFBD93F9), Color(0xFF282A36)),
        ThemeOption("KANAGAWA", R.string.theme_kanagawa, Color(0xFF7E9CD8), Color(0xFF1F1F28)),
        ThemeOption("OXOCARBON", R.string.theme_oxocarbon, Color(0xFFBE95FF), Color(0xFF161616)),
    )

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (theme in themes) {
            val isSelected = selected == theme.key

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.bgColor, RoundedCornerShape(10.dp))
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                theme.primaryColor,
                                RoundedCornerShape(10.dp),
                            )
                            else Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp),
                            )
                        )
                        .clickable { onSelect(theme.key) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(theme.primaryColor, CircleShape),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(theme.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
