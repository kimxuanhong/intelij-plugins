package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

class ConvertToSnakeCaseAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        // Lấy đoạn văn bản được chọn
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText

        // Kiểm tra nếu có văn bản được chọn
        if (!selectedText.isNullOrEmpty()) {
            // Convert đoạn văn bản sang snake_case
            val convertedText = convertToSnakeCase(selectedText)

            // Lấy Document và thay thế văn bản đã chọn
            e.project?.let { replaceTextInEditor(it, editor, selectionModel, convertedText) }
        } else {
            Messages.showMessageDialog(
                e.project,
                "No text selected!",
                "Error",
                Messages.getErrorIcon()
            )
        }
    }

    private fun convertToSnakeCase(text: String): String {
        return text.replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2").lowercase()
    }

    fun replaceTextInEditor(project: Project, editor: Editor, textRange: SelectionModel, newText: String) {
        // Sử dụng WriteCommandAction để đảm bảo quyền truy cập ghi trong IntelliJ
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.replaceString(textRange.selectionStart, textRange.selectionEnd, newText)
        }
    }
}