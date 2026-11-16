# Keep kotlinx.serialization metadata for type-safe navigation routes.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.inkflow.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
