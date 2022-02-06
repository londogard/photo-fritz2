package onnx

import onnx.bindings.FeedsType
import onnx.bindings.Tensor
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLImageElement

object TensorHelper {
    private const val maxSize: Float = 1280*720f

    fun rescaleToMaxSize(width: Int, height: Int): Pair<Int, Int> {
        val scale = maxSize / (width * height)

        return (scale * width).toInt().coerceAtMost(width) to (scale * height).toInt().coerceAtMost(height)
    }

    fun imgToUInt8ClampedArray(img: HTMLImageElement, ctx: CanvasRenderingContext2D, width: Int, height: Int): Uint8ClampedArray {
        val canvas = ctx.canvas
        canvas.width = width
        canvas.height = height
        ctx.drawImage(img, 0.0, 0.0, width.toDouble(), height.toDouble())

        return ctx.getImageData(0.0, 0.0, width.toDouble(), height.toDouble()).data
    }

    fun float32ToUInt8Clamped(data: Float32Array): Uint8ClampedArray {
        val rgb = arrayOf(0, data.length / 3, data.length / 3 * 2)
        val intOut = Uint8ClampedArray(data.length / 3 * 4)

        for (i in 0 until intOut.length / 4) {
            intOut.asDynamic()[i * 4 + 0] = data[rgb[0] + i].toInt()
            intOut.asDynamic()[i * 4 + 1] = data[rgb[1] + i].toInt()
            intOut.asDynamic()[i * 4 + 2] = data[rgb[2] + i].toInt()
            intOut.asDynamic()[i * 4 + 3] = 255
        }

        return intOut
    }

    fun tensorToInput(tensor: Tensor, inputName: String = "input"): FeedsType {
        val input: dynamic = object {}
        input[inputName] = tensor

        return input.unsafeCast<FeedsType>()
    }

    fun uInt8ClampedToFloat32Array(data: Uint8ClampedArray): Float32Array {
        val floats = Float32Array(data.length / 4 * 3)
        val rgb = listOf(0, data.length / 4, data.length / 4 * 2)

        for (i in 0 until data.length step 4) {
            floats[rgb[0] + i / 4] = data[i + 0] / 255f
            floats[rgb[1] + i / 4] = data[i + 1] / 255f
            floats[rgb[2] + i / 4] = data[i + 2] / 255f // Skip i+3 as that's ALPHA
        }

        return floats
    }
}