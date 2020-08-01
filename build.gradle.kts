plugins {
    kotlin("jvm") version "1.3.72"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.sultanofcardio"
version = "1.0.0"
gradlePlugin {
    plugins {
        create("iib-gradle") {
            id = "com.sultanofcardio.iib-gradle"
            implementationClass = "com.sultanofcardio.iib.plugin.IIBGradlePlugin"
        }
    }
}

val sultanofcardioUser: String by project
val sultanofcardioPassword: String by project
val sultanofcardioUrl: String by project

publishing {
    repositories {
        maven {
            name = "sultanofcardio"
            credentials {
                username = sultanofcardioUser
                password = sultanofcardioPassword
            }
            url = uri(sultanofcardioUrl)
        }
    }

    publications {

        forEach {
            println("${it.name}: ${it::class.qualifiedName}")
        }

        create<MavenPublication>("pluginMavenSnapshot") {
            artifactId = project.name
            version = "${project.version}-SNAPSHOT"
            from(components["java"])
        }

        create<MavenPublication>("${project.name}PluginMarkerMavenSnapshot") pnpmms@ {
            artifactId = project.name
            version = "${project.version}-SNAPSHOT"
            pom.withXml {
                val dependencies = asNode().appendNode("dependencies")
                val dependency = dependencies.appendNode("dependency")
                dependency.appendNode("groupId", group)
                dependency.appendNode("artifactId", project.name)
                dependency.appendNode("version", this@pnpmms.version)
            }
        }
    }
}

afterEvaluate {
    publishing.publications {
        getByName<MavenPublication>("pluginMavenSnapshot") pms@ {
            groupId = getByName<MavenPublication>("pluginMaven").groupId
            artifactId = getByName<MavenPublication>("pluginMaven").artifactId
        }

        getByName<MavenPublication>("${project.name}PluginMarkerMavenSnapshot") pnpmms@ {
            groupId = getByName<MavenPublication>("${project.name}PluginMarkerMaven").groupId
            artifactId = getByName<MavenPublication>("${project.name}PluginMarkerMaven").artifactId
        }
    }

    publishing.publications.forEach {
        when(it) {
            is MavenPublication -> {
                println("Name: ${it.name}")
                println("Class name: ${it::class.qualifiedName}")
                println("Group ID: ${it.groupId}")
                println("Version: ${it.version}")
                println("Artifact ID: ${it.artifactId}")
                println("Artifacts: ${it.artifacts}")
                println("POM: ${it.pom.url.getOrElse("")}")
                println("=======")
            }
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("com.sultanofcardio:iib-java-toolkit:1.0.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks {

    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}