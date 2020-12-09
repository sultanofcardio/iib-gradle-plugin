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

publishing {

    if(
        "sultanofcardioUser" in project.properties &&
        "sultanofcardioPassword" in project.properties &&
        "sultanofcardioUrl" in project.properties
    ) {
        val sultanofcardioUser: String by project
        val sultanofcardioPassword: String by project
        val sultanofcardioUrl: String by project

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
    }

    publications {

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
    }
}

repositories {
    mavenCentral()
    maven("https://repo.sultanofcardio.com/artifactory/sultanofcardio")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("com.sultanofcardio:iib-java-toolkit:1.0.1-SNAPSHOT")
    testImplementation(fileTree("lib-10.0.0.21") { this.include("*.jar") })
    testImplementation("org.eclipse.jetty:jetty-util:9.4.35.v20201120")
    testImplementation("junit:junit:4.13.1")
}

tasks {

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}