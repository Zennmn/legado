package io.legado.app.help.coroutine

@Suppress("unused")
class CompositeCoroutine : CoroutineContainer {

    private var resources: HashSet<Coroutine<*>>? = null

    val size: Int get() = resources?.size ?: 0
    val isEmpty: Boolean get() = size == 0

    constructor()

    constructor(vararg coroutines: Coroutine<*>) {
        resources = hashSetOf(*coroutines)
    }

    constructor(coroutines: Iterable<Coroutine<*>>) {
        resources = hashSetOf<Coroutine<*>>().apply { addAll(coroutines) }
    }

    override fun add(coroutine: Coroutine<*>): Boolean = synchronized(this) {
        val set = resources ?: hashSetOf<Coroutine<*>>().also { resources = it }
        set.add(coroutine)
    }

    override fun addAll(vararg coroutines: Coroutine<*>): Boolean = synchronized(this) {
        val set = resources ?: hashSetOf<Coroutine<*>>().also { resources = it }
        coroutines.all { set.add(it) }
    }

    override fun remove(coroutine: Coroutine<*>): Boolean {
        if (!delete(coroutine)) return false
        coroutine.cancel()
        return true
    }

    override fun delete(coroutine: Coroutine<*>): Boolean = synchronized(this) {
        resources?.remove(coroutine) ?: false
    }

    override fun clear() {
        val set = synchronized(this) {
            resources.also { resources = null }
        }
        set?.forEach { it.cancel() }
    }
}
