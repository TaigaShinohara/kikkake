// NOTE: this VM uses local mirror (see uroboe). On normal machine replace with google()/mavenCentral()/gradlePluginPortal().
pluginManagement {
    repositories {
        maven { url = uri("http://127.0.0.1:18080/google"); isAllowInsecureProtocol = true }
        maven { url = uri("http://127.0.0.1:18080/central"); isAllowInsecureProtocol = true }
        maven { url = uri("http://127.0.0.1:18080/plugins"); isAllowInsecureProtocol = true }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("http://127.0.0.1:18080/google"); isAllowInsecureProtocol = true }
        maven { url = uri("http://127.0.0.1:18080/central"); isAllowInsecureProtocol = true }
    }
}
rootProject.name = "Kikkake"
include(":app")
