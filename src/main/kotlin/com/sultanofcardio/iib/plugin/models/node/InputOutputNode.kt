@file:Suppress("MemberVisibilityCanBePrivate", "DuplicatedCode")

package com.sultanofcardio.iib.plugin.models.node

import com.sultanofcardio.iib.plugin.models.MessageFlow
import com.sultanofcardio.iib.plugin.models.ValidateMaster
import com.sultanofcardio.iib.plugin.models.valueOrNull

sealed class Node(val name: String) {
    internal abstract val namespace: String
    internal lateinit var messageFlow: MessageFlow
    abstract fun compile(messageFlowName: String): String
    abstract fun toXml(compositeNumber: Int): String
    internal fun requireMessageFlow() = require(this::messageFlow.isInitialized) { "Message flow not set" }
    fun StringBuilder.configurableProperty(propertyName: String, override: () -> String? = { null }): String {
        requireMessageFlow()
        val overrideValue = override()
        val property = if (overrideValue != null) {
            "<ConfigurableProperty override=\"$overrideValue\" uri=\"${messageFlow.name}#${name}.$propertyName\"/>"
        } else {
            "<ConfigurableProperty uri=\"${messageFlow.name}#${name}.$propertyName\"/>"
        }
        appendln(property)
        return property
    }
}

sealed class InputNode(name: String) : Node(name)

class MQInputNode(name: String, val queueName: String) : InputNode(name) {

    override val namespace: String
        get() = """xmlns:ComIbmMQInput.msgnode="ComIbmMQInput.msgnode""""

    var browse = false

    var sslCipherSpec = ""
    var sslPeerName = ""
    var channelName = ""
    var destinationQueueManagerName = ""
    var connection = MQConnection.Local
    var listenerPortNumber: Int? = null
    var policyUrl = ""
    var queueManagerHostName = ""
    var resetBrowseTimeout = -1
    var securityIdentity = ""
    var securityProfile = SecurityProfile.None
    var zOSSerializationToken = ""
    var topic = ""
    var useSsl = false
    var validate = ValidateMaster.None
    var additionalInstances = 0

    override fun compile(messageFlowName: String): String {
        return buildString {
            configurableProperty("queueName") { queueName }
            configurableProperty("connection") {
                connection.valueOrNull { it != MQConnection.Local }
            }
            configurableProperty("destinationQueueManagerName") {
                destinationQueueManagerName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("queueManagerHostname") {
                queueManagerHostName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("listenerPortNumber") {
                listenerPortNumber?.toString()
            }
            configurableProperty("channelName") {
                channelName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("securityIdentity") {
                securityIdentity.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("useSSL") {
                useSsl.valueOrNull()
            }
            configurableProperty("SSLPeerName") {
                sslPeerName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("SSLCipherSpec") {
                sslCipherSpec.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("serializationToken") {
                zOSSerializationToken.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("topicProperty") {
                topic.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("resetBrowseTimeout") {
                resetBrowseTimeout.valueOrNull { it >= 0 }
            }
            configurableProperty("validateMaster") {
                validate.valueOrNull { it != ValidateMaster.None }
            }
            configurableProperty("securityProfileName") {
                securityProfile.valueOrNull { it != SecurityProfile.None }
            }
            configurableProperty("componentLevel")
            configurableProperty("additionalInstances") {
                additionalInstances.valueOrNull { it > 0 }
            }
            configurableProperty("policyUrl") {
                policyUrl.valueOrNull { it.isNotBlank() }
            }
        }
    }

    override fun toXml(compositeNumber: Int): String {
        return """
             <nodes xmi:type="ComIbmMQInput.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_$compositeNumber" location="68,216"
                   queueName="$queueName" browse="$browse">
                <translation xmi:type="utility:ConstantString" string="$name"/>
            </nodes>
        """.trimIndent()
    }

    enum class SecurityProfile(val value: String) {
        None("No Security Profile"),
        DefaultPropagation("Default Propagation");

        override fun toString(): String = value
    }
}

enum class MQConnection(val value: String) {
    Local("Local queue manager"),
    MQClientConnectionProps("MQ client connection properties"),
    CCDT("Client channel definition table (CCDT) file");

    override fun toString(): String = value
}

sealed class OutputNode(name: String) : Node(name)

class MQOutputNode(name: String, val queueName: String) : OutputNode(name) {

    override val namespace: String
        get() = """xmlns:ComIbmMQOutput.msgnode="ComIbmMQOutput.msgnode" """

    var sslCipherSpec = ""
    var sslPeerName = ""
    var channelName = ""
    var destinationQueueManagerName = ""
    var connection = MQConnection.Local
    var listenerPortNumber: Int? = null
    var policyUrl = ""
    var queueManagerHostName = ""
    var queueManagerName = ""
    var replyToQueue = ""
    var replyToQueueManager = ""
    var securityIdentity = ""
    var securityProfile = MQInputNode.SecurityProfile.None
    var useSsl = false
    var validate = ValidateMaster.Inherit
    var addToRequestGroup = false
    var requestFolder = ""
    var requestTimeout = 0

    override fun compile(messageFlowName: String): String {
        return buildString {
            configurableProperty("queueName") { queueName }
            configurableProperty("connection") {
                connection.valueOrNull { it != MQConnection.Local }
            }
            configurableProperty("destinationQueueManagerName") {
                destinationQueueManagerName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("queueManagerHostname") {
                queueManagerHostName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("listenerPortNumber") {
                listenerPortNumber?.toString()
            }
            configurableProperty("channelName") {
                channelName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("securityIdentity") {
                securityIdentity.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("useSSL") {
                useSsl.valueOrNull()
            }
            configurableProperty("SSLPeerName") {
                sslPeerName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("SSLCipherSpec") {
                sslCipherSpec.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("queueManagerName") {
                queueManagerName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("replyToQMgr") {
                replyToQueueManager.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("replyToQ") {
                replyToQueue.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("validateMaster") {
                validate.valueOrNull { it != ValidateMaster.Inherit }
            }
            configurableProperty("securityProfileName") {
                securityProfile.valueOrNull { it != MQInputNode.SecurityProfile.None }
            }
            configurableProperty("policyUrl") {
                policyUrl.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("AddRequestToGroup") {
                addToRequestGroup.valueOrNull()
            }
            configurableProperty("GroupRequestFolderName") {
                requestFolder.valueOrNull { isNotBlank() }
            }
            configurableProperty("GroupRequestTimeout") {
                requestTimeout.valueOrNull { it > 0 }
            }
        }
    }

    override fun toXml(compositeNumber: Int): String {
        return """
            <nodes xmi:type="ComIbmMQOutput.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_$compositeNumber" location="461,311"
                   queueName="$queueName">
                <translation xmi:type="utility:ConstantString" string="$name"/>
            </nodes>
        """.trimIndent()
    }
}

class ProcessingNode(name: String, val qualifiedClassName: String) : Node(name) {

    override val namespace: String
        get() = """xmlns:ComIbmJavaCompute.msgnode="ComIbmJavaCompute.msgnode""""

    var javaClassLoader: String = ""
    var validate = ValidateMaster.None

    var input: InputNode? = null
        set(value) {
            field = value?.apply {
                messageFlow = this@ProcessingNode.messageFlow
            }
        }
    var out: OutputNode? = null
        set(value) {
            field = value?.apply {
                messageFlow = this@ProcessingNode.messageFlow
            }
        }
    var alternate: OutputNode? = null
        set(value) {
            field = value?.apply {
                messageFlow = this@ProcessingNode.messageFlow
            }
        }
    var failure: OutputNode? = null
        set(value) {
            field = value?.apply {
                messageFlow = this@ProcessingNode.messageFlow
            }
        }

    override fun compile(messageFlowName: String): String {
        return buildString {
            configurableProperty("javaClassLoader") {
                javaClassLoader.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("validateMaster") {
                validate.valueOrNull { it != ValidateMaster.None }
            }
        }
    }

    override fun toXml(compositeNumber: Int): String {
        return """
            <nodes xmi:type="ComIbmJavaCompute.msgnode:FCMComposite_1" xmi:id="FCMComposite_1_$compositeNumber" location="211,216"
                   javaClass="$qualifiedClassName">
                <translation xmi:type="utility:ConstantString" string="$name"/>
            </nodes>
        """.trimIndent()
    }
}
