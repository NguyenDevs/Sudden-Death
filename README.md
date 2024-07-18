plugins {
    id 'java'
}

group = 'org.NguyenDevs'
version = '2.0.2'

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
    }
    maven {
        name = "sonatype"
        url = "https://oss.sonatype.org/content/groups/public/"
    }
    maven {
        url = 'https://repo.extendedclip.com/content/repositories/placeholderapi/'
    }
    mavenCentral()
    maven { url "https://maven.enginehub.org/repo/" }
}

dependencies {
    compileOnly 'com.sk89q.worldguard:worldguard-bukkit:7.0.9'
    implementation 'org.apache.commons:commons-lang3:3.14.0'
    compileOnly "org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT"
    implementation 'com.googlecode.json-simple:json-simple:1.1.1'
    compileOnly 'me.clip:placeholderapi:2.11.6'
}

def targetJavaVersion = 17
java {
    def javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') {
        expand props
    }
}