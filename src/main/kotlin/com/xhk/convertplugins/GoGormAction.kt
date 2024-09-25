package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.util.*

class GoGormAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.project

        if (project != null) {
            val selectionModel: SelectionModel = editor.selectionModel
            val selectedText = selectionModel.selectedText

            if (selectedText != null) {
                handleTextConversion(project, editor, selectionModel, selectedText.trim())
            } else {
                showErrorMessage(project, "No text selected!")
            }
        } else {
            showErrorMessage(null, "No project found!")
        }
    }

    private fun handleTextConversion(
        project: Project,
        editor: Editor,
        selectionModel: SelectionModel,
        selectedText: String
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            val convertedText = generateGormCode(selectedText)
            editor.document.replaceString(selectionModel.selectionStart, selectionModel.selectionEnd, convertedText)
        }
    }

    private fun showErrorMessage(project: Project?, message: String) {
        Messages.showMessageDialog(
            project,
            message,
            "Error",
            Messages.getErrorIcon()
        )
    }

    private fun generateGormCode(structCode: String): String {
        val structName = getStructName(structCode)
        val builder = StringBuilder()

        structCode.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                if (line.contains("type") || line.trim() == "}") {
                    builder.appendLine(line)
                } else {
                    builder.appendLine(convertFieldToGorm(line))
                }
            }

        builder.appendLine()
            .append("func ($structName) TableName() string {\n")
            .append("    return \"${convertToSnakeCase(structName)}\"\n")
            .append("}\n")

        return builder.toString()
    }

    private fun getStructName(structCode: String): String {
        return structCode.split("\\s+".toRegex())
            .filter { it.isNotBlank() }[1]
    }

    private fun convertFieldToGorm(line: String): String {
        val parts = line.trim().split("\\s+".toRegex())

        if (parts.size < 2) return ""

        val fieldName = parts[0]
        val fieldType = parts[1]
        val annotations = parts.drop(2).joinToString(" ").replace("`", "")


        // Nếu dòng đã có tag json thì không thêm nữa
        if (annotations.trim().contains("gorm:")) {
            return "    $fieldName $fieldType `$annotations`"
        }

        val gormTag = buildString {
            append("gorm:\"column:${convertToSnakeCase(fieldName)}")
            if (fieldName.equals("id", ignoreCase = true)) append(";primaryKey")
            append("\"")
        }

        return "    $fieldName $fieldType `$gormTag $annotations`"
    }

    private fun convertToSnakeCase(text: String): String {
        return text.replace("([a-z])([A-Z])".toRegex(), "$1_$2")
            .replace("([A-Z]+)([A-Z][a-z])".toRegex(), "$1_$2")
            .split("[ _-]+".toRegex())
            .joinToString("_")
            .lowercase(Locale.getDefault())
    }
}