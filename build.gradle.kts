plugins {
    alias(sjy.plugins.android.application) apply false
    alias(sjy.plugins.android.library) apply false
    alias(sjy.plugins.compose.multiplatform) apply false
    alias(sjy.plugins.kotlin.compose) apply false
    alias(sjy.plugins.kotlin.multiplatform) apply false
    alias(sjy.plugins.ksp) apply false
    alias(sjy.plugins.detekt) apply true
    alias(sjy.plugins.ktorfit) apply false
    alias(sjy.plugins.kotlin.serialization) apply false
    alias(sjy.plugins.android.kotlin.multiplatform.library) apply false
    alias(sjy.plugins.android.lint) apply false
    alias(libs.plugins.composeHotReload) apply false
}
