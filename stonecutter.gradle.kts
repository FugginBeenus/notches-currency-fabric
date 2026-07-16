plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.20.1"

// `./gradlew chiseledBuild` builds every supported version in one go.
tasks.register("chiseledBuild") {
    group = "project"
    dependsOn(":1.20.1:build", ":1.21.1:build")
}
