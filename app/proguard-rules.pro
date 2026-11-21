# Glance creates widget action callbacks by reflection (actionRunCallback<T>()).
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}

# Glance finds a widget's content by its GlanceAppWidget class name; merged or renamed classes
# would draw one widget's content into another.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget

# Receivers, tiles and workers are created by the system from the manifest by name.
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.service.quicksettings.TileService
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# ML Kit finds its components by reflection (ComponentDiscovery): their registrars keep a public constructor.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class com.google.mlkit.** { <init>(); }

# WorkManager (Glance refreshes widgets through it) creates its input mergers by reflection from their class name;
# without this the widget stays on its loading spinner ("OverwritingInputMerger has no zero argument constructor").
-keep class * extends androidx.work.InputMerger { <init>(); }
