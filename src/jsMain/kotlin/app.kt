
import dev.fritz2.binding.RootStore
import dev.fritz2.components.clickButton
import dev.fritz2.components.data.File
import dev.fritz2.components.file
import dev.fritz2.dom.html.Canvas
import dev.fritz2.dom.html.Img
import dev.fritz2.dom.html.render
import dev.fritz2.tracking.tracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.map
import onnx.ONNX
import onnx.TensorHelper
import onnx.bindings.Tensor
import org.khronos.webgl.Float32Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.ImageData
import kotlin.math.min
import kotlin.time.ExperimentalTime

object store : RootStore<String>("") {
    val tracking = tracker()

    val processImage = handle<Pair<Canvas, Img>> { b64, (targetCanvas, srcImg) ->
        tracking.track("processImage") {
            val imgContext = targetCanvas.domNode.getContext("2d") as CanvasRenderingContext2D
            val (width, height) = srcImg.domNode.naturalWidth to srcImg.domNode.naturalHeight
            val intData = TensorHelper.imgToUInt8ClampedArray(srcImg.domNode, imgContext)
            val floats = TensorHelper.uInt8ClampedToFloat32Array(intData)

            val tensor = Tensor("float32", floats, arrayOf(1, 3, width, height))
            val input = TensorHelper.tensorToInput(tensor)

            val ir = ONNX.loadInferenceSession()
            val out = ir.run(input).await()

            val outTensor = out["output"] as Tensor
            val outData = outTensor.data as Float32Array

            for (i in 0 until outData.length) {
                outData[i] = min(outData[i], 1f) * 255f
            }
            val intOut = TensorHelper.float32ToUInt8Clamped(outData)

            val imgData = ImageData(intOut, width, height)
            imgContext.putImageData(imgData, 0.0, 0.0)
            targetCanvas.domNode.toDataURL()
        }
    }
}

object srcFile : RootStore<String>("") {
    val parseFile = handleAndEmit<File, String> { _, file ->
        val name = file.name.split('.')[0]
        emit("${name}_night_vision")

        "data:${file.type};base64,${file.content}"
    }
}


@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
fun main() {
    render {
        file {
            accept("image/*")
            button {
                text("Upload Image")
                icon { cloudUpload }
            }
        } handledBy srcFile.parseFile

        val fromImg = img(id = "img-from") {
            width(300)
            src(srcFile.data)
        }
        val targetCanvas = canvas { inlineStyle("display:none;") }

        clickButton {
            enabled(srcFile.data.map { it.isNotEmpty() })
            text("Transform Image")
            loading(store.tracking.data)
            loadingText("Transforming data...")
        }.map { targetCanvas to fromImg } handledBy store.processImage

        img(id = "img-to") {
            width(300)
            src(store.data)
        }

        a {
            download(srcFile.parseFile)
            href(store.data)
            clickButton {
                enabled(store.data.map { it.isNotEmpty() })
                text("Download")
                icon { download }
            }
        }
    }
}