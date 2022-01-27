package onnx

import dev.fritz2.components.showAlertToast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import onnx.bindings.InferenceSession

object ONNX {
    val webgl = jsObject { executionProviders = arrayOf("webgl") }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun loadInferenceSession(): InferenceSession =
        runCatching { InferenceSession.create("./dce2.onnx", webgl).await() }
            .onFailure { showAlertToast { alert { title("Could not load WebGL, using WASM.") } } }
            .getOrDefault(InferenceSession.create("./dce2.onnx").await())

    inline fun jsObject(init: dynamic.() -> Unit): dynamic {
        val o = js("{}")
        init(o)
        return o
    }
}