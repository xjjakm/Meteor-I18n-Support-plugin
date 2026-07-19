pluginManagement {
    repositories {
        // 阿里云镜像
        maven {
            name = "AliyunGradlePlugin"
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }

        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
