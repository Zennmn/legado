package io.legado.app.model

object Debug {
    fun log(vararg msg: Any?) = Unit
    fun dumpHprofData(fileName: String) = Unit
}
