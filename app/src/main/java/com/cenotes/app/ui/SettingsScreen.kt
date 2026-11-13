package com.cenotes.app.ui

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
import androidx.compose.foundation.clickable
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
import com.cenotes.app.ui.glass.GlassBackground
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
        SettingsRoute.LABEL_COLORS -> stringResource(R.string.label_colors)
        SettingsRoute.BACKUP -> stringResource(R.string.backup)
        SettingsRoute.SETTINGS_BACKUP -> stringResource(R.string.settings_section_backup)
        SettingsRoute.UPDATES -> stringResource(R.string.settings_section_updates)
        else -> stringResource(R.string.settings)
    }

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPill(Modifier.fillMaxWidth()) {
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
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (target) {
                        SettingsRoute.MAIN -> MainPage(settings) { route = it }
                        SettingsRoute.APPEARANCE -> AppearancePage(settings, repo, onAccentChanged)
                        SettingsRoute.LABEL_COLORS -> LabelColorsPage(settings, onAddColor, onEditColor, onDeleteColor)
                        SettingsRoute.BACKUP -> BackupPage(backup, onNotesImported)
                        SettingsRoute.SETTINGS_BACKUP -> SettingsBackupPage(repo, onAccentChanged)
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
            .clickable(role = Role.RadioButton, onClickLabel = name, onClick = onClick),
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
 * Export to / import from a .zip the user picks with the system file picker (Drive, Downloads,
 * a USB stick…). Images are optional so a backup can stay small.
 */
@Composable
private fun BackupPage(backup: Backup, onNotesImported: () -> Unit) {
    val resources = androidx.compose.ui.platform.LocalResources.current
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
                resources.getString(R.string.backup_not_cenotes)
            } catch (e: Exception) {
                resources.getString(R.string.backup_failed, e.localizedMessage ?: e.javaClass.simpleName)
            }
            working = false
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) run {
            val r = backup.export(uri, includeImages)
            resources.getString(R.string.backup_exported, r.notes, r.images)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) run {
            val r = backup.import(uri)
            onNotesImported()
            resources.getString(R.string.backup_imported, r.added, r.skipped, r.images)
        }
    }

    SwitchRow(
        R.string.backup_include_images,
        if (includeImages) R.string.backup_include_images_on else R.string.backup_include_images_off,
        includeImages,
    ) { includeImages = it }
    SettingsCard {
        Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.backup_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.backup_export), selected = false, enabled = !working) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                exportLauncher.launch(resources.getString(R.string.backup_file_name, date))
            }
            ChoiceChip(stringResource(R.string.backup_import), selected = false, enabled = !working) {
                importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
            }
        }
        val status = if (working) stringResource(R.string.backup_working) else message
        if (status != null) {
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// --- Settings backup ---

/**
 * Settings → Settings backup: every setting (look, colours, label colours, updates) saved to a file the user picks
 * (Drive, Downloads…) and brought back on this or another device. Notes are not in it: they have their own backup.
 */
@Composable
private fun SettingsBackupPage(repo: SettingsRepository, onAccentChanged: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<Int?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(repo.exportJson().toByteArray()) }
        }.fold({ R.string.settings_backup_saved }, { R.string.settings_backup_failed })
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }.getOrNull()
        message = when {
            text == null -> R.string.settings_backup_failed
            repo.importJson(text) -> R.string.settings_backup_restored.also { onAccentChanged() }
            else -> R.string.settings_backup_invalid
        }
    }
    val fileName = stringResource(R.string.settings_backup_file)
    SettingsCard {
        Text(stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.settings_backup_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.settings_backup_export), false) { save.launch(fileName) }
            ChoiceChip(stringResource(R.string.settings_backup_import), false) {
                open.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
        }
        message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
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
        Row {
            ChoiceChip(stringResource(if (checking) R.string.update_checking else R.string.settings_updates_now), false, enabled = !checking) {
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
    }
    WhatsNew()
}
