package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ui.Messages
import java.util.*

class GoJsonAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.project

        if (project == null) {
            showErrorDialog("No project available!", null)
            return
        }

        val caretModel: SelectionModel = editor.selectionModel
        WriteCommandAction.runWriteCommandAction(project) {
            val selectedText = caretModel.selectedText?.trim()

            if (selectedText.isNullOrEmpty()) {
                showErrorDialog("No text selected!", project)
                return@runWriteCommandAction
            }

            val convertedText = generateJsonCode(selectedText)
            editor.document.replaceString(caretModel.selectionStart, caretModel.selectionEnd, convertedText)
        }
    }

    private fun showErrorDialog(message: String, project: com.intellij.openapi.project.Project?) {
        Messages.showMessageDialog(
            project,
            message,
            "Error",
            Messages.getErrorIcon()
        )
    }

    private fun generateJsonCode(structCode: String): String {
        val lines = structCode.lines()
        val structName = lines.firstOrNull()?.split("\\s+".toRegex())?.getOrNull(1) ?: return ""

        return buildString {
            lines.forEach { line ->
                when {
                    line.isEmpty() -> return@forEach
                    line.contains("type") || line == "}" -> appendLine(line)
                    else -> appendLine(convertFieldToJson(line))
                }
            }
        }
    }

    private fun convertFieldToJson(line: String): String {
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 2) return ""

        val fieldName = parts[0]
        val fieldType = parts[1]
        val annotations = parts.drop(2).joinToString(" ").replace("`", "")

        // Nếu dòng đã có tag json thì không thêm nữa
        if (annotations.contains("json:")) {
            return "    $fieldName $fieldType `$annotations`"
        }

        val jsonTag = "`json:\"${convertToSnakeCase(fieldName)}\""
        return "    $fieldName $fieldType $jsonTag $annotations`"
    }

    private fun convertToSnakeCase(text: String): String {
        return text.replace("([a-z])([A-Z])".toRegex(), "$1_$2")
            .replace("([A-Z]+)([A-Z][a-z])".toRegex(), "$1_$2")
            .split("[ _-]+".toRegex())
            .joinToString("_") { it.lowercase(Locale.getDefault()) }
    }
}
