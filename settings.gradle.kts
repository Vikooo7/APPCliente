pluginManagement {
    repositories {
        // Copia local de KSP (81 MB) descargada aparte: evita volver a bajarla con conexión lenta.
        mavenLocal {
            content { includeModule("com.google.devtools.ksp", "symbol-processing-aa-embeddable") }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal {
            content { includeModule("com.google.devtools.ksp", "symbol-processing-aa-embeddable") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "RutaLog Cliente"
include(":app")
