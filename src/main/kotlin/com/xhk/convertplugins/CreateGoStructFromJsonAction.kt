package com.xhk.convertplugins

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextArea
import javax.swing.JTextField

class CreateGoStructFromJsonAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Hiển thị dialog nhập JSON và class name
        val dialog = CreateGoStructDialog(project)
        dialog.show()

        if (dialog.isOK) {
            val jsonString = dialog.jsonInput.trim()
            val className = dialog.className.trim()

            if (jsonString.isNotBlank() && className.isNotBlank()) {
                processJsonAndInsertAtCursor(e, project, jsonString, className)
            } else {
                showErrorDialog(project, "JSON Input or Struct Name cannot be empty!")
            }
        }
    }

    private fun processJsonAndInsertAtCursor(
        e: AnActionEvent,
        project: Project,
        jsonString: String,
        className: String
    ) {
        try {
            val objectMapper = jacksonObjectMapper()
            val jsonData: JsonNode = objectMapper.readTree(jsonString)

            val editor = e.getRequiredData(CommonDataKeys.EDITOR)
            insertTextAtCursor(e.project, editor) {
                GenerateGoStruct.GeneratePojos.generateObject(jsonData, className)
            }
        } catch (ex: Exception) {
            showErrorDialog(project, "Error processing JSON: ${ex.message}")
        }
    }

    private fun insertTextAtCursor(project: Project?, editor: Editor, generateStruct: () -> String) {
        val caretModel: CaretModel = editor.caretModel
        WriteCommandAction.runWriteCommandAction(project) {
            val convertedText = generateStruct()
            val offset = caretModel.offset
            editor.document.insertString(offset, convertedText)
        }
    }

    private fun showErrorDialog(project: Project?, message: String) {
        Messages.showMessageDialog(project, message, "Error", Messages.getErrorIcon())
    }
}

class CreateGoStructDialog(project: Project) : DialogWrapper(project) {
    private val jsonInputField = JTextArea(20, 80)
    private val classNameField = JTextField()

    init {
        init()
        title = "Create Go Struct from JSON"
    }

    override fun createCenterPanel(): JComponent = panel {
        row("JSON Input:") {
            scrollPane(jsonInputField)
        }
        row("Struct Name:") {
            classNameField(growX)
        }
    }

    val jsonInput: String
        get() = jsonInputField.text

    val className: String
        get() = classNameField.text
}
