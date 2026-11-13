package com.cenotes.app.ui.theme

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

val CheckBoxIcon: ImageVector by lazy {
    pathIcon(
        "CheckBox",
        "M19,3H5C3.89,3 3,3.9 3,5v14c0,1.1 0.89,2 2,2h14c1.11,0 2,-0.9 2,-2V5C21,3.9 20.11,3 19,3z" +
            "M10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8L10,17z",
    )
}

val CheckBoxOutlineIcon: ImageVector by lazy {
    pathIcon(
        "CheckBoxOutline",
        "M19,5v14H5V5H19M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3L19,3z",
    )
}

val BulletListIcon: ImageVector by lazy {
    pathIcon(
        "BulletList",
        "M4,10.5c-0.83,0 -1.5,0.67 -1.5,1.5s0.67,1.5 1.5,1.5 1.5,-0.67 1.5,-1.5 -0.67,-1.5 -1.5,-1.5z" +
            "M4,4.5c-0.83,0 -1.5,0.67 -1.5,1.5S3.17,7.5 4,7.5 5.5,6.83 5.5,6 4.83,4.5 4,4.5z" +
            "M4,16.5c-0.83,0 -1.5,0.68 -1.5,1.5s0.68,1.5 1.5,1.5 1.5,-0.68 1.5,-1.5 -0.67,-1.5 -1.5,-1.5z" +
            "M7,19h14v-2H7v2zM7,13h14v-2H7v2zM7,5v2h14V5H7z",
    )
}

val NumberedListIcon: ImageVector by lazy {
    pathIcon(
        "NumberedList",
        "M2,17h2v0.5H3v1h1v0.5H2v1h3v-4H2v1zM3,8h1V4H2v1h1v3zM2,11h1.8L2,13.1v0.9h3v-1H3.2L5,10.9V10H2v1z" +
            "M7,5v2h14V5H7zM7,19h14v-2H7v2zM7,13h14v-2H7v2z",
    )
}

val QuoteIcon: ImageVector by lazy {
    pathIcon("Quote", "M6,17h3l2,-4V7H5v6h3zM14,17h3l2,-4V7h-6v6h3z")
}

val VisibilityIcon: ImageVector by lazy {
    pathIcon(
        "Visibility",
        "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5z" +
            "M12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z" +
            "M12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z",
    )
}

val IndentIcon: ImageVector by lazy {
    pathIcon(
        "Indent",
        "M3,21h18v-2H3V21zM3,8v8l4,-4L3,8zM11,17h10v-2H11V17zM3,3v2h18V3H3zM11,9h10V7H11V9zM11,13h10v-2H11V13z",
    )
}

val OutdentIcon: ImageVector by lazy {
    pathIcon(
        "Outdent",
        "M11,17h10v-2H11V17zM3,12l4,4V8L3,12zM3,21h18v-2H3V21zM3,3v2h18V3H3zM11,9h10V7H11V9zM11,13h10v-2H11V13z",
    )
}

val LinkIcon: ImageVector by lazy {
    pathIcon(
        "Link",
        "M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7c-1.71,0 -3.1,-1.39 -3.1,-3.1z" +
            "M8,13h8v-2H8v2zM17,7h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5s-2.24,-5 -5,-5z",
    )
}

val ImageIcon: ImageVector by lazy {
    pathIcon(
        "Image",
        "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2z" +
            "M8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z",
    )
}

val CameraIcon: ImageVector by lazy {
    pathIcon(
        "Camera",
        "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0" +
            "M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9z" +
            "M12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
    )
}

val OpenInNewIcon: ImageVector by lazy {
    pathIcon(
        "OpenInNew",
        "M19,19H5V5h7V3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2v7z" +
            "M14,3v2h3.59l-9.83,9.83 1.41,1.41L19,6.41V10h2V3h-7z",
    )
}

val BackupIcon: ImageVector by lazy {
    pathIcon(
        "Backup",
        "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13" +
            "c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96zM14,13v4h-4v-4H7l5,-5 5,5h-3z",
    )
}

val LabelIcon: ImageVector by lazy {
    pathIcon(
        "Label",
        "M17.63,5.84C17.27,5.33 16.67,5 16,5L5,5.01C3.9,5.01 3,5.9 3,7v10c0,1.1 0.9,1.99 2,1.99L16,19" +
            "c0.67,0 1.27,-0.33 1.63,-0.84L22,12l-4.37,-6.16z",
    )
}

/** Hollow label: a note without labels (like the hollow star of a note that isn't a favourite). */
val LabelOutlineIcon: ImageVector by lazy {
    pathIcon(
        "LabelOutline",
        "M17.63,5.84C17.27,5.33 16.67,5 16,5L5,5.01C3.9,5.01 3,5.9 3,7v10c0,1.1 0.9,1.99 2,1.99L16,19" +
            "c0.67,0 1.27,-0.33 1.63,-0.84L22,12l-4.37,-6.16zM16,17H5V7h11l3.55,5L16,17z",
    )
}

val ArchiveIcon: ImageVector by lazy {
    pathIcon(
        "Archive",
        "M20.54,5.23l-1.39,-1.68C18.88,3.21 18.47,3 18,3H6c-0.47,0 -0.88,0.21 -1.16,0.55L3.46,5.23" +
            "C3.17,5.57 3,6.02 3,6.5V19c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V6.5c0,-0.48 -0.17,-0.93 -0.46,-1.27z" +
            "M12,17.5L6.5,12H10v-2h4v2h3.5L12,17.5zM5.12,5l0.81,-1h12l0.94,1H5.12z",
    )
}

val UnarchiveIcon: ImageVector by lazy {
    pathIcon(
        "Unarchive",
        "M20.55,5.22l-1.39,-1.68C18.88,3.21 18.47,3 18,3H6c-0.47,0 -0.88,0.21 -1.15,0.55L3.46,5.22" +
            "C3.17,5.57 3,6.01 3,6.5V19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6.5c0,-0.49 -0.17,-0.93 -0.45,-1.28z" +
            "M12,9.5l5.5,5.5H14v2h-4v-2H6.5L12,9.5zM5.12,5l0.82,-1h12l0.93,1H5.12z",
    )
}

val NotesIcon: ImageVector by lazy {
    pathIcon("Notes", "M3,18h12v-2H3v2zM3,6v2h18V6H3zM3,13h18v-2H3v2z")
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

val DownloadIcon: ImageVector by lazy {
    pathIcon("Download", "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z")
}

/** A gear with an arrow: the backup of the settings (the notes have [BackupIcon]). */
val SettingsBackupIcon: ImageVector by lazy {
    pathIcon(
        "SettingsBackupRestore",
        "M14,12c0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2 0.9,2 2,2 2,-0.9 2,-2zM12,3c-4.97,0 -9,4.03 -9,9H0l4,4 4,-4H5" +
            "c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.51,0 -2.91,-0.49 -4.06,-1.3l-1.42,1.44" +
            "C8.04,20.3 9.94,21 12,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9z",
    )
}
