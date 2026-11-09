package com.cenotes.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cenotes.app.R
import com.cenotes.app.data.AppSettings
import com.cenotes.app.ui.glass.GlassBackground
import com.cenotes.app.ui.glass.GlassIconButton
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.GlassPill
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.glassSpring
import com.cenotes.app.ui.theme.Accent
import com.cenotes.app.ui.theme.LocalGlass
import com.cenotes.app.ui.theme.PaletteIcon
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import com.cenotes.app.data.Backup
import com.cenotes.app.ui.theme.BackupIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    backup: Backup,
    onAccentChange: (Accent) -> Unit,
    onTransparencyChange: (Int) -> Unit,
    onNotesImported: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
                Spacer(Modifier.width(12.dp))
                GlassPill {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            ExpandableCard(
                icon = PaletteIcon,
                title = stringResource(R.string.appearance),
                subtitle = stringResource(R.string.appearance_summary, settings.accent.label, settings.transparency),
                initiallyExpanded = true,
            ) {
                Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val dark = LocalGlass.current.dark
                    Accent.entries.forEach { accent ->
                        AccentDot(
                            color = if (dark) accent.dark else accent.light,
                            label = accent.label,
                            selected = accent == settings.accent,
                            onClick = { onAccentChange(accent) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.transparency),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.transparency_value, settings.transparency),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Slider(
                    value = settings.transparency.toFloat(),
                    onValueChange = { onTransparencyChange(it.roundToInt()) },
                    valueRange = 0f..100f,
                )
                Text(
                    stringResource(R.string.transparency_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            BackupCard(backup, onNotesImported)

            Spacer(Modifier.height(40.dp))
            Text(
                stringResource(R.string.version, version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Export to / import from a .zip the user picks with the system file picker (Drive, Downloads,
 * a USB stick…). Images are optional so a backup can stay small.
 */
@Composable
private fun BackupCard(backup: Backup, onNotesImported: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var includeImages by rememberSaveable { mutableStateOf(true) }
    var working by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> String) {
        working = true
        message = null
        scope.launch {
            message = try {
                block()
            } catch (e: Backup.NotABackupException) {
                context.getString(R.string.backup_not_cenotes)
            } catch (e: Exception) {
                context.getString(R.string.backup_failed, e.localizedMessage ?: e.javaClass.simpleName)
            }
            working = false
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) run {
            val r = backup.export(uri, includeImages)
            context.getString(R.string.backup_exported, r.notes, r.images)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run {
            val r = backup.import(uri)
            onNotesImported()
            context.getString(R.string.backup_imported, r.added, r.skipped, r.images)
        }
    }

    ExpandableCard(
        icon = BackupIcon,
        title = stringResource(R.string.backup),
        subtitle = stringResource(R.string.backup_summary),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.backup_include_images), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(if (includeImages) R.string.backup_include_images_on else R.string.backup_include_images_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = includeImages, onCheckedChange = { includeImages = it })
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                    exportLauncher.launch(context.getString(R.string.backup_file_name, date))
                },
                enabled = !working,
                shape = CircleShape,
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(stringResource(R.string.backup_export), fontWeight = FontWeight.Bold) }
            FilledTonalButton(
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                enabled = !working,
                shape = CircleShape,
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(stringResource(R.string.backup_import), fontWeight = FontWeight.Bold) }
        }
        val status = if (working) stringResource(R.string.backup_working) else message
        if (status != null) {
            Spacer(Modifier.height(12.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Settings card like Celander's: tinted icon circle, title + summary, chevron that springs open. */
@Composable
private fun ExpandableCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, glassSpring(), label = "chevron")
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), GlassKind.Card)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.expand),
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(glassSpring()) + expandVertically(glassSpring()),
            exit = fadeOut(glassSpring()) + shrinkVertically(glassSpring()),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), content = content)
        }
    }
}

@Composable
private fun AccentDot(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                else Color.White.copy(alpha = 0.5f),
                shape = CircleShape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
    }
}
