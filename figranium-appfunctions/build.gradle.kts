plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "dev.figranium.sdk.appfunctions"
    compileSdk = 37

    defaultConfig { minSdk = 23 }
}

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":figranium"))
    api(libs.appfunctions)
    implementation(libs.serialization.json)
    ksp(libs.appfunctions.compiler)
}

mavenPublishing {
    coordinates("dev.figranium", "figranium-appfunctions", providers.gradleProperty("VERSION_NAME").get())
    publishToMavenCentral()
    signAllPublications()
    pom {
        name.set("Figranium AppFunctions")
        description.set("Android AppFunctions integration for the Figranium Kotlin SDK.")
        inceptionYear.set("2026")
        url.set("https://figranium.dev")
        licenses { license { name.set("Apache-2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
        developers { developer { id.set("figranium"); name.set("Figranium"); url.set("https://github.com/figranium") } }
        scm { url.set("https://github.com/figranium/figranium-kotlin"); connection.set("scm:git:git://github.com/figranium/figranium-kotlin.git"); developerConnection.set("scm:git:ssh://git@github.com/figranium/figranium-kotlin.git") }
    }
}
