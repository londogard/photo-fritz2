
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import onnx.ONNX
import onnx.TensorHelper
import onnx.TensorHelper.rescaleToMaxSize
import onnx.bindings.Tensor
import org.khronos.webgl.Float32Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.ImageData
import kotlin.time.ExperimentalTime

object OnnxInferenceStore : RootStore<String>("") {
    val tracking = tracker()


    val processImage = handle<Pair<Canvas, Img>> { b64, (targetCanvas, srcImg) ->
        tracking.track {
            kotlin.runCatching {
                delay(60)
                val imgContext = targetCanvas.domNode.getContext("2d") as CanvasRenderingContext2D
                val (width, height) = rescaleToMaxSize(srcImg.domNode.naturalWidth, srcImg.domNode.naturalHeight)
                val intData = TensorHelper.imgToUInt8ClampedArray(srcImg.domNode, imgContext, width, height)
                val floats = TensorHelper.uInt8ClampedToFloat32Array(intData)

                val tensor = Tensor("float32", floats, arrayOf(1, 3, width, height))
                val input = TensorHelper.tensorToInput(tensor)

                val ir = ONNX.loadInferenceSession()
                // println("Loaded")
                val out = ir.run(input).await()

                val outTensor = out["output"] as Tensor
                // js("outTensor *= 255.0")
                val outData = outTensor.data as Float32Array
                // println("Float32")

                for (i in 0 until outData.length) {
                    outData[i] = if (outData[i] > 1) 255f else outData[i] * 255f
                }
                // println("Minified float32...")
                val intOut = TensorHelper.float32ToUInt8Clamped(outData)
                // println("Created int")
                val imgData = ImageData(intOut, width, height)
                imgContext.putImageData(imgData, 0.0, 0.0)
                // println("Put img")
                targetCanvas.domNode.toDataURL()
            }.onFailure { println("Failed: $it") }.getOrThrow()// .also { println("Donezo!") }

        }
    }
}

object SourceImgStore : RootStore<String>("") {
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
        } handledBy SourceImgStore.parseFile

        val fromImg = img(id = "img-from") {
            width(300)
            src(SourceImgStore.data)
        }
        val targetCanvas = canvas { inlineStyle("display:none;") }

        clickButton {
            enabled(SourceImgStore.data.map { it.isNotEmpty() })
            text("Transform Image")
            loading(OnnxInferenceStore.tracking.data)
            loadingText("Transforming data...")
        }.map { targetCanvas to fromImg } handledBy OnnxInferenceStore.processImage

        img(id = "img-to") {
            width(300)
            src(OnnxInferenceStore.data)
        }

        a {
            download(SourceImgStore.parseFile)
            href(OnnxInferenceStore.data)
            clickButton {
                enabled(OnnxInferenceStore.data.map { it.isNotEmpty() })
                text("Download")
                icon { download }
            }
        }
    }
}