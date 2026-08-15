plugins {
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"
    `maven-publish`
}

val modVersion = project.property("mod.version") as String
val modArchivesName = project.property("mod.archives_name") as String
val targetMinecraftVersion = sc.current.version
val fabricLoaderVersion = project.property("deps.fabric_loader").toString()
val fabricApiVersion = project.property("deps.fabric_api").toString()
val modMenuVersion = project.property("deps.modmenu").toString()
val yaclVersion = project.property("deps.yacl").toString()

version = "$modVersion+$targetMinecraftVersion"
group = project.property("mod.group") as String

base {
    archivesName = modArchivesName
}

repositories {
    mavenCentral()

    // YACL Quilt parsers missing in dev runtime classpath.
    maven {
        name = "Quilt"
        url = uri("https://maven.quiltmc.org/repository/release/")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven {
        name = "DevAuth"
        url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$targetMinecraftVersion")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    implementation("maven.modrinth:modmenu:$modMenuVersion")
    implementation("maven.modrinth:yacl:$yaclVersion")

    runtimeOnly("org.quiltmc.parsers:json:0.2.1")
    runtimeOnly("org.quiltmc.parsers:gson:0.2.1")
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
}

loom {
    runs {
        named("client") {
            configName = "Minecraft Client $targetMinecraftVersion"
            appendProjectPathToConfigName.set(false)
            runDir = "../../run"
        }
    }
}

val targetJavaVersion = 25

tasks.processResources {
    val resourceProperties = mapOf(
        "version" to "$modVersion+$targetMinecraftVersion",
        "minecraft_version" to targetMinecraftVersion,
        "loader_version" to fabricLoaderVersion,
        "fabric_version" to fabricApiVersion,
        "modmenu_version" to modMenuVersion,
        "yacl_version" to yaclVersion,
        "java_version" to targetJavaVersion.toString()
    )
    inputs.properties(resourceProperties)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(resourceProperties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = targetJavaVersion
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withSourcesJar()
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.jar {
    from("LICENSE") {
        rename("LICENSE", "LICENSE_$modArchivesName")
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds this Minecraft version and collects its jars in the root build directory."
    dependsOn(tasks.build)
    from(tasks.jar.flatMap { it.archiveFile })
    from(tasks.named<Jar>("sourcesJar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = modArchivesName
            from(components["java"])
        }
    }
}
