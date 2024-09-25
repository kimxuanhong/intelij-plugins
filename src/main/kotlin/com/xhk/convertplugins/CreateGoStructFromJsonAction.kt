package com.xhk.convertplugins

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.JTextField

class CreateCoStructFromJsonAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dialog = CreateGoStructDialog(project)
        dialog.show()

        if (dialog.isOK) {
            val jsonString = dialog.jsonInput.trim()
            val className = dialog.className

            processJsonAndCreateFile(e, project, jsonString, className)
        }
    }

    private fun processJsonAndCreateFile(e: AnActionEvent, project: Project, jsonString: String, className: String) {
        try {
            val objectMapper = jacksonObjectMapper()
            val jsonData: JsonNode = objectMapper.readTree(jsonString)

            val editor = e.getRequiredData(CommonDataKeys.EDITOR)
            if (e.project != null) {
                val caretModel: SelectionModel = editor.selectionModel
                // Iterate through each caret
                WriteCommandAction.runWriteCommandAction(e.project) {
                    val selectedText = caretModel.selectedText
                    if (selectedText != null) {
                        // Chuyển đổi văn bản đã chọn thành CamelCase hoặc SnakeCase
                        val convertedText: String = GenerateGoStruct.GeneratePojos.generateObject(jsonData, className);
                        // Thay thế văn bản đã chọn bằng văn bản đã chuyển đổi
                        editor.document.replaceString(caretModel.selectionStart, caretModel.selectionEnd, convertedText)
                    } else {
                        Messages.showMessageDialog(
                            e.project,
                            "No text selected!",
                            "Error",
                            Messages.getErrorIcon()
                        )
                    }
                }
            } else {
                Messages.showMessageDialog(
                    e.project,
                    "No text selected!",
                    "Error",
                    Messages.getErrorIcon()
                )
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Error processing JSON: ${e.message}", "Error")
        }
    }
}

class CreateGoStructDialog(project: Project) : DialogWrapper(project) {
    private val jsonInputField = JTextArea(60, 80)
    private val classNameField = JTextField()

    init {
        init()
        title = "Create Go struct from JSON"
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("JSON Input:") {
                scrollPane(jsonInputField)
            }
            row("Struct Name:") {
                classNameField(growX)
            }
        }
    }

    val jsonInput: String
        get() = jsonInputField.text

    val className: String
        get() = classNameField.text
}

class CreateGoStructDialogSuccess(project: Project) : DialogWrapper(project) {
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