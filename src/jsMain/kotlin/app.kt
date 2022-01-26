import dev.fritz2.binding.RootStore
import dev.fritz2.components.*
import dev.fritz2.dom.html.Canvas
import dev.fritz2.dom.html.render
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageData
import kotlin.math.min
import kotlin.time.ExperimentalTime

fun imgToUInt8ClampedArray(img: HTMLImageElement, ctx: CanvasRenderingContext2D): Uint8ClampedArray {
    val canvas = ctx.canvas
    canvas.width = img.naturalWidth
    canvas.height = img.naturalHeight
    ctx.drawImage(img, 0.0, 0.0)

    return ctx.getImageData(0.0, 0.0, img.naturalWidth.toDouble(), img.naturalHeight.toDouble()).data
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

suspend fun loadInferenceSession(webgl: dynamic): InferenceSession =
    runCatching { InferenceSession.create("./dce2.onnx", webgl).await() }
        .onFailure { showAlertToast { alert { title("Could not load WebGL, using WASM.") } } }
        .getOrDefault(InferenceSession.create("./dce2.onnx").await())

val targetB64 = RootStore("")

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
fun main() {
    val fileData = RootStore("random.png")
    val srcB64 = RootStore("")
    val isLoaded = RootStore("")
    val webgl: dynamic = object {}
    webgl["executionProviders"] = arrayOf("webgl")

    render {
        flexBox({
            direction { column }
            justifyContent { center }
        }) {
            file {
                accept("image/*")
                button {
                    text("Upload Image")
                    icon { cloudUpload }
                }
            }
                .onEach { file -> fileData.update("${file.name.split('.')[0]}_night_vision") }
                .map { file -> "data:${file.type};base64,${file.content}" } handledBy srcB64.update

            val srcImg = img(id = "img-from") {
                width(300)
                src(srcB64.data)
                domNode.onload = { isLoaded.update(domNode.src) }
            }

            isLoaded.data
                .filter { it.isNotEmpty() }
                .render {
                    icon { fromTheme { arrowDown } }
                }

            img(id = "img-to") {
                width(300)
                src(targetB64.data)
            }

            isLoaded.data
                .distinctUntilChanged()
                .filter { it.isNotEmpty() }
                .map {
                    val targetCanvas = Canvas(job = job, scope = scope)
                    val imgContext = targetCanvas.domNode.getContext("2d") as CanvasRenderingContext2D

                    val intData = imgToUInt8ClampedArray(srcImg.domNode, imgContext)
                    val floats = uInt8ClampedToFloat32Array(intData)

                    val tensor = Tensor(
                        "float32",
                        floats,
                        arrayOf(1, 3, srcImg.domNode.naturalWidth, srcImg.domNode.naturalHeight)
                    )
                    val input = tensorToInput(tensor)

                    val ir = loadInferenceSession(webgl)
                    val out = ir.run(input).await()

                    val outTensor = out["output"] as Tensor
                    val outData = outTensor.data as Float32Array

                    for (i in 0 until outData.length) {
                        outData[i] = min(outData[i], 1f) * 255f
                    }
                    val intOut = float32ToUInt8Clamped(outData)

                    val imgData = ImageData(intOut, srcImg.domNode.naturalWidth, srcImg.domNode.naturalHeight)
                    imgContext.putImageData(imgData, 0.0, 0.0)
                    targetCanvas.domNode.toDataURL().also { targetCanvas.domNode.remove() }
                } handledBy targetB64.update

            targetB64.data
                .filter { it.isNotEmpty() }
                .render {
                    a {
                        download(fileData.data)
                        href(targetB64.data)
                        clickButton {
                            text("Download")
                            icon { download }
                        }
                    }
                }
        }
    }
}