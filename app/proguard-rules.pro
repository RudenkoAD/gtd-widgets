# QuickJS wrapper вызывается через JNI — имена классов/методов моста трогать нельзя.
-keep class com.whl.quickjs.wrapper.** { *; }
-keep class com.whl.quickjs.android.** { *; }

# kotlinx.serialization: сгенерированные сериализаторы + аннотации моделей.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.gtdflow.widget.engine.** {
    *** Companion;
}
-keepclasseswithmembers class com.gtdflow.widget.engine.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gtdflow.widget.engine.**$$serializer { *; }

# Glance/WorkManager используют рефлексию по именам ресиверов/воркеров.
-keep class com.gtdflow.widget.**Receiver { *; }
-keep class com.gtdflow.widget.work.** { *; }
