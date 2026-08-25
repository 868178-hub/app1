package com.example.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.model.GeoPoint
import com.example.data.model.Territory
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon as OsmPolygon
import org.osmdroid.views.overlay.Polyline as OsmPolyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import androidx.compose.ui.draw.rotate

enum class OsmMapStyle(val label: String, val desc: String) {
    STREET_MAP("Callejero Real", "OpenStreetMap estándar con todas las calles y edificios"),
    SATELLITE("Vista Satélite HD", "Fotografía aérea y satelital de alta resolución (Esri/ArcGIS)"),
    DARK_TACTICAL("Modo Oscuro Táctico", "CartoDB Dark Matter con contraste para correr de noche"),
    CLEAN_VOYAGER("Modo Deportivo", "CartoDB Voyager de alta visibilidad para rutas"),
    TOPO_TERRAIN("Topográfico", "OpenTopoMap con curvas de nivel y caminos de tierra")
}

// Satellite Aerial imagery source (Esri World Imagery)
private val EsriSatelliteTileSource: ITileSource = XYTileSource(
    "EsriSatellite",
    0, 19, 256, ".jpg",
    arrayOf(
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
    ),
    "© Esri, Maxar, Earthstar Geographics, and the GIS User Community"
)

// Custom tile sources for dark mode and clean voyager
private val CartoDarkTileSource: ITileSource = XYTileSource(
    "CartoDark",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
        "https://d.basemaps.cartocdn.com/dark_all/"
    ),
    "© OpenStreetMap contributors, © CARTO"
)

private val CartoVoyagerTileSource: ITileSource = XYTileSource(
    "CartoVoyager",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
    ),
    "© OpenStreetMap contributors, © CARTO"
)

@Composable
fun TerritoryMapView(
    currentLocation: GeoPoint,
    activeTrail: List<GeoPoint>,
    territories: List<Territory>,
    userSignatureColor: Color,
    isStealthActive: Boolean,
    modifier: Modifier = Modifier,
    onRecenterClicked: (() -> Unit)? = null,
    onMapTapped: ((GeoPoint) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapStyle by remember { mutableStateOf(OsmMapStyle.DARK_TACTICAL) }
    var isAutoFollowUser by remember { mutableStateOf(true) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var mapOrientationDegrees by remember { mutableStateOf(0f) }

    // Keep reference to MapView for programmatic controls (zoom, pan, overlays)
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    val trailPolylineRef = remember { mutableStateOf<OsmPolyline?>(null) }
    val runnerMarkerRef = remember { mutableStateOf<Marker?>(null) }

    // Lifecycle handling for OSMDroid MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef.value?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef.value?.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef.value?.onDetach()
        }
    }

    // Auto-center on user GPS when currentLocation changes if auto-follow is active
    LaunchedEffect(currentLocation, isAutoFollowUser) {
        mapViewRef.value?.let { map ->
            val userOsmPoint = OsmGeoPoint(currentLocation.lat, currentLocation.lng)
            if (isAutoFollowUser) {
                map.controller.animateTo(userOsmPoint)
            }
            // Update Runner Marker
            runnerMarkerRef.value?.let { marker ->
                marker.position = userOsmPoint
                map.invalidate()
            }
        }
    }

    // Update active trail polyline dynamically
    LaunchedEffect(activeTrail, userSignatureColor) {
        mapViewRef.value?.let { map ->
            val polyline = trailPolylineRef.value ?: return@let
            val osmPoints = activeTrail.map { OsmGeoPoint(it.lat, it.lng) }
            polyline.setPoints(osmPoints)
            val argbColor = userSignatureColor.toArgb()
            polyline.outlinePaint.color = argbColor
            polyline.outlinePaint.strokeWidth = 12f
            polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
            polyline.outlinePaint.strokeJoin = Paint.Join.ROUND
            polyline.outlinePaint.isAntiAlias = true
            map.invalidate()
        }
    }

    // Update territories overlays
    LaunchedEffect(territories) {
        mapViewRef.value?.let { map ->
            updateTerritoryPolygons(map, territories)
        }
    }

    // Update tile style when selected
    LaunchedEffect(mapStyle) {
        mapViewRef.value?.let { map ->
            val tileSource = when (mapStyle) {
                OsmMapStyle.STREET_MAP -> TileSourceFactory.MAPNIK
                OsmMapStyle.SATELLITE -> EsriSatelliteTileSource
                OsmMapStyle.DARK_TACTICAL -> CartoDarkTileSource
                OsmMapStyle.CLEAN_VOYAGER -> CartoVoyagerTileSource
                OsmMapStyle.TOPO_TERRAIN -> TileSourceFactory.OpenTopo
            }
            map.setTileSource(tileSource)
            map.invalidate()
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("territory_map_osm_container")) {
        // Native OpenStreetMap View with AndroidView
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("osmdroid_map_view"),
            factory = { ctx ->
                // Ensure configuration loaded
                try {
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE))
                    Configuration.getInstance().userAgentValue = ctx.packageName
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                MapView(ctx).apply {
                    setDestroyMode(false)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    minZoomLevel = 5.0
                    maxZoomLevel = 20.0

                    // Initial tile source
                    val initTileSource = when (mapStyle) {
                        OsmMapStyle.STREET_MAP -> TileSourceFactory.MAPNIK
                        OsmMapStyle.SATELLITE -> EsriSatelliteTileSource
                        OsmMapStyle.DARK_TACTICAL -> CartoDarkTileSource
                        OsmMapStyle.CLEAN_VOYAGER -> CartoVoyagerTileSource
                        OsmMapStyle.TOPO_TERRAIN -> TileSourceFactory.OpenTopo
                    }
                    setTileSource(initTileSource)

                    // Initial center & zoom
                    controller.setZoom(17.5)
                    val startPoint = OsmGeoPoint(currentLocation.lat, currentLocation.lng)
                    controller.setCenter(startPoint)

                    // Touch listener to stop auto-following if the user manually drags the map
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            mapOrientationDegrees = mapOrientation
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            mapOrientationDegrees = mapOrientation
                            return false
                        }
                    })

                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_MOVE) {
                            if (isAutoFollowUser) {
                                isAutoFollowUser = false
                            }
                            mapOrientationDegrees = mapOrientation
                        }
                        false
                    }

                    // Enable 2-Finger Map Rotation
                    val rotationGestureOverlay = RotationGestureOverlay(this).apply {
                        isEnabled = true
                    }
                    overlays.add(rotationGestureOverlay)

                    // Create Active Trail Polyline
                    val activePolyline = OsmPolyline(this).apply {
                        outlinePaint.color = userSignatureColor.toArgb()
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                        outlinePaint.isAntiAlias = true
                    }
                    trailPolylineRef.value = activePolyline
                    overlays.add(activePolyline)

                    // Create Runner GPS Marker
                    val runnerMarker = Marker(this).apply {
                        position = startPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = if (isStealthActive) "Sigilo Activo (GPS Cifrado)" else "Tu Posición"
                        snippet = "Lat: %.5f, Lng: %.5f".format(currentLocation.lat, currentLocation.lng)
                        icon = createRunnerMarkerDrawable(ctx, userSignatureColor.toArgb())
                    }
                    runnerMarkerRef.value = runnerMarker
                    overlays.add(runnerMarker)

                    // Populate initial territory polygons
                    updateTerritoryPolygons(this, territories)

                    mapViewRef.value = this
                }
            },
            update = { map ->
                // Update runner marker icon/title
                runnerMarkerRef.value?.let { marker ->
                    marker.title = if (isStealthActive) "Sigilo Activo (GPS Cifrado)" else "Tu Posición"
                    marker.icon = createRunnerMarkerDrawable(context, userSignatureColor.toArgb())
                }
            }
        )

        // Floating Control Panel (Top-Right: Layers, Recenter, Zoom In/Out)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Map Layer Selector Button
            Box {
                Surface(
                    shape = CircleShape,
                    color = DarkSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = { showLayerMenu = true },
                        modifier = Modifier.fillMaxSize().testTag("btn_toggle_map_layers")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Cambiar estilo de mapa",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showLayerMenu,
                    onDismissRequest = { showLayerMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    OsmMapStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (style) {
                                                OsmMapStyle.STREET_MAP -> Icons.Default.Map
                                                OsmMapStyle.SATELLITE -> Icons.Default.Public
                                                OsmMapStyle.DARK_TACTICAL -> Icons.Default.DarkMode
                                                OsmMapStyle.CLEAN_VOYAGER -> Icons.Default.MyLocation
                                                OsmMapStyle.TOPO_TERRAIN -> Icons.Default.Terrain
                                            },
                                            contentDescription = null,
                                            tint = if (mapStyle == style) NeonCyan else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = style.label,
                                            color = if (mapStyle == style) NeonCyan else TextPrimary,
                                            fontWeight = if (mapStyle == style) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        text = style.desc,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                            },
                            onClick = {
                                mapStyle = style
                                showLayerMenu = false
                            }
                        )
                    }
                }
            }

            // Interactive Mini Compass (Points to North, tap to reset 0° North)
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.2.dp, if (Math.abs(mapOrientationDegrees) > 1f) NeonAmber else DarkBorder),
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        mapViewRef.value?.let { map ->
                            map.setMapOrientation(0f, true)
                            mapOrientationDegrees = 0f
                        }
                    },
                    modifier = Modifier.fillMaxSize().testTag("btn_compass_north")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Brújula Táctica (Apuntar al Norte)",
                            tint = if (Math.abs(mapOrientationDegrees) > 1f) NeonCoral else NeonCyan,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(-mapOrientationDegrees)
                        )
                        Text(
                            text = "N",
                            color = NeonCoral,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // Recenter / Follow GPS Button
            Surface(
                shape = CircleShape,
                color = if (isAutoFollowUser) NeonCyan else DarkSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, if (isAutoFollowUser) NeonCyan else DarkBorder),
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        isAutoFollowUser = true
                        onRecenterClicked?.invoke()
                        mapViewRef.value?.let { map ->
                            val userPoint = OsmGeoPoint(currentLocation.lat, currentLocation.lng)
                            map.controller.animateTo(userPoint)
                            map.controller.setZoom(17.5)
                        }
                    },
                    modifier = Modifier.fillMaxSize().testTag("btn_recenter_gps")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Centrar en mi ubicación GPS",
                        tint = if (isAutoFollowUser) DarkSurface else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Zoom In Button (+)
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, DarkBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        mapViewRef.value?.controller?.zoomIn()
                    },
                    modifier = Modifier.fillMaxSize().testTag("btn_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Acercar mapa",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Zoom Out Button (-)
            Surface(
                shape = CircleShape,
                color = DarkSurface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, DarkBorder),
                shadowElevation = 4.dp,
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = {
                        mapViewRef.value?.controller?.zoomOut()
                    },
                    modifier = Modifier.fillMaxSize().testTag("btn_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Alejar mapa",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Live Real-Time Cartography Watermark Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurface.copy(alpha = 0.82f),
            border = BorderStroke(0.5.dp, DarkBorder),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 128.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(NeonLime, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "OpenStreetMap • Callejero Real GPS",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Helper to render and refresh conquered territory polygons on the OpenStreetMap
private fun updateTerritoryPolygons(mapView: MapView, territories: List<Territory>) {
    // Remove existing polygons while preserving base overlays (trail, runner marker)
    val toRemove = mapView.overlays.filterIsInstance<OsmPolygon>()
    mapView.overlays.removeAll(toRemove)

    // Add updated polygons
    for (territory in territories) {
        if (territory.points.size < 3) continue

        val osmPolygon = OsmPolygon(mapView).apply {
            val osmPoints = territory.points.map { OsmGeoPoint(it.lat, it.lng) }
            points = osmPoints

            val baseColor = try {
                AndroidColor.parseColor(territory.ownerColorHex)
            } catch (e: Exception) {
                AndroidColor.CYAN
            }

            // Fill translucent area
            val alphaFill = AndroidColor.argb(
                75,
                AndroidColor.red(baseColor),
                AndroidColor.green(baseColor),
                AndroidColor.blue(baseColor)
            )
            fillPaint.color = alphaFill
            fillPaint.style = Paint.Style.FILL

            // Outer Neon Border
            outlinePaint.color = baseColor
            outlinePaint.strokeWidth = 6f
            outlinePaint.style = Paint.Style.STROKE
            outlinePaint.isAntiAlias = true
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND

            title = "${territory.ownerName} • ${territory.formattedArea}"
            snippet = territory.name
        }

        // Insert behind the runner marker and active trail
        mapView.overlays.add(0, osmPolygon)
    }

    mapView.invalidate()
}

// Helper to generate a custom high-tech pulsing GPS Runner icon
private fun createRunnerMarkerDrawable(context: Context, colorArgb: Int): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setSize(42, 42)
        setColor(colorArgb)
        setStroke(6, AndroidColor.WHITE)
    }
}
