plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2"

tasks.register("buildAll") {
    group = "build"
    description = "Builds and collects jars for every supported Minecraft version."
    dependsOn(":26.1:buildAndCollect", ":26.2:buildAndCollect")
}
