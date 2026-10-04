package com.example.data

import com.example.model.ButtonShape
import com.example.model.ControlZone
import com.example.model.GamepadButtonType
import com.example.model.GamepadControlItem
import org.json.JSONArray
import org.json.JSONObject

object ControlJsonParser {
    fun toJson(controls: List<GamepadControlItem>): String {
        val array = JSONArray()
        for (item in controls) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type.name)
                put("label", item.label)
                put("subLabel", item.subLabel)
                put("xPercent", item.xPercent.toDouble())
                put("yPercent", item.yPercent.toDouble())
                put("sizeDp", item.sizeDp.toDouble())
                put("opacity", item.opacity.toDouble())
                put("colorHex", item.colorHex)
                put("textColorHex", item.textColorHex)
                put("borderColorHex", item.borderColorHex)
                put("backgroundColorHex", item.backgroundColorHex)
                put("shape", item.shape.name)
                put("fontSizeSp", item.fontSizeSp.toDouble())
                put("cornerRadiusDp", item.cornerRadiusDp.toDouble())
                put("isVisible", item.isVisible)
                put("zone", item.zone.name)
                put("customKeycode", item.customKeycode)
                put("boundKeyId", item.boundKeyId)
                put("rotationDegrees", item.rotationDegrees.toDouble())
                put("isLocked", item.isLocked)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String): List<GamepadControlItem> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<GamepadControlItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val typeName = obj.optString("type", GamepadButtonType.BUTTON_A.name)
                val type = try {
                    GamepadButtonType.valueOf(typeName)
                } catch (e: Exception) {
                    GamepadButtonType.BUTTON_A
                }

                val zoneName = obj.optString("zone", ControlZone.RIGHT.name)
                val zone = try {
                    ControlZone.valueOf(zoneName)
                } catch (e: Exception) {
                    ControlZone.RIGHT
                }

                val shapeName = obj.optString("shape", ButtonShape.ROUNDED.name)
                val shape = try {
                    ButtonShape.valueOf(shapeName)
                } catch (e: Exception) {
                    ButtonShape.ROUNDED
                }

                val color = obj.optLong("colorHex", 0xFF3DDC84)

                list.add(
                    GamepadControlItem(
                        id = obj.optString("id", "item_$i"),
                        type = type,
                        label = obj.optString("label", type.defaultLabel),
                        subLabel = obj.optString("subLabel", ""),
                        xPercent = obj.optDouble("xPercent", 0.5).toFloat(),
                        yPercent = obj.optDouble("yPercent", 0.5).toFloat(),
                        sizeDp = obj.optDouble("sizeDp", 60.0).toFloat(),
                        opacity = obj.optDouble("opacity", 0.70).toFloat(),
                        colorHex = color,
                        textColorHex = obj.optLong("textColorHex", 0xFFFFFFFF),
                        borderColorHex = obj.optLong("borderColorHex", color),
                        backgroundColorHex = obj.optLong("backgroundColorHex", 0xFF16191D),
                        shape = shape,
                        fontSizeSp = obj.optDouble("fontSizeSp", 14.0).toFloat(),
                        cornerRadiusDp = obj.optDouble("cornerRadiusDp", 16.0).toFloat(),
                        isVisible = obj.optBoolean("isVisible", true),
                        zone = zone,
                        customKeycode = obj.optInt("customKeycode", 0),
                        boundKeyId = obj.optString("boundKeyId", ""),
                        rotationDegrees = obj.optDouble("rotationDegrees", 0.0).toFloat(),
                        isLocked = obj.optBoolean("isLocked", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
