package com.example.neurogate.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val Dashboard: ImageVector
        get() {
            if (_dashboard != null) {
                return _dashboard!!
            }
            _dashboard = ImageVector.Builder(
                name = "Dashboard",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
                ) {
                    moveTo(3f, 13f)
                    horizontalLineTo(11f)
                    verticalLineTo(3f)
                    horizontalLineTo(3f)
                    verticalLineTo(13f)
                    close()
                    moveTo(3f, 21f)
                    horizontalLineTo(11f)
                    verticalLineTo(15f)
                    horizontalLineTo(3f)
                    verticalLineTo(21f)
                    close()
                    moveTo(13f, 21f)
                    horizontalLineTo(21f)
                    verticalLineTo(11f)
                    horizontalLineTo(13f)
                    verticalLineTo(21f)
                    close()
                    moveTo(13f, 3f)
                    verticalLineTo(9f)
                    horizontalLineTo(21f)
                    verticalLineTo(3f)
                    horizontalLineTo(13f)
                    close()
                }
            }.build()
            return _dashboard!!
        }
    private var _dashboard: ImageVector? = null

    val ClearAll: ImageVector
        get() {
            if (_clearAll != null) {
                return _clearAll!!
            }
            _clearAll = ImageVector.Builder(
                name = "ClearAll",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
                ) {
                    moveTo(5f, 13f)
                    horizontalLineTo(19f)
                    verticalLineTo(11f)
                    horizontalLineTo(5f)
                    verticalLineTo(13f)
                    close()
                    moveTo(3f, 17f)
                    horizontalLineTo(19f)
                    verticalLineTo(15f)
                    horizontalLineTo(3f)
                    verticalLineTo(17f)
                    close()
                    moveTo(7f, 7f)
                    verticalLineTo(9f)
                    horizontalLineTo(21f)
                    verticalLineTo(7f)
                    horizontalLineTo(7f)
                    close()
                }
            }.build()
            return _clearAll!!
        }
    private var _clearAll: ImageVector? = null

    val Security: ImageVector
        get() {
            if (_security != null) {
                return _security!!
            }
            _security = ImageVector.Builder(
                name = "Security",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
                ) {
                    moveTo(12f, 1f)
                    lineTo(3f, 5f)
                    verticalLineTo(11f)
                    curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
                    curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
                    verticalLineTo(5f)
                    lineTo(12f, 1f)
                    close()
                    moveTo(12f, 11.99f)
                    horizontalLineTo(19f)
                    curveTo(18.47f, 16.11f, 15.72f, 19.78f, 12f, 20.93f)
                    verticalLineTo(12f)
                    horizontalLineTo(5f)
                    verticalLineTo(6.3f)
                    lineTo(12f, 2.19f)
                    verticalLineTo(11.99f)
                    close()
                }
            }.build()
            return _security!!
        }
    private var _security: ImageVector? = null

    val Code: ImageVector
        get() {
            if (_code != null) {
                return _code!!
            }
            _code = ImageVector.Builder(
                name = "Code",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
                ) {
                    moveTo(9.4f, 16.6f)
                    lineTo(4.8f, 12f)
                    lineTo(9.4f, 7.4f)
                    lineTo(8f, 6f)
                    lineTo(2f, 12f)
                    lineTo(8f, 18f)
                    lineTo(9.4f, 16.6f)
                    close()
                    moveTo(14.6f, 16.6f)
                    lineTo(19.2f, 12f)
                    lineTo(14.6f, 7.4f)
                    lineTo(16f, 6f)
                    lineTo(22f, 12f)
                    lineTo(16f, 18f)
                    lineTo(14.6f, 16.6f)
                    close()
                }
            }.build()
            return _code!!
        }
    private var _code: ImageVector? = null

    val Build: ImageVector
        get() {
            if (_build != null) {
                return _build!!
            }
            _build = ImageVector.Builder(
                name = "Build",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
                ) {
                    moveTo(22.7f, 19.0f)
                    lineTo(13.6f, 9.9f)
                    curveTo(14.5f, 7.6f, 14.0f, 4.9f, 12.1f, 3.0f)
                    curveTo(10.1f, 1.0f, 7.1f, 0.6f, 4.7f, 1.7f)
                    lineTo(9.0f, 6.0f)
                    lineTo(6.0f, 9.0f)
                    lineTo(1.6f, 4.7f)
                    curveTo(0.4f, 7.1f, 0.9f, 10.1f, 2.9f, 12.1f)
                    curveTo(4.8f, 14.0f, 7.5f, 14.5f, 9.8f, 13.6f)
                    lineTo(18.9f, 22.7f)
                    curveTo(19.3f, 23.1f, 19.9f, 23.1f, 20.3f, 22.7f)
                    lineTo(22.6f, 20.4f)
                    curveTo(23.1f, 19.9f, 23.1f, 19.3f, 22.7f, 19.0f)
                    close()
                }
            }.build()
            return _build!!
        }
    private var _build: ImageVector? = null

    val RadioButtonChecked: ImageVector
        get() {
            if (_radioButtonChecked != null) {
                return _radioButtonChecked!!
            }
            _radioButtonChecked = ImageVector.Builder(
                name = "RadioButtonChecked",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 0.0f,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
                    strokeLineMiter = 4.0f,
                    pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
                ) {
                    moveTo(12f, 2f)
                    curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                    reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                    reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                    reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                    close()
                    moveTo(12f, 20f)
                    curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                    reflectiveCurveTo(7.58f, 4f, 12f, 4f)
                    reflectiveCurveTo(20f, 7.58f, 20f, 12f)
                    reflectiveCurveTo(16.42f, 20f, 12f, 20f)
                    close()
                    moveTo(12f, 7f)
                    curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                    reflectiveCurveTo(9.24f, 17f, 12f, 17f)
                    reflectiveCurveTo(17f, 14.76f, 17f, 12f)
                    reflectiveCurveTo(14.76f, 7f, 12f, 7f)
                    close()
                }
            }.build()
            return _radioButtonChecked!!
        }
    private var _radioButtonChecked: ImageVector? = null
}
