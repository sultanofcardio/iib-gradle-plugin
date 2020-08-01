@file:Suppress("MemberVisibilityCanBePrivate")

package com.sultanofcardio.iib.plugin.models

import com.sultanofcardio.iib.plugin.models.node.InputNode
import com.sultanofcardio.iib.plugin.models.node.OutputNode
import com.sultanofcardio.iib.plugin.models.node.ProcessingNode

class MessageFlow(var name: String) {

    private lateinit var computeNode: ProcessingNode

    var policy = ""
    var additionalInstances = 0
    var processingTimeout = 0
    var processingAction = ProcessingAction.None
    var startAdditionalInstancesWhenFlowStarts = false
    var startMode = StartMode.Maintained
    var commitCount = 1
    var commitInterval = 0

    var consumerPolicySet: PolicySet? = null
    var consumerPolicySetBindings: PolicySetBindings? = null
    var coordinatedTransaction = false
    var providerPolicySet: PolicySet? = null
    var providerPolicySetBindings: PolicySetBindings? = null
    var monitoringProfileName = ""
    var securityProfileName = ""

    /**
     * Messages per second
     */
    var notificationThreshold = 0

    /**
     * Messages per second
     */
    var maximumRate = 0

    fun processingNode(clsName: String, node: ProcessingNode.() -> Unit) {
        this.computeNode = ProcessingNode("processor", clsName).apply {
            messageFlow = this@MessageFlow
            node(this)
        }
    }

    private fun StringBuilder.configurableProperty(propertyName: String, override: () -> String? = { null }): String {
        val overrideValue = override()
        val property =  if(overrideValue != null) {
            "<ConfigurableProperty override=\"$overrideValue\" uri=\"$name#$propertyName\"/>"
        } else {
            "<ConfigurableProperty uri=\"$name#$propertyName\"/>"
        }
        appendln(property)
        return property
    }

    internal fun compile(): String {
        val (inputNode, _, outputs) = ensureNodes()
        val (out, alternate, failure) = outputs

        return buildString {
            appendln("<CompiledMessageFlow name=\"$name\">")
            configurableProperty("additionalInstances") {
                additionalInstances.valueOrNull { it > 0 }
            }
            configurableProperty("notificationThresholdMsgsPerSec") {
                notificationThreshold.valueOrNull { it > 0 }
            }
            configurableProperty("maximumRateMsgsPerSec") {
                maximumRate.valueOrNull { it > 0 }
            }
            configurableProperty("processingTimeoutSec") {
                processingTimeout.valueOrNull { it > 0 }
            }
            configurableProperty("processingTimeoutAction") {
                processingAction.valueOrNull { it != ProcessingAction.None }
            }
            configurableProperty("wlmPolicy") {
                policy.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("commitCount") {
                commitCount.valueOrNull { it > 1 }
            }
            configurableProperty("commitInterval") {
                commitInterval.valueOrNull { it > 0 }
            }
            configurableProperty("coordinatedTransaction") {
                coordinatedTransaction.valueOrNull()
            }
            configurableProperty("consumerPolicySet") {
                consumerPolicySet?.value
            }
            configurableProperty("providerPolicySet") {
                providerPolicySet?.value
            }
            configurableProperty("consumerPolicySetBindings") {
                consumerPolicySetBindings?.value
            }
            configurableProperty("providerPolicySetBindings") {
                providerPolicySetBindings?.value
            }
            configurableProperty("securityProfileName") {
                securityProfileName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("monitoringProfile") {
                monitoringProfileName.valueOrNull { it.isNotBlank() }
            }
            configurableProperty("startMode") {
                startMode.valueOrNull { it != StartMode.Maintained }
            }
            configurableProperty("startInstancesWhenFlowStarts") {
                startAdditionalInstancesWhenFlowStarts.valueOrNull()
            }
            appendln(inputNode.compile(name))
            if(out != null) appendln(out.compile(name))
            if(alternate != null) appendln(alternate.compile(name))
            if(failure != null) appendln(inputNode.compile(name))
            appendln("</CompiledMessageFlow>")
        }
    }

    fun ensureNodes(): Triple<InputNode, ProcessingNode, List<OutputNode?>> {
        require(this::computeNode.isInitialized) { "You must specify a JavaComputeNode" }
        val input = computeNode.input
        requireNotNull(input) { "You must specify an InputNode" }
        val out = computeNode.out
        val alternate = computeNode.alternate
        val failure = computeNode.failure

        return Triple(input, computeNode, listOf(out, alternate, failure))
    }

    fun StringBuilder.connection(
        sourceCompositeNumber: Int, targetCompositeNumber: Int,
        sourceTerminal: String = "out", targetTerminal: String = "in"
    ) {
        appendln("""
            <connections xmi:type="eflow:FCMConnection" xmi:id="FCMConnection_1"
                sourceNode="FCMComposite_1_$sourceCompositeNumber" 
                sourceTerminalName="OutTerminal.$sourceTerminal"
                targetNode="FCMComposite_1_$targetCompositeNumber"
                targetTerminalName="InTerminal.$targetTerminal"/>
        """.trimIndent())
    }

    internal fun toXml(): String {
        val (input, computeNode, outputs) = ensureNodes()
        val (out, alternate, failure) = outputs
        val namespaces = listOfNotNull(input, computeNode, out, alternate, failure).map { it.namespace }.toSet()

        return buildString {
            appendln("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ecore:EPackage xmi:version="2.0"
                                xmlns:xmi="http://www.omg.org/XMI"
                                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                                xmlns:eflow="http://www.ibm.com/wbi/2005/eflow"
                                xmlns:utility="http://www.ibm.com/wbi/2005/eflow_utility" nsURI="$name.msgflow"                
            """.trimIndent())
            namespaces.forEach { appendln(it) }
            appendln("""
                                nsPrefix="$name.msgflow">
                    <eClassifiers xmi:type="eflow:FCMComposite" name="FCMComposite_1">
                        <eSuperTypes href="http://www.ibm.com/wbi/2005/eflow#//FCMBlock"/>
                        <translation xmi:type="utility:TranslatableString" key="$name" bundleName="$name"
                                     pluginId="flowAPIgenerated"/>
                        <colorGraphic16 xmi:type="utility:GIFFileGraphic"
                                        resourceName="platform:/plugin/flowAPIgenerated/icons/full/obj16/$name.gif"/>
                        <colorGraphic32 xmi:type="utility:GIFFileGraphic"
                                        resourceName="platform:/plugin/flowAPIgenerated/icons/full/obj30/$name.gif"/>
                        <composition>
            """.trimIndent())

            appendln(input.toXml(1))
            appendln(computeNode.toXml(2))

            connection(1, 2)

            out?.let {
                appendln(out.toXml(3))
                connection(2, 3)
            }

            alternate?.let {
                appendln(alternate.toXml(4))
                connection(2, 4, "alternate")
            }

            failure?.let {
                appendln(failure.toXml(5))
                connection(2, 5, "failure")
            }
            appendln("""
                        </composition>
                        <propertyOrganizer>
                        </propertyOrganizer>
                        <stickyBoard/>
                    </eClassifiers>
                </ecore:EPackage>
            """.trimIndent())
        }
    }

    enum class ProcessingAction(val value: String) {
        None("None"),
        Restart("Restart the Integration Server");

        override fun toString(): String = value
    }

    enum class StartMode(val value: String) {
        Maintained("Maintained"),
        Manual("Manual"),
        Automatic("Automatic");

        override fun toString(): String = value
    }

    enum class PolicySet(val value: String) {
        WSRMDefault("WSRMDefault"),
        WSS10Default("WSS10Default");

        override fun toString(): String = value
    }

    enum class PolicySetBindings(val value: String) {
        WSS10Default("WSS10Default");

        override fun toString(): String = value
    }
}

fun <T: Any> T.valueOrNull(value: String = toString(), check: (T) -> Boolean): String? {
    return if(check(this)) value else null
}

fun Boolean.valueOrNull(value: String = toString()): String? = valueOrNull(value) { it }