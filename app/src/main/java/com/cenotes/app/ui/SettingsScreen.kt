package com.cenotes.app.ui

import com.cenotes.app.ui.theme.NotesIcon
import com.cenotes.app.data.NoteSort
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cenotes.app.BuildConfig
import com.cenotes.app.CenotesApp
import com.cenotes.app.R
import com.cenotes.app.data.AppSettings
import com.cenotes.app.data.Backup
import com.cenotes.app.data.MaxCustomColors
import com.cenotes.app.data.SettingsRepository
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.GlassPill
import com.cenotes.app.ui.glass.GlassSlider
import com.cenotes.app.ui.glass.PillIcon
import com.cenotes.app.ui.glass.bounceSpring
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.glassSpring
import com.cenotes.app.ui.glass.onBackdrop
import com.cenotes.app.ui.glass.pressScale
import com.cenotes.app.ui.glass.softBounceSpring
import com.cenotes.app.ui.theme.Accent
import com.cenotes.app.ui.theme.BackupIcon
import com.cenotes.app.ui.theme.DownloadIcon
import com.cenotes.app.ui.theme.SettingsBackupIcon
import com.cenotes.app.ui.theme.LabelIcon
import com.cenotes.app.ui.theme.LocalGlass
import androidx.compose.foundation.selection.toggleable
import com.cenotes.app.ui.theme.PaletteIcon
import com.cenotes.app.ui.theme.Skin
import com.cenotes.app.ui.theme.ThemeMode
import com.cenotes.app.ui.theme.ThemeStyle
import com.cenotes.app.ui.theme.customPalette
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The screens of Settings: a short tree (main → section) walked with the Back button. A route is a plain string so it
 * survives a rotation.
 */
private object SettingsRoute {
    const val MAIN = "main"
    const val APPEARANCE = "appearance"
    const val NOTES = "notes"
    const val LABEL_COLORS = "label_colors"
    const val BACKUP = "backup"
    const val SETTINGS_BACKUP = "settings_backup"
    const val UPDATES = "updates"

    fun parent(route: String): String? = if (route == MAIN) null else MAIN
    fun depth(route: String): Int = if (route == MAIN) 0 else 1
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    repo: SettingsRepository,
    backup: Backup,
    onAccentChanged: () -> Unit,
    onAddColor: (Int) -> Unit,
    onEditColor: (Int, Int) -> Unit,
    onDeleteColor: (Int) -> Unit,
    onNotesImported: () -> Unit,
    onBack: () -> Unit,
) {
    var route by rememberSaveable { mutableStateOf(SettingsRoute.MAIN) }
    val goBack = { SettingsRoute.parent(route)?.let { route = it } ?: onBack() }
    BackHandler(onBack = goBack)

    val title = when (route) {
        SettingsRoute.APPEARANCE -> stringResource(R.string.appearance)
        SettingsRoute.NOTES -> stringResource(R.string.section_notes)
        SettingsRoute.LABEL_COLORS -> stringResource(R.string.label_colors)
        SettingsRoute.BACKUP -> stringResource(R.string.backup)
        SettingsRoute.SETTINGS_BACKUP -> stringResource(R.string.settings_section_backup)
        SettingsRoute.UPDATES -> stringResource(R.string.settings_section_updates)
        else -> stringResource(R.string.settings)
    }

    ScreenBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPill(Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                PillIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), goBack)
                Text(
                    title,
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackdrop(MaterialTheme.colorScheme.onSurface),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Going deeper grows the new screen in from a little smaller; going back does the opposite.
            AnimatedContent(
                targetState = route,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    if (SettingsRoute.depth(targetState) >= SettingsRoute.depth(initialState)) {
                        (fadeIn(glassSpring()) + scaleIn(softBounceSpring(), initialScale = 0.94f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(glassSpring(), targetScale = 1.03f))
                    } else {
                        (fadeIn(glassSpring()) + scaleIn(softBounceSpring(), initialScale = 1.04f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(glassSpring(), targetScale = 0.94f))
                    }.using(SizeTransform(clip = false))
                },
                label = "settingsPage",
            ) { target ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        // The side padding goes inside the scroll area: the cards' shadows spill into it instead of being cut at the edge.
                        .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (target) {
                        SettingsRoute.MAIN -> MainPage(settings) { route = it }
                        SettingsRoute.APPEARANCE -> AppearancePage(settings, repo, onAccentChanged)
                        SettingsRoute.NOTES -> NotesPage(settings, repo)
                        SettingsRoute.LABEL_COLORS -> LabelColorsPage(settings, onAddColor, onEditColor, onDeleteColor)
                        SettingsRoute.BACKUP -> BackupPage(backup, onNotesImported, onAccentChanged)
                        SettingsRoute.SETTINGS_BACKUP -> SettingsBackupPage(backup, onAccentChanged)
                        SettingsRoute.UPDATES -> UpdatesPage(settings, repo)
                    }
                }
            }
        }
    }
}

// --- The main screen: one big row per section ---

@Composable
private fun MainPage(settings: AppSettings, onRoute: (String) -> Unit) {
    val version = BuildConfig.VERSION_NAME
    val rows: List<Triple<String, String, Pair<String, ImageVector>>> = listOf(
        Triple(stringResource(R.string.appearance), stringResource(R.string.settings_row_appearance_body), SettingsRoute.APPEARANCE to PaletteIcon),
        Triple(stringResource(R.string.section_notes), stringResource(R.string.settings_row_notes_body), SettingsRoute.NOTES to NotesIcon),
        Triple(stringResource(R.string.label_colors), stringResource(R.string.label_colors_summary, settings.customColors.size, MaxCustomColors), SettingsRoute.LABEL_COLORS to LabelIcon),
        Triple(stringResource(R.string.backup), stringResource(R.string.backup_summary), SettingsRoute.BACKUP to BackupIcon),
        Triple(stringResource(R.string.settings_section_backup), stringResource(R.string.settings_row_backup_body), SettingsRoute.SETTINGS_BACKUP to SettingsBackupIcon),
        Triple(stringResource(R.string.settings_section_updates), stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), SettingsRoute.UPDATES to DownloadIcon),
    )
    rows.forEachIndexed { index, (title, body, target) ->
        BigRow(title, body, index, onClick = { onRoute(target.first) }) {
            IconBubble { Icon(target.second, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
    Text(
        stringResource(R.string.version, version),
        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = onBackdrop(MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// --- Appearance ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearancePage(settings: AppSettings, repo: SettingsRepository, onAccentChanged: () -> Unit) {
    // Like Celander's "Design": light or dark, the colour (eight, or any hue), and the glass.
    val glassLook = settings.skin == Skin.GLASS
    val dark = LocalGlass.current.dark
    SettingsCard {
        Text(stringResource(R.string.skin_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.skin_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Skin.entries.forEach { skin ->
                ChoiceChip(stringResource(skin.label), settings.skin == skin) { repo.setSkin(skin) }
            }
        }
        if (settings.skin.forcedDark != null) {
            // Only one mode exists for this design: no light/dark choice to offer.
            Text(stringResource(R.string.skin_always_dark), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.theme_mode_title), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                ).forEach { (mode, label) ->
                    ChoiceChip(stringResource(label), settings.themeMode == mode) { repo.setThemeMode(mode) }
                }
            }
        }
    }
    if (glassLook) SwitchRow(R.string.depth_title, R.string.depth_body, settings.depth3d, repo::setDepth3d)
    // Which side the note list goes on: only where there are two panes (tablets, open foldables), not on phones.
    if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= TWO_PANE_MIN_WIDTH) SettingsCard {
        Text(stringResource(R.string.list_side_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.list_side_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.list_side_left), !settings.listOnRight) { repo.setListOnRight(false) }
            ChoiceChip(stringResource(R.string.list_side_right), settings.listOnRight) { repo.setListOnRight(true) }
        }
    }
    SettingsCard {
        if (glassLook) {
            Text(stringResource(R.string.style_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.style_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeStyle.entries.forEach { style ->
                    ChoiceChip(stringResource(style.label), settings.themeStyle == style) { repo.setThemeStyle(style) }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Text(stringResource(R.string.accent_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.accent_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Accent.entries.forEach { accent ->
                AccentDot(if (dark) accent.dark else accent.light, stringResource(accent.label), selected = settings.customHue == null && settings.accent == accent) {
                    repo.setAccent(accent)
                    onAccentChanged() // widgets use the accent colour too
                }
            }
        }
        Text(stringResource(R.string.accent_custom), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall)
        HueBar(settings.customHue) { hue ->
            repo.setCustomHue(hue)
            onAccentChanged()
        }
    }
    if (glassLook) SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_transparency_title), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(
                NumberFormat.getPercentInstance().format(settings.transparency / 100.0),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(stringResource(R.string.settings_transparency_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GlassSlider(
            value = settings.transparency.toFloat(),
            onValueChange = { repo.setTransparency(it.roundToInt()) },
            valueRange = 0f..100f,
        )
        // How the glass looks right now: coloured bars behind a floating pill.
        Box(
            Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.background)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) // tinted like the app's background
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val c = MaterialTheme.colorScheme
                listOf(
                    c.primary to 0.5f, c.error to 0.8f,
                    (if (dark) Accent.TEAL.dark else Accent.TEAL.light) to 0.35f,
                    (if (dark) Accent.AMBER.dark else Accent.AMBER.light) to 0.65f,
                ).forEach { (color, part) ->
                    Box(Modifier.fillMaxWidth(part).height(14.dp).clip(CircleShape).background(color.copy(alpha = 0.8f)))
                }
            }
            Text(
                stringResource(R.string.preview),
                Modifier.align(Alignment.Center).glass(CircleShape, GlassKind.Chrome, elevation = 6.dp).padding(horizontal = 22.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** One accent colour to pick: a round swatch, with a check and a ring when it is the one in use. */
@Composable
private fun AccentDot(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, bounceSpring(), label = "ring")
    Box(
        Modifier
            .pressScale(0.9f)
            .size(44.dp)
            .then(if (ring > 0.dp) Modifier.border(ring, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), CircleShape) else Modifier)
            .padding(ring + 2.dp)
            .clip(CircleShape)
            .background(color)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, name, Modifier.size(20.dp), tint = Color.White)
    }
}

/**
 * Any colour: a rainbow bar to tap or drag along (like Celander's "Custom colour"). The knob shows the chosen hue;
 * without one (an accent of the list is in use) there is no knob.
 */
@Composable
private fun HueBar(hue: Float?, onHue: (Float) -> Unit) {
    // The same colours the custom accent will have, so the bar shows exactly what each point gives.
    val stops = remember { (0..24).map { customPalette((it * 15f).coerceAtMost(359.9f)).light } }
    GlassSlider(
        value = hue,
        onValueChange = onHue,
        valueRange = 0f..359.9f,
        track = Brush.horizontalGradient(stops),
        knobColor = hue?.let { customPalette(it).light } ?: Color.Transparent,
    )
}

// --- Label colours ---

@Composable
private fun LabelColorsPage(
    settings: AppSettings,
    onAddColor: (Int) -> Unit,
    onEditColor: (Int, Int) -> Unit,
    onDeleteColor: (Int) -> Unit,
) {
    SettingsCard {
        CustomColorsRow(
            colors = settings.customColors,
            selected = 0,
            onPick = null,
            onAdd = onAddColor,
            onEdit = onEditColor,
            onDelete = onDeleteColor,
        )
        Text(
            stringResource(R.string.label_colors_help),
            Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Backup ---

/**
 * Backups are one .zip the user saves and picks with the system file picker (Drive, Downloads, a USB stick…). A dialog
 * asks what goes in (notes, with or without images; settings). Importing looks inside the file and asks what to restore. See [Backup] for the container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupPage(backup: Backup, onNotesImported: () -> Unit, onSettingsRestored: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    // What the user chose in the create dialog, kept while the file picker is open.
    var creating by rememberSaveable { mutableStateOf(false) }
    var chosen by rememberSaveable { mutableStateOf(Triple(true, true, true)) }
    // The file the user picked and what is inside it, while they choose what to restore.
    var pending by remember { mutableStateOf<Pair<android.net.Uri, Backup.Contents>?>(null) }
    var working by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    fun run(block: suspend () -> String) {
        working = true
        message = null
        scope.launch {
            message = try {
                block()
            } catch (e: Backup.NotABackupException) {
                resources.getString(R.string.backup_not_cenotes)
            } catch (e: Backup.TooNewException) {
                resources.getString(R.string.backup_too_new)
            } catch (e: Exception) {
                resources.getString(R.string.backup_failed, e.localizedMessage ?: e.javaClass.simpleName)
            }
            working = false
        }
    }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) run {
            val (notes, images, settings) = chosen
            val r = backup.export(uri, notes, images, settings)
            listOfNotNull(
                if (notes) resources.getString(R.string.backup_exported, r.notes, r.images) else null,
                if (settings) resources.getString(R.string.backup_exported_settings) else null,
            ).joinToString(" ")
        }
    }
    if (creating) {
        CreateDialog(
            onDismiss = { creating = false },
            onCreate = { notes, images, settings ->
                creating = false
                chosen = Triple(notes, images, settings)
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                val name = when {
                    notes && settings -> R.string.backup_file_all
                    notes -> R.string.backup_file_notes
                    else -> R.string.backup_file_settings
                }
                createLauncher.launch(resources.getString(name, date))
            },
        )
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run {
            pending = uri to backup.inspect(uri)
            ""
        }
    }
    pending?.let { (uri, contents) ->
        RestoreDialog(
            contents = contents,
            onDismiss = { pending = null },
            onRestore = { notes, settings ->
                pending = null
                run {
                    val r = backup.import(uri, notes, settings)
                    if (r.notesRestored) onNotesImported()
                    if (r.settingsRestored) onSettingsRestored()
                    listOfNotNull(
                        if (r.notesRestored) resources.getString(R.string.backup_imported, r.added, r.skipped, r.images) else null,
                        if (r.updated > 0) resources.getString(R.string.backup_imported_updated, r.updated) else null,
                        if (r.kept > 0) resources.getString(R.string.backup_imported_kept, r.kept) else null,
                        if (r.settingsRestored) resources.getString(R.string.backup_imported_settings) else null,
                    ).joinToString(" ")
                }
            },
        )
    }

    SettingsCard {
        Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.backup_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.backup_create), selected = false, enabled = !working) { creating = true }
            ChoiceChip(stringResource(R.string.backup_import), selected = false, enabled = !working) {
                // Any file: some devices label a .zip with another type (x-zip, x-compressed…) and the picker would grey it
                // out. Backup.inspect checks that it is a Cenotes backup and says so if it is not.
                importLauncher.launch(arrayOf("*/*"))
            }
        }
        val status = if (working) stringResource(R.string.backup_working) else message
        if (!status.isNullOrEmpty()) {
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Asks what to back up: notes (with images or not) and/or settings; at least one of the two. */
@Composable
private fun CreateDialog(onDismiss: () -> Unit, onCreate: (notes: Boolean, images: Boolean, settings: Boolean) -> Unit) {
    var notes by rememberSaveable { mutableStateOf(true) }
    var images by rememberSaveable { mutableStateOf(true) }
    var settings by rememberSaveable { mutableStateOf(true) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.backup_create_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RestoreRow(R.string.backup_restore_notes, notes) { notes = it }
                if (notes) RestoreRow(R.string.backup_with_images, images, modifier = Modifier.padding(start = 32.dp)) { images = it }
                RestoreRow(R.string.backup_restore_settings, settings) { settings = it }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = notes || settings, onClick = { onCreate(notes, images, settings) }) {
                Text(stringResource(R.string.backup_create_confirm), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Asks what to restore from a backup, with a switch only for what the file carries. Settings start off when notes are there too. */
@Composable
private fun RestoreDialog(contents: Backup.Contents, onDismiss: () -> Unit, onRestore: (notes: Boolean, settings: Boolean) -> Unit) {
    var notes by remember { mutableStateOf(contents.notes) }
    var settings by remember { mutableStateOf(contents.settings && !contents.notes) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.backup_restore_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (contents.notes) RestoreRow(R.string.backup_restore_notes, notes) { notes = it }
                if (contents.settings) RestoreRow(R.string.backup_restore_settings, settings) { settings = it }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = notes || settings, onClick = { onRestore(notes, settings) }) {
                Text(stringResource(R.string.backup_restore_confirm), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * One choice of the restore dialog: a checkbox and its label, the whole row toggles. The box is the same one as in a
 * checklist (Material's, or the Windows 98 one in Retro 98), so it follows every design.
 */
@Composable
private fun RestoreRow(label: Int, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 4.dp),
    ) {
        com.cenotes.app.ui.glass.TaskCheckbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 2.dp, end = 14.dp),
            size = 18.dp,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 0.dp) {
                androidx.compose.material3.Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

// --- Settings backup ---

/**
 * Settings → Settings backup: every setting (look, colours, label colours, updates) in a .zip the user picks
 * (Drive, Downloads…) and brought back on this or another device. Notes are not in it: they have their own backup.
 */
@Composable
private fun SettingsBackupPage(backup: Backup, onAccentChanged: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = runCatching { backup.exportSettings(uri) }
                .fold({ resources.getString(R.string.settings_backup_saved) }, { resources.getString(R.string.settings_backup_failed) })
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = try {
                if (backup.inspect(uri).settings) {
                    backup.import(uri, notes = false, settings = true)
                    onAccentChanged()
                    resources.getString(R.string.settings_backup_restored)
                } else resources.getString(R.string.settings_backup_invalid)
            } catch (e: Backup.NotABackupException) {
                resources.getString(R.string.settings_backup_invalid)
            } catch (e: Backup.TooNewException) {
                resources.getString(R.string.backup_too_new)
            } catch (e: Exception) {
                resources.getString(R.string.settings_backup_failed)
            }
        }
    }
    SettingsCard {
        Text(stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.settings_backup_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.settings_backup_export), false) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                save.launch(resources.getString(R.string.backup_file_settings, date))
            }
            ChoiceChip(stringResource(R.string.settings_backup_import), false) {
                // Any file, for the same reason; inspect says if it is not a settings backup.
                open.launch(arrayOf("*/*"))
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
    }
}

// --- Updates ---

/** Settings → Updates: this version, looking on GitHub for a newer one (by itself, or now), and what each version brought. */
@Composable
private fun UpdatesPage(settings: AppSettings, repo: SettingsRepository) {
    val context = LocalContext.current
    val updates = (context.applicationContext as CenotesApp).updates
    val scope = rememberCoroutineScope()
    // What "Look now" found: null = not asked yet; then a version, "up to date" or "could not check".
    var result by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val upToDate = stringResource(R.string.update_none, BuildConfig.VERSION_NAME)
    val failed = stringResource(R.string.update_check_failed)
    val foundText = stringResource(R.string.update_found)
    SwitchRow(R.string.settings_updates_title, R.string.settings_updates_body, settings.checkUpdates, repo::setCheckUpdates)
    UpdateBanner(allowLater = false)
    SettingsCard {
        Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleSmall)
        result?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        ChoiceChip(stringResource(if (checking) R.string.update_checking else R.string.settings_updates_now), false, enabled = !checking) {
            if (checking) return@ChoiceChip
            checking = true
            scope.launch {
                result = try {
                    if (updates.check(force = true) != null) foundText else upToDate
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed
                }
                checking = false
            }
        }
    }
    WhatsNew()
}

// --- Notes ---

/** Settings → Notes: the order of the list and how checklists behave. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesPage(settings: AppSettings, repo: SettingsRepository) {
    SettingsCard {
        Text(stringResource(R.string.sort_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.sort_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                NoteSort.EDITED to R.string.sort_edited,
                NoteSort.CREATED to R.string.sort_created,
                NoteSort.TITLE to R.string.sort_title_az,
                NoteSort.MANUAL to R.string.sort_manual,
            ).forEach { (sort, label) ->
                ChoiceChip(stringResource(label), settings.sort == sort) { repo.setSort(sort) }
            }
        }
        if (settings.sort == NoteSort.MANUAL) {
            Text(stringResource(R.string.sort_manual_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    SwitchRow(R.string.checked_bottom_title, R.string.checked_bottom_body, settings.checkedToBottom, repo::setCheckedToBottom)
}
