package com.sultanofcardio.iib.plugin

import com.ibm.broker.config.proxy.BrokerProxy
import com.ibm.broker.config.proxy.IntegrationNodeConnectionParameters
import com.sultanofcardio.iib.plugin.models.BarFile
import com.sultanofcardio.iib.plugin.tasks.BarTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

lateinit var iibProjectExtension: IIBProjectExtension

open class IIBGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        iibProjectExtension = IIBProjectExtension(project)
        val barTask = project.tasks.create("bar", BarTask::class.java)

        val bar = project.configurations.create("bar")
        project.configurations.getByName("implementation").extendsFrom(bar)
    }
}

/**
 * A class representing the various ways of configuring the IIB setup
 */
class IIBProjectExtension(private val project: Project) {

    internal val barFiles: MutableMap<String, BarFile> by lazy { mutableMapOf<String, BarFile>() }

    internal val nodes: MutableMap<String, IntegrationNode> by lazy { mutableMapOf<String, IntegrationNode>() }

    /**
     * Configure a BAR file for compilation
     */
    fun bar(name: String, receiver: BarFile.() -> Unit) {
        require(name.isNotEmpty())
        when(val bar = barFiles[name]) {
            null -> {
                BarFile(name, project).apply {
                    receiver(this)
                    barFiles[name] = this
                }
            }
            else -> receiver(bar)
        }
    }

    /**
     * Configure an integration node for deployment of BAR files
     */
    fun integrationNode(name: String, host: String, port: Int = 4414, receiver: IntegrationNode.() -> Unit) {
        require(name.isNotEmpty()) { "name should not be empty" }
        require(host.isNotEmpty()) { "host should not be empty" }
        val hash = sha256("$host:$port")
        when(val node = nodes[hash]) {
            null -> {
                IntegrationNode(name, host, port).apply {
                    receiver(this)
                    nodes[hash] = this
                }
            }
            else -> receiver(node.apply { this.name = name })
        }

        project.afterEvaluate {
            project.logger.info("Configuring deployment tasks")
            iibProjectExtension.barFiles.forEach { (barName, barFile) ->
                project.logger.info("Configuring deployment task for bar file $barName")
                iibProjectExtension.nodes.forEach { (_, node) ->
                    project.logger.info("Configuring deployment task for integration node ${node.name}")
                    project.tasks.create("deploy${barName}To${node.name}Node") { task ->
                        task.group = "deployment"
                        task.description = "Deploy the BAR file $barName to integration server ${node.name}"
                        task.dependsOn(project.tasks.getByName("bar"))
                        task.doLast {
                            val broker: BrokerProxy = BrokerProxy.getInstance(
                                if(node.username != null && node.password != null) {
                                    IntegrationNodeConnectionParameters(
                                        node.host, node.port, node.username, node.password, node.useSSL
                                    )
                                } else {
                                    IntegrationNodeConnectionParameters(node.host, node.port)
                                }
                            ).apply { synchronous = 60000 }
                            val executionGroup = broker.getExecutionGroupByName(node.server)
                            println(barFile.path)
                            val dr = executionGroup.deploy(barFile.file.inputStream(), barName, true, 30000)
                            if(dr.completionCode.intValue() != 0) {
                                val result = dr.deployResponses.toList().first()
                                throw RuntimeException(result.detail)
                            }
                        }
                    }
                }
            }
        }
    }
}

class IntegrationNode(var name: String, var host: String, var port: Int = 4414) {
    var username: String? = null
    var password: String? = null
    var useSSL = false

    /**
     * The integration server (execution group) to deploy the bar file to
     */
    lateinit var server: String

    init {
        require(name.isNotEmpty())
        require(host.isNotEmpty())
    }
}

/**
 * A function to configure the iib setup
 */
fun Project.iib(receiver: IIBProjectExtension.() -> Unit) {
    if(!::iibProjectExtension.isInitialized) {
        iibProjectExtension = IIBProjectExtension(this)
    }
    receiver(iibProjectExtension)
}

private fun sha256(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(text.toByteArray(StandardCharsets.UTF_8))
    return Base64.getEncoder().encodeToString(hash)
}