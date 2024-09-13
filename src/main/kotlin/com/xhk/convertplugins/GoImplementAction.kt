package com.xhk.convertplugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ui.Messages

class GoImplementAction : AnAction() {

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
                    val convertedText = generateImplCode(selectedText.trim())
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

    private fun generateImplCode(interfaceCode: String): String {
        val interfaceName = extractInterfaceName(interfaceCode)
        val builder = StringBuilder()
        builder.append(interfaceCode)
        builder.append("\n\n")

        builder.append("type ").append(interfaceName).append("Impl struct {\n")
        builder.append("}\n\n")

        builder.append("var instance").append(interfaceName).append(" *").append(interfaceName).append("Impl\n")
        val once = interfaceName[0].lowercaseChar() + interfaceName.substring(1) + "Once"
        builder.append("var ").append(once).append(" sync.Once").append("\n\n")

        builder.append("func ").append(interfaceName).append("Instance() *").append(interfaceName).append("Impl {\n")
        builder.append("    ").append(once).append(".Do(func() {").append("\n")
        builder.append("        instance").append(interfaceName).append(" = &").append(interfaceName).append("Impl{}\n")
        builder.append("    })\n")
        builder.append("    return instance").append(interfaceName).append(";\n")
        builder.append("}\n\n")

        val methods = extractMethods(interfaceCode)
        for (method in methods) {
            builder.append(generateMethodImpl(method, interfaceName))
        }

        return builder.toString()
    }

    private fun extractInterfaceName(interfaceCode: String): String {
        // Simple extraction of the interface name
        return interfaceCode.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
    }

    private fun extractMethods(interfaceCode: String): Array<String> {
        // Extract methods from the interface
        return interfaceCode.split("\\{".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1].replace("}".toRegex(), "").trim { it <= ' ' }.split("\n".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
    }

    private fun generateMethodImpl(methodSignature: String, interfaceName: String): String {
        val method = StringBuilder()
        method.append("func (r *").append(interfaceName).append("Impl) ").append(methodSignature.trim { it <= ' ' })
        // Kiểm tra xem có kiểu trả về hay không
        val hasReturnValue = methodSignature.contains(")") && !methodSignature.endsWith(")")

        if (hasReturnValue) {
            // Nếu có kiểu trả về, thêm logic xử lý
            val returnType = methodSignature.split("\\)".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1].trim { it <= ' ' }
            method.append(" {\n")
            method.append("    // Implement logic here\n")

            // Nếu kiểu trả về là con trỏ hoặc slice, thêm return nil
            if (returnType.startsWith("*") || returnType.startsWith("[]") || returnType.startsWith("error")) {
                method.append("    return nil\n")
            }
            method.append("}\n\n")
        } else {
            // Nếu không có kiểu trả về, chỉ thêm logic xử lý
            method.append("{\n")
            method.append("    // Implement logic here\n")
            method.append("}\n\n")
        }

        return method.toString()
    }
}