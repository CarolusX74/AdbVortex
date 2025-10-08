plugins {
    id("org.jetbrains.intellij") version "1.17.3"
    kotlin("jvm") version "1.9.24"
}

group = "com.pensa.adbvortex"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

intellij {
    version.set("2023.3")               // Versión base de IntelliJ (compatible con Android Studio Iguana / Koala)
    pluginName.set("AdbVortex")
    type.set("IC")                      // "IC" para IntelliJ Community; "AI" para Android Studio
    downloadSources.set(false)
    updateSinceUntilBuild.set(false)    // evita advertencias si se abre en versiones nuevas
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        changeNotes.set("""
            <h3>AdbVortex 1.0.0</h3>
            <ul>
              <li>Initial public release</li>
              <li>Stable single-device ADB reverse + local proxy</li>
              <li>Animated UI indicator and logs view</li>
            </ul>
        """.trimIndent())
    }

    // 🧱 Construye el ZIP instalable
    buildPlugin {
        archiveBaseName.set("AdbVortex")
    }

    // 🧹 Limpieza antes del build
    clean {
        delete("build")
    }

    // 🧩 Ejecutar el plugin en entorno sandbox (para probar)
    runIde {
        jvmArgs("-Xmx2G")
    }
}
