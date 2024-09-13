package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ui.Messages
import java.util.*
import java.util.stream.Collectors

class GoGormAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        // Lấy đoạn văn bản được chọn
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        if (e.project != null) {
            val caretModel: SelectionModel = editor.selectionModel
            // Iterate through each caret
            WriteCommandAction.runWriteCommandAction(e.project) {
                val selectedText = caretModel.selectedText
                if (selectedText != null) {
                    // Chuyển đổi văn bản đã chọn thành CamelCase hoặc SnakeCase
                    val convertedText = generateGormCode(selectedText.trim())
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
    }

    private fun generateGormCode(structCode: String): String {
        // Basic parsing to get struct name
        val structName = structCode.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val builder = StringBuilder()
        // Assuming all fields are public and of basic types
        val lines = structCode.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            if (line.trim { it <= ' ' }.isEmpty()) continue
            if (line.contains("type") || line.trim { it <= ' ' } == "}") {
                builder.append(line.trim { it <= ' ' }).append("\n")
                continue
            }
            val parts = line.trim { it <= ' ' }.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (parts.size < 2) continue
            val fieldName = parts[0]
            val fieldType = parts[1]

            builder.append("    ").append(fieldName)
                .append(" ")
                .append(fieldType)
                .append(" ")
                .append("`gorm:\"column:")
            builder.append(convertToSnakeCase(fieldName))
            if (fieldName.equals("id", ignoreCase = true)) {
                builder.append(";primaryKey")
            }
            builder.append("\"")
            if (parts.size > 2) {
                builder.append(" ")
                builder.append(
                    Arrays.stream(parts)
                        .skip(2)
                        .collect(Collectors.joining(" "))
                )
            }
            builder.append("`\n")
        }

        builder.append("\n")
        builder.append("func (")
        builder.append(structName)
        builder.append(") TableName() string {\n")
        builder.append("    return \"").append(convertToSnakeCase(structName))
            .append("\"\n")
        builder.append("}\n")


        return builder.toString()
    }
    private fun convertToSnakeCase(text: String): String {
        return java.lang.String.join("_", *text.replace("([a-z])([A-Z])".toRegex(), "$1_$2")
            .replace("([A-Z]+)([A-Z][a-z])".toRegex(), "$1_$2")
            .split("[ _-]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        ).lowercase(Locale.getDefault())
    }
}