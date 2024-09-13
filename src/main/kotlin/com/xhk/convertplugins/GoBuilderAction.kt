package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ui.Messages
import java.util.*

class GoBuilderAction : AnAction() {

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
                    val convertedText = generateBuilderCode(selectedText.trim())
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

    private fun generateBuilderCode(structCode: String): String {
        // Basic parsing to get struct name
        val structName = structCode.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val builder = StringBuilder()

        builder.append(structCode)
        builder.append("\n\n")
        builder.append("type ").append(structName).append("Builder struct {\n")
        builder.append("    ").append(structName[0].lowercaseChar()).append(structName.substring(1)).append(" ")
            .append(structName).append("\n")
        builder.append("}\n\n")
        builder.append("func New").append(structName).append("Builder() *").append(structName).append("Builder {\n")
        builder.append("    return &").append(structName).append("Builder{}\n")
        builder.append("}\n\n")

        // Assuming all fields are public and of basic types
        val lines = structCode.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            if (line.contains("type") || line.trim { it <= ' ' }.isEmpty()) continue
            val parts = line.trim { it <= ' ' }.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (parts.size < 2) continue
            val fieldName = parts[0]
            val fieldType = parts[1]
            builder.append("func (b *").append(structName).append("Builder) ").append(capitalize(fieldName))
                .append("(").append(fieldName).append(" ").append(fieldType).append(") *").append(structName)
                .append("Builder {\n")
            builder.append("    b.").append(structName[0].lowercaseChar()).append(structName.substring(1)).append(".")
                .append(fieldName).append(" = ").append(fieldName).append("\n")
            builder.append("    return b\n")
            builder.append("}\n\n")
        }

        builder.append("func (b *").append(structName).append("Builder) Build() *").append(structName).append(" {\n")
        builder.append("    return &b.").append(structName[0].lowercaseChar()).append(structName.substring(1))
            .append("\n")
        builder.append("}\n")

        return builder.toString()
    }

    private fun capitalize(str: String): String {
        return str.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }
}