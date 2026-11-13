package com.cenotes.app.data

import android.content.Context
import com.cenotes.app.ui.theme.Accent
import com.cenotes.app.ui.theme.AccentPalette
import com.cenotes.app.ui.theme.Skin
import com.cenotes.app.ui.theme.ThemeMode
import com.cenotes.app.ui.theme.ThemeStyle
import com.cenotes.app.ui.theme.customPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class NoteLayout { GRID, LIST }

/** How the notes are ordered (pinned ones always go first). */
enum class NoteSort { EDITED, CREATED, TITLE }

data class AppSettings(
    val accent: Accent = Accent.INDIGO,
    /** A colour of the free hue bar (0–360) instead of [accent]; null = the chosen accent. */
    val customHue: Float? = null,
    /** Light, dark or following the phone. */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Background and glass tint of the glass look (only used with [Skin.GLASS]). */
    val themeStyle: ThemeStyle = ThemeStyle.AURORA,
    /** The whole look of the app; [Skin.GLASS] is the one shared with Celander and Cemail. */
    val skin: Skin = Skin.GLASS,
    /** Solid "3D" shadow under every glass surface. */
    val depth3d: Boolean = false,
    /** 0 = solid glass, 100 = fully see-through. */
    val transparency: Int = 65,
    val layout: NoteLayout = NoteLayout.GRID,
    /** Colours the user added for labels (ARGB), at most [MaxCustomColors]. */
    val customColors: List<Int> = emptyList(),
    /** Ask GitHub once or twice a day whether there is a newer Cenotes. */
    val checkUpdates: Boolean = true,
    val sort: NoteSort = NoteSort.EDITED,
    /** Ticked items of a checklist sink to the end of their list (like Google Keep). */
    val checkedToBottom: Boolean = true,
) {
    /** The app's colour in its four shades: the custom one, or the chosen accent. */
    val accentPalette: AccentPalette
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

    private fun read() = AppSettings(
        accent = enumPref(KEY_ACCENT, Accent.INDIGO),
        customHue = prefs.getFloat(KEY_HUE, -1f).takeIf { it >= 0f },
        themeMode = enumPref(KEY_THEME, ThemeMode.SYSTEM),
        themeStyle = enumPref(KEY_STYLE, ThemeStyle.AURORA),
        skin = enumPref(KEY_SKIN, Skin.GLASS),
        depth3d = prefs.getBoolean(KEY_DEPTH, false),
        transparency = prefs.getInt(KEY_TRANSPARENCY, 65),
        layout = enumPref(KEY_LAYOUT, NoteLayout.GRID),
        customColors = prefs.getString(KEY_COLORS, null).orEmpty()
            .split(',').mapNotNull { it.toIntOrNull() }.take(MaxCustomColors),
        checkUpdates = prefs.getBoolean(KEY_UPDATES, true),
        sort = enumPref(KEY_SORT, NoteSort.EDITED),
        checkedToBottom = prefs.getBoolean(KEY_CHECKED_BOTTOM, true),
    )

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
    fun exportJson(): String {
        val values = JSONObject()
        prefs.all.forEach { (key, value) ->
            val typed = when (value) {
                is Boolean -> JSONObject().put("b", value)
                is Int -> JSONObject().put("i", value)
                is Long -> JSONObject().put("l", value)
                is Float -> JSONObject().put("f", value.toDouble())
                is String -> JSONObject().put("s", value)
                is Set<*> -> JSONObject().put("set", JSONArray(value.filterIsInstance<String>()))
                else -> null
            }
            if (typed != null) values.put(key, typed)
        }
        return JSONObject().put("app", "cenotes").put("format", 1).put("settings", values).toString(2)
    }

    /** Applies a backup made by [exportJson]; returns false (and changes nothing) if [text] is not one. */
    fun importJson(text: String): Boolean {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (root.optString("app") != "cenotes") return false
        val values = root.optJSONObject("settings") ?: return false
        val edit = prefs.edit()
        values.keys().forEach { key ->
            val typed = values.optJSONObject(key) ?: return@forEach
            when {
                typed.has("b") -> edit.putBoolean(key, typed.getBoolean("b"))
                typed.has("i") -> edit.putInt(key, typed.getInt("i"))
                typed.has("l") -> edit.putLong(key, typed.getLong("l"))
                typed.has("f") -> edit.putFloat(key, typed.getDouble("f").toFloat())
                typed.has("s") -> edit.putString(key, typed.getString("s"))
                typed.has("set") -> typed.getJSONArray("set").let { list ->
                    edit.putStringSet(key, (0 until list.length()).map { list.getString(it) }.toSet())
                }
            }
        }
        edit.apply()
        _state.value = read()
        return true
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

    private companion object {
        const val KEY_ACCENT = "accent"
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
    }
}
