package com.example.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import java.security.MessageDigest

object QRCodeHelper {

    fun generateRmCallId(): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val randomString = (1..6)
            .map { allowedChars.random() }
            .joinToString("")
        return "RM-$randomString"
    }

    fun parseRmCallId(raw: String): String? {
        val clean = raw.trim()
        val regex = Regex("RM-[A-Z0-9]{6}", RegexOption.IGNORE_CASE)
        val match = regex.find(clean)
        return match?.value?.uppercase()
    }

    /**
     * Generates a 25x25 QR Matrix with standard finder patterns, timing lines, and encoded data.
     */
    fun createMatrix(content: String, matrixDimension: Int = 25): Array<BooleanArray> {
        val matrix = Array(matrixDimension) { BooleanArray(matrixDimension) { false } }

        // 1. Draw 7x7 Finder Patterns at top-left, top-right, bottom-left
        drawFinderPattern(matrix, 0, 0)
        drawFinderPattern(matrix, matrixDimension - 7, 0)
        drawFinderPattern(matrix, 0, matrixDimension - 7)

        // 2. Timing patterns on row 6 and col 6
        for (i in 8 until matrixDimension - 8) {
            val isBlack = (i % 2 == 0)
            matrix[6][i] = isBlack
            matrix[i][6] = isBlack
        }

        // 3. Alignment pattern at (matrixDimension - 9, matrixDimension - 9)
        drawAlignmentPattern(matrix, matrixDimension - 9, matrixDimension - 9)

        // 4. Fill data modules based on content hash
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(content.toByteArray())
        var bitIndex = 0

        for (r in 0 until matrixDimension) {
            for (c in 0 until matrixDimension) {
                // Skip finder patterns + separators
                if (isReservedArea(r, c, matrixDimension)) continue

                // Center logo clear zone (9x9 in center)
                val centerStart = (matrixDimension - 7) / 2
                val centerEnd = centerStart + 7
                if (r in centerStart until centerEnd && c in centerStart until centerEnd) {
                    matrix[r][c] = false
                    continue
                }

                val byteVal = hash[bitIndex % hash.size].toInt()
                val bitVal = ((byteVal shr (bitIndex % 8)) and 1) == 1
                // Alternating mask
                val mask = (r + c) % 3 == 0
                matrix[r][c] = bitVal xor mask
                bitIndex++
            }
        }

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = (r == 0 || r == 6 || c == 0 || c == 6)
                val isInner = (r in 2..4 && c in 2..4)
                matrix[startR + r][startC + c] = isOuter || isInner
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, centerR: Int, centerC: Int) {
        for (r in -2..2) {
            for (c in -2..2) {
                val isOuter = (kotlin.math.abs(r) == 2 || kotlin.math.abs(c) == 2)
                val isCenter = (r == 0 && c == 0)
                if (centerR + r in matrix.indices && centerC + c in matrix[0].indices) {
                    matrix[centerR + r][centerC + c] = isOuter || isCenter
                }
            }
        }
    }

    private fun isReservedArea(r: Int, c: Int, size: Int): Boolean {
        // Top-left
        if (r < 8 && c < 8) return true
        // Top-right
        if (r < 8 && c >= size - 8) return true
        // Bottom-left
        if (r >= size - 8 && c < 8) return true
        // Timing lines
        if (r == 6 || c == 6) return true
        return false
    }
}

@Composable
fun QRCodeDisplay(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    moduleColor: Color = Color(0xFF0F172A),
    backgroundColor: Color = Color.White
) {
    val matrix = remember(content) { QRCodeHelper.createMatrix(content) }
    val dim = matrix.size

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(14.dp)
            .testTag("qr_code_display"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size.width
            val modulePixelSize = canvasSize / dim
            val cornerRadius = CornerRadius(modulePixelSize * 0.25f, modulePixelSize * 0.25f)

            for (r in 0 until dim) {
                for (c in 0 until dim) {
                    if (matrix[r][c]) {
                        drawRoundRect(
                            color = moduleColor,
                            topLeft = Offset(c * modulePixelSize, r * modulePixelSize),
                            size = Size(modulePixelSize * 0.94f, modulePixelSize * 0.94f),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }

        // Center RM Call Logo Badge
        Box(
            modifier = Modifier
                .size(size * 0.26f)
                .clip(CircleShape)
                .background(Color(0xFF0A192F))
                .border(3.dp, backgroundColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "RM Call Logo",
                modifier = Modifier.size(size * 0.18f)
            )
        }
    }
}
