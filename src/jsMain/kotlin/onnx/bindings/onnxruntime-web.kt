@file:JsModule("onnxruntime-web")
@file:JsNonModule

package onnx.bindings

import org.khronos.webgl.*
import kotlin.js.Promise

external abstract class InferenceSession {
    fun run(feeds: FeedsType): Promise<ReturnType>
    companion object {
        fun create(input: String): Promise<InferenceSession>
        // { executionProviders: ['webgl'] }
        fun create(input: String, options: SessionOptions = definedExternally): Promise<InferenceSession>
    }
    interface OnnxValueMapType {
        @nativeGetter
        operator fun get(name: String): dynamic /* Tensor? | NonTensorType? */
        @nativeSetter
        operator fun set(name: String, value: Tensor)
        @nativeSetter
        operator fun set(name: String, value: NonTensorType)
    }
    interface SessionOptions {
        var executionProviders: Array<dynamic /* Any | ExecutionProviderOption | "cpu" | "cuda" | "wasm" | "webgl" | String */>?
            get() = definedExternally
            set(value) = definedExternally
        var intraOpNumThreads: Number?
            get() = definedExternally
            set(value) = definedExternally
        var interOpNumThreads: Number?
            get() = definedExternally
            set(value) = definedExternally
        var graphOptimizationLevel: String? /* "disabled" | "basic" | "extended" | "all" */
            get() = definedExternally
            set(value) = definedExternally
        var enableCpuMemArena: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var enableMemPattern: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var executionMode: String? /* "sequential" | "parallel" */
            get() = definedExternally
            set(value) = definedExternally
        var enableProfiling: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var profileFilePrefix: String?
            get() = definedExternally
            set(value) = definedExternally
        var logId: String?
            get() = definedExternally
            set(value) = definedExternally
        var logSeverityLevel: Number? /* 0 | 1 | 2 | 3 | 4 */
            get() = definedExternally
            set(value) = definedExternally
        var logVerbosityLevel: Number?
            get() = definedExternally
            set(value) = definedExternally
    }
}

external open class Tensor(type: String, data: Any, dims: Array<Number>) : TypedTensorBase<String /* "float32" | "uint8" | "int8" | "uint16" | "int16" | "int32" | "int64" | "string" | "bool" | "float16" | "float64" | "uint32" | "uint64" */>,
    TypedTensorUtils<String /* "float32" | "uint8" | "int8" | "uint16" | "int16" | "int32" | "int64" | "string" | "bool" | "float16" | "float64" | "uint32" | "uint64" */> {

    open override var dims: Array<Number>
    open override var type: String
    open override var data: Any
    override var size: Number = definedExternally

    interface DataTypeMap {
        var float32: Float32Array
        var uint8: Uint8Array
        var int8: Int8Array
        var uint16: Uint16Array
        var int16: Int16Array
        var int32: Int32Array
        var int64: Any
        var string: Array<String>
        var bool: Uint8Array
        var float16: Any
        var float64: Float64Array
        var uint32: Uint32Array
        var uint64: Any
    }
    interface ElementTypeMap {
        var float32: Number
        var uint8: Number
        var int8: Number
        var uint16: Number
        var int16: Number
        var int32: Number
        var int64: Any
        var string: String
        var bool: Boolean
        var float16: Any
        var float64: Number
        var uint32: Number
        var uint64: Any
    }

    override fun reshape(dims: Array<Number>): TypedTensor<String> = definedExternally
}
external interface Properties {
    var size: Number
}

external interface TypedShapeUtils<T : String> {
    fun reshape(dims: Array<Number>): TypedTensor<T>
}

external interface TypedTensorUtils<T : String> : Properties, TypedShapeUtils<T>

external interface TypedTensor<T : String> : TypedTensorBase<T>, TypedTensorUtils<T>

external interface TypedTensorBase<T : String> {
    var dims: Array<Number>
    var type: T
    var data: Any
}