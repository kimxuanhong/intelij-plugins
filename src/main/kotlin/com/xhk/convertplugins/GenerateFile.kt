package com.xhk.convertplugins

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.ui.Messages
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.function.Consumer

class GenerateFile {
    class Pojos {
        var className: String? = null
        var imports: MutableSet<String> = HashSet()
        var fields: MutableMap<String, String> = HashMap()

        constructor()

        constructor(className: String?) {
            this.className = className
        }
    }


    object GeneratePojos {
        private val imports: HashMap<String?, String?> = object : HashMap<String?, String?>() {
            init {
                put("Boolean", Boolean::class.java.name)
                put("Double", Double::class.java.name)
                put("Integer", Int::class.java.name)
                put("Float", Float::class.java.name)
                put("String", String::class.java.name)
                put("Long", Long::class.java.name)
                put("List", MutableList::class.java.name)
                put("Set", MutableSet::class.java.name)
                put("Map", MutableMap::class.java.name)
                put("Object", Any::class.java.name)
            }
        }

        fun GenerateObject(node: JsonNode, packageName: String, className: String): String {
            val objects: MutableList<Pojos> = ArrayList()

            val mainContent = StringBuilder()

            parseObject(objects, node, className)

            for (pojos in objects) {
                val content = StringBuilder()
                content.append(packageName).append("\n\n")
                pojos.imports.forEach(Consumer { item: String ->
                    if (imports.containsKey(item)) {
                        content.append(imports[item]).append(";\n")
                    } else {
                        content.append(packageName).append(".")
                            .append(getType(if (item.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray().size == 1) item else item.split(";".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()[1])).append(";\n")
                    }
                })
                content.append("\n")
                content.append("public class ").append(pojos.className).append(" {\n")
                pojos.fields.forEach { (key: String?, value: String) ->
                    val type = getType(value)
                    content.append("\tprivate ").append(type).append(" ").append(key).append(";\n")
                }

                content.append("\n")

                pojos.fields.forEach { (key: String, value: String) ->
                    val type = getType(value)
                    content.append("\tpublic ").append(type).append(" get")
                        .append(key.substring(0, 1).uppercase(Locale.getDefault())).append(key.substring(1))
                        .append("() {\n").append("\t\t this.").append(key).append(";\n").append("\t}\n\n")
                }

                pojos.fields.forEach { (key: String, value: String) ->
                    val type = getType(value)
                    content.append("\tpublic void").append(" set")
                        .append(key.substring(0, 1).uppercase(Locale.getDefault())).append(key.substring(1))
                        .append("(").append(type).append(" ").append(key).append(") {\n")
                        .append("\t\t this.").append(key).append(" = ").append(key).append(";\n").append("\t}\n\n")
                }
                content.append("}\n")

                var fileName = pojos.className + ".java"

                val packagePath = packageName.replace('.', '/')
                val targetDir = File(packagePath)

                // Tạo thư mục nếu chưa tồn tại
                if (!targetDir.exists()) {
                    if (targetDir.mkdirs()) {
                        fileName = "$packagePath/$fileName"
                    }
                } else {
                    fileName = "$packagePath/$fileName"
                }
//                try {
//                    BufferedWriter(FileWriter(fileName)).use { writer ->
//                        writer.write(content.toString())
//                        writer.newLine() // ghi dòng mới
//                    }
//
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }

                //Messages.showErrorDialog(project, "Please select a directory.", "Error")

                mainContent.append(content.toString()).append("\n\n=====================================================\n\n")
                println("File written successfully!")
            }

            return mainContent.toString();
        }

        fun parseObject(objects: MutableList<Pojos>, node: JsonNode, className: String) {
            val fieldsIterator = node.fields()
            val pojos = createPojos(objects, className)

            while (fieldsIterator.hasNext()) {
                // Get the field name and child node
                val entry = fieldsIterator.next()
                val childProperty = entry.key
                val childNode = entry.value

                // Recurse into objects and arrays
                if (childNode.isObject) {
                    val childName = formatClassName(childProperty)
                    createFieldPojos(pojos, childProperty, childName)
                    parseObject(objects, childNode, childName)
                } else if (childNode.isArray) {
                    val childName = formatClassName(childProperty) // TODO:
                    createFieldPojos(pojos, childProperty, String.format("List;%s", childName))
                    parseArray(objects, childNode, childName)
                } else if (childNode.isBoolean) {
                    createFieldPojos(pojos, childProperty, "Boolean")
                } else if (childNode.isFloatingPointNumber) {
                    createFieldPojos(pojos, childProperty, "Double")
                } else if (childNode.isIntegralNumber) {
                    createFieldPojos(pojos, childProperty, "Long")
                } else if (childNode.isTextual) {
                    // Defer the type reference until later
                    createFieldPojos(pojos, childProperty, "String")
                } else {
                    createFieldPojos(pojos, childProperty, "Object")
                }
            }
        }

        private fun createFieldPojos(pojos: Pojos, childProperty: String, childType: String) {
            val fields = pojos.fields
            if (!fields.containsKey(childProperty)) {
                fields[childProperty] = childType
            }
            pojos.fields = fields

            if (!imports.containsKey(childType)) {
                pojos.imports.add(childType.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
            }

            pojos.imports.add(childType)
        }

        private fun parseArray(objects: MutableList<Pojos>, node: JsonNode, className: String) {
            val elementsIterator = node.elements()
            while (elementsIterator.hasNext()) {
                val childNode = elementsIterator.next()
                // Recurse on the first object or array
                if (childNode.isObject) {
                    parseObject(objects, childNode, className)
                    break
                } else if (childNode.isArray) {
                    parseObject(objects, childNode, className)
                    break
                }
            }
        }

        private fun formatClassName(childProperty: String): String {
            var childProperty = childProperty
            childProperty =
                childProperty.substring(0, 1).uppercase(Locale.getDefault()) + childProperty.substring(1) // TODO:

            return childProperty
        }

        private fun createPojos(objects: MutableList<Pojos>, className: String): Pojos {
            for (pojo in objects) {
                if (pojo.className == className) {
                    return pojo
                }
            }
            val newPojo = Pojos(className)
            objects.add(newPojo)
            return newPojo
        }

        private fun getType(value: String): String {
            if (value.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray().size > 1) return value.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0] + "<" + getType(value.split(";".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1]) + ">"
            return value.substring(0, 1).uppercase(Locale.getDefault()) + value.substring(1)
        }
    }
}
