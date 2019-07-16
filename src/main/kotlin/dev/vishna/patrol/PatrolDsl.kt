package dev.vishna.patrol

import java.io.File
import java.lang.IllegalArgumentException

typealias CommandArgs = Array<String>

typealias PatrolInspection = ((watchPoint: WatchPoint, dryRun: Boolean) -> Unit)

typealias BootstrapPatrol = ((patrolFile: File) -> Boolean)

/**
 * Patrol is a simplistic shell for any routine that wants to be subscribed to changes
 * in file system as defined in the accompanying patrol file.
 */
data class Patrol(
    /**
     * You can provide bootstrap strategy for patrol's configuration file, that is create
     * the configuration file for this patrol if it doesn't exist yet
     */
    val bootstrap: BootstrapPatrol?,

    /**
     * Whenever patrol detected changes in one of the WatchPoints, this is a callback that
     * will get triggered.
     */
    val onInspection: PatrolInspection,

    /**
     * The name of the CLI command
     */
    val name: String,

    /**
     * Description that will be shown in CLI if --help flag is passed
     */
    val help: String,

    /**
     * Name of the patrol file that will be used to instigate all the WatchPoints. Defaults to
     * name + .yaml extension
     */
    val patrolFileName: String,

    /**
     * Whether or not we should run in debug mode
     */
    val debug: Boolean
) {
    @PatrolDsl
    class Builder {
        private var bootstrap: BootstrapPatrol? = null
        var debug: Boolean = false
        private lateinit var onInspection_: PatrolInspection
        private lateinit var name_ : String
        private lateinit var help_ : String
        private lateinit var patrolFileName_: String

        fun bootstrap(bootstrap: BootstrapPatrol) {
            this.bootstrap = bootstrap
        }

        fun onInspection(onInspection: PatrolInspection) {
            onInspection_ = onInspection
        }

        fun name(init: () -> String) {
            name_ = init()
        }

        fun help(init: () -> String) {
            help_ = init()
        }

        fun patrolFileName(init: () -> String) {
            patrolFileName_ = init()
        }

        fun build() : Patrol {
            if (!::onInspection_.isInitialized) throw IllegalArgumentException("onInspection block missing")
            if (!::name_.isInitialized) throw IllegalArgumentException("name not set")
            if (!::help_.isInitialized) throw IllegalArgumentException("help not set")
            if (!::patrolFileName_.isInitialized) {
                patrolFileName_ = "$name_.yaml"
            }

            return Patrol(
                name = name_,
                help = help_,
                onInspection = onInspection_,
                patrolFileName = patrolFileName_,
                bootstrap = bootstrap,
                debug = debug
            )
        }
    }
}

@DslMarker
annotation class PatrolDsl
fun CommandArgs.patrol(block: Patrol.Builder.() -> Unit) {
    val patrol = Patrol.Builder().apply(block).build()

    PatrolCommand(patrol).main(this)
}