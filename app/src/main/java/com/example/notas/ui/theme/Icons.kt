package com.example.notas.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Builds a 24dp icon from Material SVG path data (for icons not in material-icons-core). */
private fun pathIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()

val StarBorderIcon: ImageVector by lazy {
    pathIcon(
        "StarBorder",
        "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24z" +
            "M12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
    )
}

val GridViewIcon: ImageVector by lazy {
    pathIcon(
        "GridView",
        "M3,3v8h8V3H3zM9,9H5V5h4V9zM3,13v8h8v-8H3zM9,19H5v-4h4V19zM13,3v8h8V3H13zM19,9h-4V5h4V9z" +
            "M13,13v8h8v-8H13zM19,19h-4v-4h4V19z",
    )
}

val ViewAgendaIcon: ImageVector by lazy {
    pathIcon(
        "ViewAgenda",
        "M19,13H5c-1.1,0-2,0.9-2,2v4c0,1.1,0.9,2,2,2h14c1.1,0,2-0.9,2-2v-4C21,13.9,20.1,13,19,13zM19,19H5v-4h14V19z" +
            "M19,3H5C3.9,3,3,3.9,3,5v4c0,1.1,0.9,2,2,2h14c1.1,0,2-0.9,2-2V5C21,3.9,20.1,3,19,3zM19,9H5V5h14V9z",
    )
}

val PaletteIcon: ImageVector by lazy {
    pathIcon(
        "Palette",
        "M12,3c-4.97,0-9,4.03-9,9s4.03,9,9,9c0.83,0,1.5-0.67,1.5-1.5c0-0.39-0.15-0.74-0.39-1.01" +
            "c-0.23-0.26-0.38-0.61-0.38-0.99c0-0.83,0.67-1.5,1.5-1.5H16c2.76,0,5-2.24,5-5C21,6.58,16.97,3,12,3z" +
            "M6.5,12C5.67,12,5,11.33,5,10.5S5.67,9,6.5,9S8,9.67,8,10.5S7.33,12,6.5,12z" +
            "M9.5,8C8.67,8,8,7.33,8,6.5S8.67,5,9.5,5S11,5.67,11,6.5S10.33,8,9.5,8z" +
            "M14.5,8C13.67,8,13,7.33,13,6.5S13.67,5,14.5,5S16,5.67,16,6.5S15.33,8,14.5,8z" +
            "M17.5,12c-0.83,0-1.5-0.67-1.5-1.5S16.67,9,17.5,9S19,9.67,19,10.5S18.33,12,17.5,12z",
    )
}
