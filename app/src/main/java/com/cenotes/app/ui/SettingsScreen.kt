package com.cenotes.app.ui

import com.ce.kit.backup.NotABackupException
import com.ce.kit.backup.TooNewException
import com.ce.kit.ui.settings.AppearancePage as KitAppearancePage
import com.ce.kit.ui.settings.CeAppearanceActions
import com.ce.kit.ui.settings.ChoiceChip
import com.ce.kit.ui.settings.SettingsCard
import com.ce.kit.ui.settings.SwitchRow
import com.ce.kit.ui.settings.IconBubble
import com.ce.kit.ui.settings.BigRow
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
import androidx.compose.material.icons.filled.Info
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
import com.ce.kit.ui.glass.GlassKind
import com.ce.kit.ui.glass.GlassPill
import com.ce.kit.ui.glass.GlassSlider
import com.ce.kit.ui.glass.PillIcon
import com.ce.kit.ui.glass.bounceSpring
import com.ce.kit.ui.glass.glass
import com.ce.kit.ui.glass.glassSpring
import com.ce.kit.ui.glass.onBackdrop
import com.ce.kit.ui.glass.pressScale
import com.ce.kit.ui.glass.softBounceSpring
import com.ce.kit.ui.theme.Accent
import com.cenotes.app.ui.theme.BackupIcon
import com.cenotes.app.ui.theme.DownloadIcon
import com.cenotes.app.ui.theme.LabelIcon
import com.ce.kit.ui.theme.LocalGlass
import androidx.compose.foundation.selection.toggleable
import com.cenotes.app.ui.theme.PaletteIcon
import com.ce.kit.ui.theme.Skin
import com.ce.kit.ui.theme.ThemeMode
import com.ce.kit.ui.theme.ThemeStyle
import com.ce.kit.ui.theme.customPalette
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
    const val UPDATES = "updates"
    const val DIAGNOSTICS = "diagnostics"

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
    // The dialogs of the pages live here, not in the scrolling page: a GlassDialog fills the box it is drawn in.
    val colorEditor = rememberColorEditorState()
    val backupUi = rememberBackupUi(backup, onNotesImported, onAccentChanged)

    val title = when (route) {
        SettingsRoute.APPEARANCE -> stringResource(R.string.appearance)
        SettingsRoute.NOTES -> stringResource(R.string.section_notes)
        SettingsRoute.LABEL_COLORS -> stringResource(R.string.label_colors)
        SettingsRoute.BACKUP -> stringResource(R.string.backup)
        SettingsRoute.UPDATES -> stringResource(R.string.settings_section_updates)
        SettingsRoute.DIAGNOSTICS -> stringResource(R.string.settings_section_diagnostics)
        else -> stringResource(R.string.settings)
    }

    ScreenBackground {
      Box(Modifier.fillMaxSize()) {
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
                        SettingsRoute.LABEL_COLORS -> LabelColorsPage(settings, colorEditor)
                        SettingsRoute.BACKUP -> BackupPage(backupUi)
                        SettingsRoute.UPDATES -> com.ce.kit.ui.update.UpdatesPage(settings.checkUpdates, repo::setCheckUpdates, rememberChangelog())
                        SettingsRoute.DIAGNOSTICS -> com.ce.kit.ui.update.DiagnosticsPage()
                    }
                }
            }
        }
        ColorEditorDialogs(colorEditor, onAdd = onAddColor, onEdit = onEditColor, onDelete = onDeleteColor)
        backupUi.dialogs()
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
        Triple(stringResource(R.string.settings_section_updates), stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), SettingsRoute.UPDATES to DownloadIcon),
        Triple(stringResource(R.string.settings_section_diagnostics), stringResource(R.string.settings_row_diagnostics_body), SettingsRoute.DIAGNOSTICS to Icons.Filled.Info),
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

// --- Appearance (the page itself is the kit's; here only the app's own card and what each choice stores) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearancePage(settings: AppSettings, repo: SettingsRepository, onAccentChanged: () -> Unit) {
    val actions = remember(repo) {
        object : CeAppearanceActions {
            override fun setSkin(skin: Skin) = repo.setSkin(skin)
            override fun setThemeMode(mode: ThemeMode) = repo.setThemeMode(mode)
            override fun setDepth3d(on: Boolean) = repo.setDepth3d(on)
            override fun setThemeStyle(style: ThemeStyle) = repo.setThemeStyle(style)
            override fun setAccent(accent: Accent) { repo.setAccent(accent); onAccentChanged() } // widgets use the accent colour too
            override fun setCustomHue(hue: Float) { repo.setCustomHue(hue); onAccentChanged() }
            override fun setTransparency(percent: Int) = repo.setTransparency(percent)
        }
    }
    KitAppearancePage(settings, actions) {
        // Which side the note list goes on: only where there are two panes (tablets, open foldables), not on phones.
        if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= TWO_PANE_MIN_WIDTH) SettingsCard {
            Text(stringResource(R.string.list_side_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.list_side_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(stringResource(R.string.list_side_left), !settings.listOnRight) { repo.setListOnRight(false) }
                ChoiceChip(stringResource(R.string.list_side_right), settings.listOnRight) { repo.setListOnRight(true) }
            }
        }
    }
}

// --- Label colours ---

@Composable
private fun LabelColorsPage(settings: AppSettings, editor: ColorEditorState) {
    SettingsCard {
        CustomColorsRow(
            colors = settings.customColors,
            selected = 0,
            onPick = null,
            editor = editor,
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
/** What the backup page needs: its two buttons, its status line, and its dialogs (see [dialogs]). */
private class BackupUi(val create: () -> Unit, val import: () -> Unit, val working: Boolean, val status: String?, val dialogs: @Composable () -> Unit)

@Composable
private fun rememberBackupUi(backup: Backup, onNotesImported: () -> Unit, onSettingsRestored: () -> Unit): BackupUi {
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
            } catch (e: NotABackupException) {
                resources.getString(R.string.backup_not_cenotes)
            } catch (e: TooNewException) {
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
    // The dialogs are not drawn here (a GlassDialog fills its box): the screen calls [BackupUi.dialogs] in its full-size Box.
    val createDialog: @Composable () -> Unit = {
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
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run {
            pending = uri to backup.inspect(uri)
            ""
        }
    }
    val restoreDialog: @Composable () -> Unit = {
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
    }
    return BackupUi(
        create = { creating = true },
        import = {
            // Any file: some devices label a .zip with another type (x-zip, x-compressed…) and the picker would grey it
            // out. Backup.inspect checks that it is a Cenotes backup and says so if it is not.
            importLauncher.launch(arrayOf("*/*"))
        },
        working = working,
        status = if (working) resources.getString(R.string.backup_working) else message,
        dialogs = { createDialog(); restoreDialog() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackupPage(ui: BackupUi) {
    SettingsCard {
        Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.backup_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.backup_create), selected = false, enabled = !ui.working, onClick = ui.create)
            ChoiceChip(stringResource(R.string.backup_import), selected = false, enabled = !ui.working, onClick = ui.import)
        }
        if (!ui.status.isNullOrEmpty()) {
            Text(ui.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Asks what to back up: notes (with images or not) and/or settings; at least one of the two. */
@Composable
private fun CreateDialog(onDismiss: () -> Unit, onCreate: (notes: Boolean, images: Boolean, settings: Boolean) -> Unit) {
    var notes by rememberSaveable { mutableStateOf(true) }
    var images by rememberSaveable { mutableStateOf(true) }
    var settings by rememberSaveable { mutableStateOf(true) }
    com.ce.kit.ui.GlassDialog(
        visible = true,
        title = stringResource(R.string.backup_create_title),
        message = "",
        confirmLabel = stringResource(R.string.backup_create_confirm),
        onConfirm = { onCreate(notes, images, settings) },
        onDismiss = onDismiss,
        confirmEnabled = notes || settings,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RestoreRow(R.string.backup_restore_notes, notes) { notes = it }
            if (notes) RestoreRow(R.string.backup_with_images, images, modifier = Modifier.padding(start = 32.dp)) { images = it }
            RestoreRow(R.string.backup_restore_settings, settings) { settings = it }
        }
    }
}

/** Asks what to restore from a backup, with a switch only for what the file carries. Settings start off when notes are there too. */
@Composable
private fun RestoreDialog(contents: Backup.Contents, onDismiss: () -> Unit, onRestore: (notes: Boolean, settings: Boolean) -> Unit) {
    var notes by remember { mutableStateOf(contents.notes) }
    var settings by remember { mutableStateOf(contents.settings && !contents.notes) }
    com.ce.kit.ui.GlassDialog(
        visible = true,
        title = stringResource(R.string.backup_restore_title),
        message = "",
        confirmLabel = stringResource(R.string.backup_restore_confirm),
        onConfirm = { onRestore(notes, settings) },
        onDismiss = onDismiss,
        confirmEnabled = notes || settings,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (contents.notes) RestoreRow(R.string.backup_restore_notes, notes) { notes = it }
            if (contents.settings) RestoreRow(R.string.backup_restore_settings, settings) { settings = it }
        }
    }
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
        com.ce.kit.ui.glass.TaskCheckbox(
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

// --- Updates ---


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

/** The bundled release notes (res/raw/changelog), read once. */
@Composable
private fun rememberChangelog(): String {
    val context = LocalContext.current
    return remember { context.resources.openRawResource(R.raw.changelog).bufferedReader().use { it.readText() } }
}
