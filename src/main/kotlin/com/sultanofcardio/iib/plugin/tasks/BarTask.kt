package com.sultanofcardio.iib.plugin.tasks

import com.sultanofcardio.iib.plugin.barFiles
import com.sultanofcardio.iib.plugin.models.BarFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class BarTask : DefaultTask() {

    @OutputFiles
    private val barFiles: MutableMap<String, BarFile> = project.barFiles

    @InputFiles
    private var compileClasspath: MutableSet<File> = mutableSetOf()

    private lateinit var jar: File

    init {
        group = "build"
        description = "Create a BAR file with this project's resources"
        project.afterEvaluate {

            compileClasspath = mutableSetOf()

            var compileTaskPresent = false
            var jarTaskPresent = false

            compileClasspath.addAll(project.configurations.getByName("bar").toList())

            project.tasks.forEach {
                when {
                    it is JavaCompile && it.name == "compileJava" -> {
                        compileTaskPresent = true
                    }
                    it is Jar && it.name == "jar" -> {
                        jarTaskPresent = true
                        jar = it.outputs.files.first()
                    }
                    it.name == "compileKotlin" -> {
                        compileTaskPresent = true
                    }
                }
            }

            if("assemble" in project.tasks.asMap) {
                project.tasks.getByName("assemble").dependsOn(this)
            }

            if(!compileTaskPresent)
                throw IllegalStateException("Unable to find a compile task. Add the java or kotlin plugin")

            if(!jarTaskPresent)
                throw IllegalStateException("Unable to find jar task")
            else dependsOn("jar")
        }
    }

    @Suppress("DuplicatedCode")
    @TaskAction
    fun action() {
        if (barFiles.isEmpty()) {
            logger.warn("No bar files configured")
            return
        }

        barFiles.forEach { (_, bar) ->
            val iibBuildDir = bar.file.parentFile
            val barDirName = bar.file.name.removeSuffix(".bar")

            val barDir = iibBuildDir.childDir(barDirName) {
                childDir("META-INF") {
                    child("manifest.mf") {
                        writeText(bar.manifest)
                    }
                }

                val appDir = childDir(barDirName) {
                    childDir("META-INF") {
                        child("manifest.mf") {
                            writeText(bar.manifest)
                        }
                        child("broker.xml") {
                            writeText(buildString {
                                appendln(
                                    """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <Broker>
                                    <CompiledApplication>
                                        <ConfigurableProperty uri="startMode"/>
                                        <ConfigurableProperty uri="javaIsolation"/>
                                    </CompiledApplication>
                                    """.trimIndent()
                                )

                                bar.messageFlows.forEach {
                                    appendln(it.compile())
                                }
                                appendln(
                                    """
                                </Broker>
                            """.trimIndent()
                                )
                            })
                        }
                    }

                    child("application.descriptor") {
                        writeText(
                            """
                            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                            <ns2:appDescriptor xmlns="http://com.ibm.etools.mft.descriptor.base"
                                               xmlns:ns2="http://com.ibm.etools.mft.descriptor.app">
                                <references/>
                            </ns2:appDescriptor>
                        """.trimIndent()
                        )
                    }

                    childDir("libs") {
                        if(compileClasspath.isEmpty()) println("No files on compile classpath")
                        compileClasspath.forEach {
                            child(it.name) {
                                it.copyTo(this, true)
                            }
                        }
                    }

                    child("src.jar") {
                        jar.copyTo(this, true)
                    }

                    bar.messageFlows.forEach {
                        child("${it.name}.msgflow") {
                            writeText(it.toXml())
                        }
                    }
                }

                val appArchive = File(this, "${appDir.name}.appzip")
                try {
                    logger.info("Creating zip file ${appArchive.absolutePath}")
                    ZipOutputStream(FileOutputStream(appArchive)).use { zos ->
                        appDir.zip(zos)
                        appDir.deleteRecursively()
                    }
                } catch (e: Throwable) {
                    appDir.deleteRecursively()
                    appArchive.delete()
                    throw e
                }
            }
            try {
                logger.info("Creating zip file ${bar.file.absolutePath}")
                ZipOutputStream(FileOutputStream(bar.file)).use { zos ->
                    barDir.zip(zos)
                    barDir.deleteRecursively()
                }
            } catch (e: Throwable) {
                barDir.deleteRecursively()
                bar.file.delete()
                throw e
            }
        }
    }

    private fun File.zip(zos: ZipOutputStream, parent: String? = null) {
        val dirList: Array<String> = list() ?: throw IOException("Unable to compress $name")
        val readBuffer = ByteArray(2156)
        var bytesIn: Int
        dirList.forEach { file ->
            val path = if (parent != null) "$parent/$file" else file
            val f = File(this, file)
            if (f.isDirectory) {
                f.zip(zos, path)
            } else {
                logger.info("Adding file ${f.path}")
                FileInputStream(f).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        val zipEntry = ZipEntry(path)
                        zos.putNextEntry(zipEntry)
                        bytesIn = fis.read(readBuffer)
                        while (bytesIn != -1) {
                            zos.write(readBuffer, 0, bytesIn)
                            bytesIn = bis.read(readBuffer)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}

internal fun File.childDir(path: String, child: File.() -> Unit): File {
    require(isDirectory)
    val c = File(this, path)
    c.mkdirs()
    child(c)
    return c
}

internal fun File.child(path: String, child: File.() -> Unit): File {
    require(isDirectory)
    val c = File(this, path)
    c.createNewFile()
    child(c)
    return c
}
