package com.sultanofcardio.iib.plugin

import com.sultanofcardio.iib.plugin.models.BarFile
import com.sultanofcardio.iib.plugin.tasks.BarTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class IIBGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("bar", BarTask::class.java)
    }
}

internal val Project.barFiles: MutableMap<String, BarFile> by lazy { mutableMapOf<String, BarFile>() }

fun Project.bar(name: String, receiver: BarFile.() -> Unit) {
    when(val bar = barFiles[name]) {
        null -> {
            BarFile(name, this).apply {
                receiver(this)
                barFiles[name] = this
            }
        }
        else -> receiver(bar)
    }
}