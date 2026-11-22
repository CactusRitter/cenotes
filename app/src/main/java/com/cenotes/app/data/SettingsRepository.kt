package com.cenotes.app.data

import android.content.Context
import com.ce.kit.ui.theme.Accent
import com.ce.kit.ui.theme.CeAppearance
import com.ce.kit.ui.theme.AccentPalette
import com.ce.kit.ui.theme.Skin
import com.ce.kit.ui.theme.ThemeMode
import com.ce.kit.ui.theme.ThemeStyle
import com.ce.kit.ui.theme.customPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ce.kit.backup.PrefsBackup
import com.ce.kit.settings.CommonKeys
import com.ce.kit.settings.CommonSettings
import org.json.JSONObject

enum class NoteLayout { GRID, LIST }

/** How the notes are ordered (pinned ones always go first). */
enum class NoteSort { EDITED, CREATED, TITLE, MANUAL }

data class AppSettings(
    override val accent: Accent = Accent.INDIGO,
    /** A colour of the free hue bar (0–360) instead of [accent]; null = the chosen accent. */
    override val customHue: Float? = null,
    /** Light, dark or following the phone. */
    override val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Background and glass tint of the glass look (only used with [Skin.GLASS]). */
    override val themeStyle: ThemeStyle = ThemeStyle.AURORA,
    /** The whole look of the app; [Skin.GLASS] is the one shared with Celander and Cemail. */
    override val skin: Skin = Skin.GLASS,
    /** Solid "3D" shadow under every glass surface. */
    override val depth3d: Boolean = false,
    /** 0 = solid glass, 100 = fully see-through. */
    override val transparency: Int = 65,
    val layout: NoteLayout = NoteLayout.GRID,
    /** Colours the user added for labels (ARGB), at most [MaxCustomColors]. */
    val customColors: List<Int> = emptyList(),
    /** Ask GitHub once or twice a day whether there is a newer Cenotes. */
    val checkUpdates: Boolean = true,
    val sort: NoteSort = NoteSort.EDITED,
    /** Ticked items of a checklist sink to the end of their list (like Google Keep). */
    val checkedToBottom: Boolean = true,
    /** Tablets: the list on the right (and the floating buttons on the left), as in Cemail. */
    val listOnRight: Boolean = false,
    /** Where Settings sits on a wide screen: -1 start, 0 centre, 1 end (moved by dragging beside it), as in Cemail. */
    val settingsDock: Int = 0,
) : CeAppearance {
    /** The app's colour in its four shades: the custom one, or the chosen accent. */
    override val accentPalette: AccentPalette
        get() = customHue?.let { customPalette(it) } ?: accent.palette
}

const val MaxCustomColors = 10

/** Small app preferences, kept in SharedPreferences and exposed as a StateFlow. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private inline fun <reified E : Enum<E>> enumPref(key: String, default: E): E =
        prefs.getString(key, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun read(): AppSettings {
        val c = CommonSettings.read(prefs, CommonKeys(depth3d = KEY_DEPTH))
        return AppSettings(
            accent = c.accent,
            customHue = c.customHue,
            themeMode = c.themeMode,
            themeStyle = c.themeStyle,
            skin = c.skin,
            depth3d = c.depth3d,
            transparency = c.transparency,
            layout = enumPref(KEY_LAYOUT, NoteLayout.GRID),
            customColors = prefs.getString(KEY_COLORS, null).orEmpty()
                .split(',').mapNotNull { it.toIntOrNull() }.take(MaxCustomColors),
            checkUpdates = c.checkUpdates,
            sort = enumPref(KEY_SORT, NoteSort.EDITED),
            checkedToBottom = prefs.getBoolean(KEY_CHECKED_BOTTOM, true),
            listOnRight = prefs.getBoolean(KEY_LIST_RIGHT, false),
            settingsDock = prefs.getInt(KEY_SETTINGS_DOCK, 0).coerceIn(-1, 1),
        )
    }

    fun setSettingsDock(value: Int) {
        prefs.edit().putInt(KEY_SETTINGS_DOCK, value.coerceIn(-1, 1)).apply()
        _state.value = _state.value.copy(settingsDock = value.coerceIn(-1, 1))
    }

    fun setListOnRight(value: Boolean) {
        prefs.edit().putBoolean(KEY_LIST_RIGHT, value).apply()
        _state.value = _state.value.copy(listOnRight = value)
    }

    fun setSort(value: NoteSort) {
        prefs.edit().putString(KEY_SORT, value.name).apply()
        _state.value = _state.value.copy(sort = value)
    }

    fun setCheckedToBottom(value: Boolean) {
        prefs.edit().putBoolean(KEY_CHECKED_BOTTOM, value).apply()
        _state.value = _state.value.copy(checkedToBottom = value)
    }

    fun setCheckUpdates(value: Boolean) {
        prefs.edit().putBoolean(KEY_UPDATES, value).apply()
        _state.value = _state.value.copy(checkUpdates = value)
    }

    /**
     * Every setting as JSON, for Settings → Settings backup (the same format as Cemail's, with "app": "cenotes").
     * Notes are not in it: they have their own backup.
     */
    fun exportJson(): String =
        JSONObject().put("app", "cenotes").put("format", 1).put("settings", exportValues()).toString(2)

    /** The settings as typed JSON (also embedded in the notes backup). Keys that look like secrets are never included. */
    fun exportValues(): JSONObject = PrefsBackup.export(prefs)


    /** Applies a backup made by [exportJson]; returns false (and changes nothing) if [text] is not one. */
    fun importJson(text: String): Boolean {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (root.optString("app") != "cenotes") return false
        val values = root.optJSONObject("settings") ?: return false
        importValues(values)
        return true
    }

    /** Applies typed settings made by [exportValues]; secret-looking keys are ignored even if a file carries them. */
    fun importValues(values: JSONObject) {
        PrefsBackup.import(prefs, values)
        _state.value = read()
    }


    private fun saveColors(colors: List<Int>) {
        val list = colors.distinct().take(MaxCustomColors)
        prefs.edit().putString(KEY_COLORS, list.joinToString(",")).apply()
        _state.value = _state.value.copy(customColors = list)
    }

    fun addCustomColor(color: Int) = saveColors(_state.value.customColors + color)

    fun replaceCustomColor(old: Int, new: Int) =
        saveColors(_state.value.customColors.map { if (it == old) new else it })

    fun removeCustomColor(color: Int) = saveColors(_state.value.customColors - color)

    /** Picks one of the eight accents (and drops any custom hue). */
    fun setAccent(accent: Accent) {
        prefs.edit().putString(KEY_ACCENT, accent.name).remove(KEY_HUE).apply()
        _state.value = _state.value.copy(accent = accent, customHue = null)
    }

    /** Picks a colour of the free hue bar. */
    fun setCustomHue(hue: Float) {
        prefs.edit().putFloat(KEY_HUE, hue).apply()
        _state.value = _state.value.copy(customHue = hue)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setThemeStyle(style: ThemeStyle) {
        prefs.edit().putString(KEY_STYLE, style.name).apply()
        _state.value = _state.value.copy(themeStyle = style)
    }

    fun setSkin(skin: Skin) {
        prefs.edit().putString(KEY_SKIN, skin.name).apply()
        _state.value = _state.value.copy(skin = skin)
    }

    fun setDepth3d(value: Boolean) {
        prefs.edit().putBoolean(KEY_DEPTH, value).apply()
        _state.value = _state.value.copy(depth3d = value)
    }

    fun setTransparency(value: Int) {
        prefs.edit().putInt(KEY_TRANSPARENCY, value).apply()
        _state.value = _state.value.copy(transparency = value)
    }

    fun setLayout(layout: NoteLayout) {
        prefs.edit().putString(KEY_LAYOUT, layout.name).apply()
        _state.value = _state.value.copy(layout = layout)
    }

    companion object {
        /** Every key this class writes (the backup test checks none of them looks like a secret). */
        val AllKeys = listOf("accent", "accent_hue", "theme_mode", "theme_style", "skin", "depth3d", "transparency", "layout", "customColors", "check_updates", "sort", "checked_to_bottom", "list_on_right", "settings_dock")

        private const val KEY_ACCENT = "accent"
        const val KEY_HUE = "accent_hue"
        const val KEY_THEME = "theme_mode"
        const val KEY_STYLE = "theme_style"
        const val KEY_SKIN = "skin"
        const val KEY_DEPTH = "depth3d"
        const val KEY_TRANSPARENCY = "transparency"
        const val KEY_LAYOUT = "layout"
        const val KEY_COLORS = "customColors"
        const val KEY_UPDATES = "check_updates"
        const val KEY_SORT = "sort"
        const val KEY_CHECKED_BOTTOM = "checked_to_bottom"
        const val KEY_LIST_RIGHT = "list_on_right"
        const val KEY_SETTINGS_DOCK = "settings_dock"
    }
}
