package com.cenotes.app.ui

import com.ce.kit.ui.AccentButton
import com.ce.kit.ui.SoftButton
import com.ce.kit.ui.settings.ChoiceChip
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cenotes.app.R
import com.cenotes.app.data.Place
import com.cenotes.app.notify.Places
import com.cenotes.app.notify.await
import com.ce.kit.ui.glass.GlassKind
import com.ce.kit.ui.glass.glass
import com.ce.kit.ui.glass.popIn
import com.ce.kit.ui.theme.LocalSkin
import com.cenotes.app.ui.theme.MyLocationIcon
import com.cenotes.app.ui.theme.PlaceIcon
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** A place found by its address or name (not saved yet). */
private data class Found(val name: String, val detail: String, val latitude: Double, val longitude: Double)

private enum class PlaceStep { LIST, NAME, PERMISSION }

/**
 * "At a place": on arriving or on leaving, then the place — one already saved, "I'm here", or an address or name looked
 * up on the device (Android's own geocoder: no map, no account, nothing sent by Cenotes). A new place is saved with a
 * name to use again. If Android does not yet let Cenotes know the location while it is closed, the last step says why
 * it is needed and opens the setting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceDialog(
    placeId: Long?,
    leave: Boolean,
    places: List<Place>,
    onSave: suspend (name: String, latitude: Double, longitude: Double) -> Long,
    onDelete: (Long) -> Unit,
    onSet: (placeId: Long?, leave: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var onLeave by remember { mutableStateOf(leave) }
    var step by remember { mutableStateOf(PlaceStep.LIST) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Found>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<Int?>(null) }
    var chosen by remember { mutableStateOf<Found?>(null) }
    var name by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Long?>(null) }

    // Done choosing: straight away when Android already allows it, otherwise after the step that explains the setting.
    fun finish(id: Long) {
        picked = id
        if (Places.allowed(context, background = true)) onSet(id, onLeave) else step = PlaceStep.PERMISSION
    }

    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { picked?.let { onSet(it, onLeave) } }
    val foreground = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it } && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            picked?.let { onSet(it, onLeave) }
        }
    }
    fun askPermission() {
        if (Places.allowed(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else picked?.let { onSet(it, onLeave) }
        } else {
            foreground.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // "I'm here": the location needs the permission while Cenotes is open; asked right there.
    fun here() {
        busy = true
        problem = null
        scope.launch {
            val found = runCatching { currentPlace(context) }.getOrNull()
            busy = false
            if (found == null) {
                problem = R.string.place_here_failed
            } else {
                chosen = found
                name = found.name
                step = PlaceStep.NAME
            }
        }
    }
    val hereLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) here() else problem = R.string.place_no_permission
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        busy = true
        problem = null
        scope.launch {
            val found = runCatching { findPlaces(context, q) }.getOrDefault(emptyList())
            busy = false
            results = found
            if (found.isEmpty()) problem = R.string.place_not_found
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .popIn(initialScale = 0.75f)
                .fillMaxWidth()
                .glass(RoundedCornerShape(LocalSkin.current.radius(28.dp)), GlassKind.Menu, elevation = 12.dp)
                .padding(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.place_title), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            when (step) {
                PlaceStep.LIST -> {
                    FlowRow(
                        Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip(stringResource(R.string.place_arrive), !onLeave) { onLeave = false }
                        ChoiceChip(stringResource(R.string.place_leave), onLeave) { onLeave = true }
                    }
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        places.forEach { p ->
                            PlaceRow(PlaceIcon, p.name, selected = p.id == placeId, onDelete = { onDelete(p.id) }) { finish(p.id) }
                        }
                        PlaceRow(MyLocationIcon, stringResource(R.string.place_here)) {
                            if (Places.allowed(context)) here()
                            else hereLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                        com.ce.kit.ui.GlassTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = stringResource(R.string.place_search_hint),
                            trailingIcon = {
                                IconButton(onClick = ::search) { Icon(Icons.Filled.Search, stringResource(R.string.place_search)) }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { search() }),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                        if (busy) {
                            CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                        }
                        problem?.let {
                            Text(
                                stringResource(it), Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                            )
                        }
                        results.orEmpty().forEach { f ->
                            PlaceRow(PlaceIcon, f.name, detail = f.detail) {
                                chosen = f
                                name = f.name
                                step = PlaceStep.NAME
                            }
                        }
                        if (placeId != null) {
                            PlaceRow(PlaceIcon, stringResource(R.string.place_remove), danger = true) { onSet(null, false) }
                        }
                    }
                }
                PlaceStep.NAME -> {
                    chosen?.let {
                        Text(
                            it.detail, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    com.ce.kit.ui.GlassTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = stringResource(R.string.place_name),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                        SoftButton(stringResource(R.string.back), { step = PlaceStep.LIST })
                        Spacer(Modifier.width(8.dp))
                        AccentButton(stringResource(R.string.place_save), {
                            val f = chosen ?: return@AccentButton
                            scope.launch { finish(onSave(name, f.latitude, f.longitude)) }
                        })
                    }
                }
                PlaceStep.PERMISSION -> {
                    Text(
                        stringResource(R.string.place_permission_body),
                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                        SoftButton(stringResource(R.string.place_permission_later), { picked?.let { onSet(it, onLeave) } })
                        Spacer(Modifier.width(8.dp))
                        AccentButton(stringResource(R.string.place_permission_open), ::askPermission)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String? = null,
    selected: Boolean = false,
    danger: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) c.error else c.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label, style = MaterialTheme.typography.bodyLarge, color = if (danger) c.error else c.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, stringResource(R.string.place_forget), tint = c.onSurfaceVariant) }
        }
    }
}

/** The small chip of a note's place reminder (on its card and in the editor). */
@Composable
fun PlaceChip(name: String, leave: Boolean, modifier: Modifier = Modifier, iconSize: Dp = 14.dp) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(LocalSkin.current.shape(CircleShape))
            .background(c.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PlaceIcon, null, Modifier.size(iconSize), tint = c.primary)
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(if (leave) R.string.place_chip_leave else R.string.place_chip_arrive, name),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = c.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- Finding places, all on the device ----

private suspend fun geocode(context: Context, block: Geocoder.(Geocoder.GeocodeListener) -> Unit, legacy: Geocoder.() -> List<Address>?): List<Address> =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext emptyList()
        val g = Geocoder(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                g.block(object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) = cont.resume(emptyList())
                })
            }
        } else {
            g.legacy().orEmpty()
        }
    }

private fun Address.toFound(fallback: String): Found {
    val lines = (0..maxAddressLineIndex).mapNotNull { getAddressLine(it) }
    val title = featureName?.takeIf { it.isNotBlank() && it.any(Char::isLetter) } ?: thoroughfare ?: lines.firstOrNull() ?: fallback
    return Found(title, lines.joinToString(", "), latitude, longitude)
}

/** Addresses and places matching [query] (Android's geocoder, which Google Play services provides). */
@Suppress("DEPRECATION")
private suspend fun findPlaces(context: Context, query: String): List<Found> =
    geocode(context, { getFromLocationName(query, 6, it) }, { getFromLocationName(query, 6) }).map { it.toFound(query) }

/** Where the device is now, with the address there as its suggested name. */
@SuppressLint("MissingPermission") // asked for before this is called
@Suppress("DEPRECATION")
private suspend fun currentPlace(context: Context): Found? {
    val location = LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() ?: return null
    val address = runCatching {
        geocode(context, { getFromLocation(location.latitude, location.longitude, 1, it) }, { getFromLocation(location.latitude, location.longitude, 1) })
    }.getOrDefault(emptyList()).firstOrNull()
    val fallback = context.getString(R.string.place_here_name)
    return address?.toFound(fallback)?.copy(latitude = location.latitude, longitude = location.longitude)
        ?: Found(fallback, "%.5f, %.5f".format(location.latitude, location.longitude), location.latitude, location.longitude)
}
