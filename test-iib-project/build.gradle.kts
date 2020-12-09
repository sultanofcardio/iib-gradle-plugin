import com.sultanofcardio.iib.plugin.bar
import com.sultanofcardio.iib.plugin.models.node.*
import java.util.concurrent.TimeUnit

buildscript {
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    }

    repositories {
        mavenCentral()
        maven("https://repo.sultanofcardio.com/artifactory/sultanofcardio")
    }
}

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.sultanofcardio.iib-gradle") version "1.0.0-SNAPSHOT"
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

bar(name) {
    createMessageFlow("sample_message_flow") {
        processingNode("com.sample.App") {
            input = MQInputNode("MQ_INPUT", "INPUT_QUEUE").apply {
                browse = true
            }

            out = MQOutputNode("MQ_OUTPUT", "OUTPUT_QUEUE")

            failure = MQOutputNode("MQ_FAILURE", "FAILURE_QUEUE")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.sultanofcardio.com/artifactory/sultanofcardio")
}

dependencies {
    implementation("com.sultanofcardio:iib-java-toolkit:1.0.0-SNAPSHOT")
}
