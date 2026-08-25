package com.example.util

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.studyfiles.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class MovableShapeItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type2D: Shape2DType? = null,
    val type3D: Shape3DType? = null,
    val center: Offset,
    val size: Float = 140f,
    val color: Color = Color(0xFF2563EB),
    val strokeWidth: Float = 4f,
    val isFilled: Boolean = false,
    val vertices: List<Offset>? = null,
    val isSelected: Boolean = false
)

data class MovableTextItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val offset: Offset,
    val fontSize: Float = 16f,
    val color: Color = Color(0xFF1E293B),
    val bgColor: Color = Color(0x33FEF08A),
    val isBold: Boolean = true,
    val isSelected: Boolean = false
)

data class PageAnnotationData(
    val pageIndex: Int,
    val strokes: List<DrawStroke> = emptyList(),
    val stickyNotes: List<StickyNoteItem> = emptyList(),
    val movableShapes: List<MovableShapeItem> = emptyList(),
    val movableTexts: List<MovableTextItem> = emptyList()
)

object StudyFileAnnotationStore {

    private fun getFileKey(filePath: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(filePath.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            filePath.hashCode().toString()
        }
    }

    private fun getStorageFile(context: Context, filePath: String): File {
        val dir = File(context.filesDir, "annotations_store")
        if (!dir.exists()) dir.mkdirs()
        val key = getFileKey(filePath.ifBlank { "whiteboard_session" })
        return File(dir, "annot_$key.json")
    }

    suspend fun savePageAnnotations(
        context: Context,
        filePath: String,
        pageIndex: Int,
        strokes: List<DrawStroke>,
        stickyNotes: List<StickyNoteItem>,
        movableShapes: List<MovableShapeItem>,
        movableTexts: List<MovableTextItem>
    ) = withContext(Dispatchers.IO) {
        try {
            val targetFile = getStorageFile(context, filePath)
            val rootJson = if (targetFile.exists()) {
                try {
                    JSONObject(targetFile.readText())
                } catch (e: Exception) {
                    JSONObject()
                }
            } else {
                JSONObject()
            }

            val pagesArray = rootJson.optJSONArray("pages") ?: JSONArray()
            val newPagesArray = JSONArray()

            // Remove existing entry for this page
            for (i in 0 until pagesArray.length()) {
                val pObj = pagesArray.getJSONObject(i)
                if (pObj.optInt("pageIndex", -1) != pageIndex) {
                    newPagesArray.put(pObj)
                }
            }

            // Create current page JSON object
            val pageObj = JSONObject().apply {
                put("pageIndex", pageIndex)

                // Strokes
                val strokesArr = JSONArray()
                strokes.forEach { s ->
                    val sObj = JSONObject().apply {
                        put("color", s.color.value.toLong())
                        put("strokeWidth", s.strokeWidth.toDouble())
                        put("penStyle", s.penStyle.name)
                        put("isFilled", s.isFilled)

                        if (s.shape3DType != null) {
                            put("shape3DType", s.shape3DType.name)
                            s.shape3DCenter?.let { c ->
                                put("shape3DCenterX", c.x.toDouble())
                                put("shape3DCenterY", c.y.toDouble())
                            }
                            put("shape3DSize", s.shape3DSize.toDouble())
                        }

                        if (s.shape2DType != null) {
                            put("shape2DType", s.shape2DType.name)
                            if (s.shape2DVertices != null) {
                                val vArr = JSONArray()
                                s.shape2DVertices.forEach { v ->
                                    vArr.put(JSONObject().apply {
                                        put("x", v.x.toDouble())
                                        put("y", v.y.toDouble())
                                    })
                                }
                                put("shape2DVertices", vArr)
                            }
                        }

                        val ptsArr = JSONArray()
                        s.points.forEach { pt ->
                            ptsArr.put(JSONObject().apply {
                                put("x", pt.x.toDouble())
                                put("y", pt.y.toDouble())
                            })
                        }
                        put("points", ptsArr)
                    }
                    strokesArr.put(sObj)
                }
                put("strokes", strokesArr)

                // Sticky notes
                val notesArr = JSONArray()
                stickyNotes.forEach { n ->
                    val nObj = JSONObject().apply {
                        put("id", n.id)
                        put("text", n.text)
                        put("x", n.offset.x.toDouble())
                        put("y", n.offset.y.toDouble())
                        put("color", n.color.value.toLong())
                        put("fontSize", n.fontSize.toDouble())
                    }
                    notesArr.put(nObj)
                }
                put("stickyNotes", notesArr)

                // Movable shapes
                val shapesArr = JSONArray()
                movableShapes.forEach { sh ->
                    val shObj = JSONObject().apply {
                        put("id", sh.id)
                        sh.type2D?.let { put("type2D", it.name) }
                        sh.type3D?.let { put("type3D", it.name) }
                        put("centerX", sh.center.x.toDouble())
                        put("centerY", sh.center.y.toDouble())
                        put("size", sh.size.toDouble())
                        put("color", sh.color.value.toLong())
                        put("strokeWidth", sh.strokeWidth.toDouble())
                        put("isFilled", sh.isFilled)
                        if (sh.vertices != null) {
                            val vArr = JSONArray()
                            sh.vertices.forEach { v ->
                                vArr.put(JSONObject().apply {
                                    put("x", v.x.toDouble())
                                    put("y", v.y.toDouble())
                                })
                            }
                            put("vertices", vArr)
                        }
                    }
                    shapesArr.put(shObj)
                }
                put("movableShapes", shapesArr)

                // Movable texts
                val textsArr = JSONArray()
                movableTexts.forEach { t ->
                    val tObj = JSONObject().apply {
                        put("id", t.id)
                        put("text", t.text)
                        put("x", t.offset.x.toDouble())
                        put("y", t.offset.y.toDouble())
                        put("fontSize", t.fontSize.toDouble())
                        put("color", t.color.value.toLong())
                        put("bgColor", t.bgColor.value.toLong())
                        put("isBold", t.isBold)
                    }
                    textsArr.put(tObj)
                }
                put("movableTexts", textsArr)
            }

            newPagesArray.put(pageObj)
            rootJson.put("pages", newPagesArray)
            rootJson.put("lastModified", System.currentTimeMillis())

            targetFile.writeText(rootJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadPageAnnotations(
        context: Context,
        filePath: String,
        pageIndex: Int
    ): PageAnnotationData = withContext(Dispatchers.IO) {
        try {
            val targetFile = getStorageFile(context, filePath)
            if (!targetFile.exists()) {
                return@withContext PageAnnotationData(pageIndex = pageIndex)
            }

            val rootJson = JSONObject(targetFile.readText())
            val pagesArray = rootJson.optJSONArray("pages") ?: return@withContext PageAnnotationData(pageIndex = pageIndex)

            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                if (pageObj.optInt("pageIndex", -1) == pageIndex) {
                    // Parse Strokes
                    val parsedStrokes = mutableListOf<DrawStroke>()
                    val strokesArr = pageObj.optJSONArray("strokes")
                    if (strokesArr != null) {
                        for (sIdx in 0 until strokesArr.length()) {
                            val sObj = strokesArr.getJSONObject(sIdx)
                            val colorLong = sObj.optLong("color", 0xFF000000)
                            val strokeW = sObj.optDouble("strokeWidth", 3.0).toFloat()
                            val pStyle = try {
                                PenStyle.valueOf(sObj.optString("penStyle", "NORMAL"))
                            } catch (e: Exception) {
                                PenStyle.NORMAL
                            }
                            val isFilled = sObj.optBoolean("isFilled", false)

                            val s3D = if (sObj.has("shape3DType")) {
                                try { Shape3DType.valueOf(sObj.getString("shape3DType")) } catch (e: Exception) { null }
                            } else null

                            val s3DCenter = if (sObj.has("shape3DCenterX")) {
                                Offset(sObj.getDouble("shape3DCenterX").toFloat(), sObj.getDouble("shape3DCenterY").toFloat())
                            } else null

                            val s3DSize = sObj.optDouble("shape3DSize", 140.0).toFloat()

                            val s2D = if (sObj.has("shape2DType")) {
                                try { Shape2DType.valueOf(sObj.getString("shape2DType")) } catch (e: Exception) { null }
                            } else null

                            val s2DVertices = if (sObj.has("shape2DVertices")) {
                                val vArr = sObj.getJSONArray("shape2DVertices")
                                val vList = mutableListOf<Offset>()
                                for (vIdx in 0 until vArr.length()) {
                                    val vObj = vArr.getJSONObject(vIdx)
                                    vList.add(Offset(vObj.getDouble("x").toFloat(), vObj.getDouble("y").toFloat()))
                                }
                                vList
                            } else null

                            val ptsArr = sObj.optJSONArray("points")
                            val pts = mutableListOf<Offset>()
                            if (ptsArr != null) {
                                for (pIdx in 0 until ptsArr.length()) {
                                    val ptObj = ptsArr.getJSONObject(pIdx)
                                    pts.add(Offset(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                                }
                            }

                            parsedStrokes.add(
                                DrawStroke(
                                    points = pts,
                                    color = Color(colorLong.toULong()),
                                    strokeWidth = strokeW,
                                    penStyle = pStyle,
                                    isFilled = isFilled,
                                    shape3DType = s3D,
                                    shape3DCenter = s3DCenter,
                                    shape3DSize = s3DSize,
                                    shape2DVertices = s2DVertices,
                                    shape2DType = s2D
                                )
                            )
                        }
                    }

                    // Parse Sticky Notes
                    val parsedNotes = mutableListOf<StickyNoteItem>()
                    val notesArr = pageObj.optJSONArray("stickyNotes")
                    if (notesArr != null) {
                        for (nIdx in 0 until notesArr.length()) {
                            val nObj = notesArr.getJSONObject(nIdx)
                            parsedNotes.add(
                                StickyNoteItem(
                                    id = nObj.optString("id", java.util.UUID.randomUUID().toString()),
                                    text = nObj.optString("text", ""),
                                    offset = Offset(nObj.optDouble("x", 100.0).toFloat(), nObj.optDouble("y", 100.0).toFloat()),
                                    color = Color(nObj.optLong("color", 0xFFFEF08A).toULong()),
                                    fontSize = nObj.optDouble("fontSize", 14.0).toFloat()
                                )
                            )
                        }
                    }

                    // Parse Movable Shapes
                    val parsedShapes = mutableListOf<MovableShapeItem>()
                    val shapesArr = pageObj.optJSONArray("movableShapes")
                    if (shapesArr != null) {
                        for (shIdx in 0 until shapesArr.length()) {
                            val shObj = shapesArr.getJSONObject(shIdx)
                            val t2D = if (shObj.has("type2D")) try { Shape2DType.valueOf(shObj.getString("type2D")) } catch (e: Exception) { null } else null
                            val t3D = if (shObj.has("type3D")) try { Shape3DType.valueOf(shObj.getString("type3D")) } catch (e: Exception) { null } else null
                            val vList = if (shObj.has("vertices")) {
                                val vArr = shObj.getJSONArray("vertices")
                                val list = mutableListOf<Offset>()
                                for (vI in 0 until vArr.length()) {
                                    val vO = vArr.getJSONObject(vI)
                                    list.add(Offset(vO.getDouble("x").toFloat(), vO.getDouble("y").toFloat()))
                                }
                                list
                            } else null

                            parsedShapes.add(
                                MovableShapeItem(
                                    id = shObj.optString("id", java.util.UUID.randomUUID().toString()),
                                    type2D = t2D,
                                    type3D = t3D,
                                    center = Offset(shObj.optDouble("centerX", 200.0).toFloat(), shObj.optDouble("centerY", 200.0).toFloat()),
                                    size = shObj.optDouble("size", 140.0).toFloat(),
                                    color = Color(shObj.optLong("color", 0xFF2563EB).toULong()),
                                    strokeWidth = shObj.optDouble("strokeWidth", 4.0).toFloat(),
                                    isFilled = shObj.optBoolean("isFilled", false),
                                    vertices = vList
                                )
                            )
                        }
                    }

                    // Parse Movable Texts
                    val parsedTexts = mutableListOf<MovableTextItem>()
                    val textsArr = pageObj.optJSONArray("movableTexts")
                    if (textsArr != null) {
                        for (tIdx in 0 until textsArr.length()) {
                            val tObj = textsArr.getJSONObject(tIdx)
                            parsedTexts.add(
                                MovableTextItem(
                                    id = tObj.optString("id", java.util.UUID.randomUUID().toString()),
                                    text = tObj.optString("text", ""),
                                    offset = Offset(tObj.optDouble("x", 100.0).toFloat(), tObj.optDouble("y", 100.0).toFloat()),
                                    fontSize = tObj.optDouble("fontSize", 16.0).toFloat(),
                                    color = Color(tObj.optLong("color", 0xFF1E293B).toULong()),
                                    bgColor = Color(tObj.optLong("bgColor", 0x33FEF08A).toULong()),
                                    isBold = tObj.optBoolean("isBold", true)
                                )
                            )
                        }
                    }

                    return@withContext PageAnnotationData(
                        pageIndex = pageIndex,
                        strokes = parsedStrokes,
                        stickyNotes = parsedNotes,
                        movableShapes = parsedShapes,
                        movableTexts = parsedTexts
                    )
                }
            }

            PageAnnotationData(pageIndex = pageIndex)
        } catch (e: Exception) {
            e.printStackTrace()
            PageAnnotationData(pageIndex = pageIndex)
        }
    }
}
