// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 全工程插件与依赖版本统一由 gradle/libs.versions.toml 集中声明与管理
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.protobuf) apply false
}