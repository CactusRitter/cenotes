package com.cenotes.app.data

import android.content.Context
import com.cenotes.app.ui.theme.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NoteLayout { GRID, LIST }

data class AppSettings(
    val accent: Accent = Accent.INDIGO,
    /** 0 = solid glass, 100 = fully see-through. */
    val transparency: Int = 65,
    val layout: NoteLayout = NoteLayout.GRID,
    /** Colours the user added for labels (ARGB), at most [MaxCustomColors]. */
    val customColors: List<Int> = emptyList(),
)

const val MaxCustomColors = 10

/** Small app preferences, kept in SharedPreferences and exposed as a StateFlow. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun read() = AppSettings(
        accent = prefs.getString(KEY_ACCENT, null)
            ?.let { name -> Accent.entries.firstOrNull { it.name == name } } ?: Accent.INDIGO,
        transparency = prefs.getInt(KEY_TRANSPARENCY, 65),
        layout = prefs.getString(KEY_LAYOUT, null)
            ?.let { name -> NoteLayout.entries.firstOrNull { it.name == name } } ?: NoteLayout.GRID,
        customColors = prefs.getString(KEY_COLORS, null).orEmpty()
            .split(',').mapNotNull { it.toIntOrNull() }.take(MaxCustomColors),
    )

    private fun saveColors(colors: List<Int>) {
        val list = colors.distinct().take(MaxCustomColors)
        prefs.edit().putString(KEY_COLORS, list.joinToString(",")).apply()
        _state.value = _state.value.copy(customColors = list)
    }

    fun addCustomColor(color: Int) = saveColors(_state.value.customColors + color)

    fun replaceCustomColor(old: Int, new: Int) =
        saveColors(_state.value.customColors.map { if (it == old) new else it })

    fun removeCustomColor(color: Int) = saveColors(_state.value.customColors - color)

    fun setAccent(accent: Accent) {
        prefs.edit().putString(KEY_ACCENT, accent.name).apply()
        _state.value = _state.value.copy(accent = accent)
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
        const val KEY_TRANSPARENCY = "transparency"
        const val KEY_LAYOUT = "layout"
        const val KEY_COLORS = "customColors"
    }
}
