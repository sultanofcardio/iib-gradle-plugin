package com.sultanofcardio.iib.plugin.models

import org.gradle.api.Project
import java.io.File

/**
 * This represents a bar archive and the resources inside it
 *
 * @param name The name of the bar file without the .bar extension
 */
open class BarFile internal constructor(var name: String, private val project: Project) {
    init {
        if(name.endsWith(".bar", true)) {
            name = name.substring(0 until name.length-4)
        }
    }

    val path: String get() = "${project.buildDir}/libs/${name}.bar"

    val file: File = File(path)

    internal var messageFlows = mutableListOf<MessageFlow>()

    fun createMessageFlow(name: String, flow: MessageFlow.() -> Unit): MessageFlow {
        val messageFlow = MessageFlow(name)
        flow(messageFlow)
        messageFlows.add(messageFlow)
        return messageFlow
    }

    fun addMessageFlow(messageFlow: MessageFlow) = messageFlows.add(messageFlow)

    val manifest: String = "srcSelected=false;showSrc=true;esql21VersionSelected=false;"
}