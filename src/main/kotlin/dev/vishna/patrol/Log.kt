package dev.vishna.patrol

var PATROL_DEBUG = false

/**
 * Emoji powered logger for patrol
 *
 * Usage
 *
 * ```kotlin
 * Log.warn.."Warning!!1"
 * ```
 */
enum class Log(val value: String) {
    edit("✏️"),
    conf("⚙️"),
    skip("⏭"),
    save("💾"),
    tool("🛠"),
    exit("👋"),
    bomb("💥"),
    warn("🚨")
}

operator fun <T> Log.rangeTo(message: T) {

    val throwable = if (message is Throwable) { message } else { null }
    val m = throwable?.message ?: message

    when (this) {
        Log.bomb -> System.err.println("${this.value} $m")
        else -> println("${this.value} $m")
    }

    if (PATROL_DEBUG) throwable?.printStackTrace(System.err)
}

/**
 * try-catch anything + auto logging. use wisely
 */
inline fun <T> safe(level: Log = Log.bomb, block: () -> T?) : T? {
    return try {
        block()
    } catch (t: Throwable) {
        level..t
        null
    }
}