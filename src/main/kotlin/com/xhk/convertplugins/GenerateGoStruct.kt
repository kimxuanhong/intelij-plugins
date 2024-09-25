package com.xhk.convertplugins

import com.fasterxml.jackson.databind.JsonNode

class GenerateGoStruct {

    class Pojos() {
        // Getters and setters
        var className: String? = null
        var fields: MutableMap<String, String> = HashMap()

        constructor(className: String?) : this() {
            this.className = className
        }
    }

    object GeneratePojos {
        fun generateObject(node: JsonNode, className: String): String {
            val objects: MutableList<Pojos> = ArrayList()
            val mainContent = StringBuilder()

            parseObject(objects, node, className)

            for (pojos in objects) {
                val content = StringBuilder()

                content.append("\n")
                    .append("type ").append(pojos.className).append(" struct {\n")

                for ((key1, type) in pojos.fields) {
                    val key = getKey(key1)
                    content.append("\t").append(key).append(" ").append(type).append(" `json:\"").append(key1)
                        .append("\"`\n")
                }
                content.append("}")

                mainContent.append(content).append("\n")
            }

            return mainContent.toString()
        }


        private fun parseObject(objects: MutableList<Pojos>, node: JsonNode, className: String) {
            val fieldsIterator = node.fieldNames()
            val pojos = createPojos(objects, className)

            while (fieldsIterator.hasNext()) {
                val childProperty = fieldsIterator.next()
                val childNode = node[childProperty]

                // Recurse into objects and arrays
                if (childNode.isObject) {
                    val childName = formatClassName(childProperty)
                    createFieldPojos(pojos, childProperty, childName)
                    parseObject(objects, childNode, childName)
                } else if (childNode.isArray) {
                    // Kiểm tra các phần tử trong mảng
                    if (!childNode.isEmpty) {
                        val firstElement = childNode[0]
                        var childName: String

                        // Xác định loại dữ liệu dựa vào phần tử đầu tiên trong mảng
                        if (firstElement.isObject) {
                            childName = formatClassName(childProperty)
                            createFieldPojos(pojos, childProperty, "[]$childName")
                            parseArray(objects, childNode, childName)
                        } else {
                            // Xử lý các kiểu dữ liệu nguyên thủy hoặc wrapper
                            val fieldType = determineFieldType(firstElement)
                            createFieldPojos(pojos, childProperty, "[]$fieldType")
                        }
                    } else {
                        // Nếu mảng rỗng, có thể đặt một kiểu mặc định
                        createFieldPojos(pojos, childProperty, "[]interface{}")
                    }
                } else if (childNode.isBoolean) {
                    createFieldPojos(pojos, childProperty, "bool")
                } else if (childNode.isFloatingPointNumber) {
                    createFieldPojos(pojos, childProperty, "float32")
                } else if (childNode.isIntegralNumber) {
                    createFieldPojos(pojos, childProperty, "int")
                } else if (childNode.isTextual) {
                    createFieldPojos(pojos, childProperty, "string")
                } else {
                    createFieldPojos(pojos, childProperty, "interface{}")
                }
            }
        }

        private fun createFieldPojos(pojos: Pojos, childProperty: String, childType: String) {
            val fields = pojos.fields
            if (!fields.containsKey(childProperty)) {
                fields[childProperty] = childType
            }
            pojos.fields = fields
        }

        private fun parseArray(objects: MutableList<Pojos>, node: JsonNode, className: String) {
            val elementsIterator = node.elements()
            while (elementsIterator.hasNext()) {
                val childNode = elementsIterator.next()
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
            return childProperty[0].uppercaseChar().toString() + childProperty.substring(1)
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


        private fun getKey(key: String): String {
            return key[0].uppercaseChar().toString() + key.substring(1)
        }

        // Phương thức xác định loại trường
        private fun determineFieldType(node: JsonNode): String {
            return if (node.isBoolean) {
                "bool"
            } else if (node.isFloatingPointNumber) {
                "float32"
            } else if (node.isIntegralNumber) {
                "int"
            } else if (node.isTextual) {
                "string"
            } else {
                "interface{}"
            }
        }
    }
}