package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.ui.Messages
import java.util.*

class ConvertToCamelCaseAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        // Lấy đoạn văn bản được chọn
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        if (e.project != null) {
            val caretModel: CaretModel = editor.caretModel
            // Iterate through each caret
            WriteCommandAction.runWriteCommandAction(e.project) {
                for (caret: Caret in caretModel.allCarets) {
                    val selectedText = caret.selectedText

                    if (selectedText != null) {
                        // Chuyển đổi văn bản đã chọn thành CamelCase hoặc SnakeCase
                        val convertedText = convertToCamelCase(selectedText)

                        // Thay thế văn bản đã chọn bằng văn bản đã chuyển đổi
                        editor.document.replaceString(caret.selectionStart, caret.selectionEnd, convertedText)
                    }
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

    private fun convertToCamelCase(text: String): String {
        return text.replace("\\s+".toRegex(), " ")  // Thay thế nhiều dấu cách bằng một dấu cách
            .split(Regex("[ _-]+"))
            .mapIndexed { index, word ->
                if (index == 0) word else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            .joinToString("")
    }

}