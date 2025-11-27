// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(sjy.plugins.android.application) apply false
    alias(sjy.plugins.android.library) apply false
    alias(sjy.plugins.kotlin.android) apply false
    alias(sjy.plugins.kotlin.compose) apply false
    alias(sjy.plugins.kotlin.serialization) apply false
    alias(sjy.plugins.ksp) apply false
    alias(sjy.plugins.detekt) apply true
    alias(sjy.plugins.ktorfit) apply false
    alias(sjy.plugins.gms.services) apply false
    alias(sjy.plugins.crashlytics) apply false
    alias(sjy.plugins.buildlogic.detekt) apply true
}
