package com.xhk.convertplugins

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.JTextField

class CreateJavaFileFromJsonAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dialog = CreateJavaFileDialog(project)
        dialog.show()

        if (dialog.isOK) {
            val jsonString = dialog.jsonInput
            val packageName = dialog.packageName
            val className = dialog.className

            processJsonAndCreateFile(project, jsonString, packageName, className)
        }
    }

    private fun processJsonAndCreateFile(project: Project, jsonString: String, packageName: String, className: String) {
        try {
            val objectMapper = jacksonObjectMapper()
            val jsonData: JsonNode = objectMapper.readTree(jsonString)
            //WriteCommandAction.runWriteCommandAction(project) {
                val content:String = GenerateFile.GeneratePojos.GenerateObject(jsonData, packageName, className);
            //}

            val dialog = CreateJavaFileDialogSuccess(project)
            dialog.jsonInputField.text = content
            dialog.show()
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Error processing JSON: ${e.message}", "Error")
        }
    }
}

class CreateJavaFileDialog(project: Project) : DialogWrapper(project) {
    private val jsonInputField = JTextArea(60, 80)
    private val packageNameField = JTextField()
    private val classNameField = JTextField()

    init {
        init()
        title = "Create Java File from JSON"
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("JSON Input:") {
                scrollPane(jsonInputField)
            }
            row("Package Name:") {
                packageNameField(growX)
            }
            row("Class Name:") {
                classNameField(growX)
            }
        }
    }

    val jsonInput: String
        get() = jsonInputField.text

    val packageName: String
        get() = packageNameField.text

    val className: String
        get() = classNameField.text
}

class CreateJavaFileDialogSuccess(project: Project) : DialogWrapper(project) {
    public val jsonInputField = JTextArea(60, 80)
    init {
        init()
        title = "Convert successfully!"
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("JSON Input:") {
                scrollPane(jsonInputField)
            }
        }
    }

    val jsonInput: String
        get() = jsonInputField.text
}