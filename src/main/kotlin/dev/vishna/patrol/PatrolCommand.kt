package dev.vishna.patrol

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vishna.emojilog.safe.safely
import dev.vishna.emojilog.std.*
import dev.vishna.watchservice.KWatchChannel
import dev.vishna.watchservice.KWatchEvent
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

/**
 * Does all the heavy lifting of parsing command line arguments, optional bootstrapping, parsing
 * of the input patrol yaml and finally setting up respective file watchers
 */
class PatrolCommand(private val patrol: Patrol) :
    CliktCommand(
        name = patrol.name,
        help = patrol.help
    ), CoroutineScope {

    val log by lazy { defaultLogger(printStackTrace = patrol.debug || debug) }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        log.boom..throwable
    }

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = job + coroutineExceptionHandler

    private val runOnce by option(help = "Runs ${patrol.name} only once, doesn't watch file system, useful for CI/CD.").flag()
    private val dryRun by option(help = "Runs ${patrol.name} in a dry mode").flag()
    private val debug by option(help = "Runs ${patrol.name} in a debug mode").flag()

    override fun run() {
        runBlocking(coroutineContext) {
            runInternal(this@PatrolCommand)
        }
        job.cancel()
    }

    private suspend fun runInternal(args: PatrolCommand) = coroutineScope {

        val patrolFile = File(pwd, patrol.patrolFileName)

        if (!patrolFile.exists()) {
            if (patrol.bootstrap?.invoke(patrolFile) != true) {
                log.boom.."No file named ${patrolFile.name} found in the working directory"
                exitProcess(1)
            }
        }

        var lastJob: Job? = null
        val ongoingPatrols = ArrayList<KWatchChannel>()

        val patrolChannel = patrolFile.asWatchChannel(scope = this)

        patrolChannel.consumeEach { event ->

            if (ongoingPatrols.isNotEmpty()) {
                ongoingPatrols.forEach { it.close() }
                ongoingPatrols.clear()
            }
            lastJob?.cancel()

            lastJob = this@PatrolCommand.launch {

                val _runnedOnce = if (args.runOnce) {
                    HashMap<KWatchChannel, CompletableDeferred<Boolean>>()
                } else { null }

                dispatchPatrols(patrolFile, ongoingPatrols, event.kind, _runnedOnce)

                if (args.runOnce) {
                    if (debug) log.alert.."awaiting ${_runnedOnce?.size} channel(s) to close"
                    _runnedOnce?.values?.forEachIndexed { index, complete ->
                        val completed = complete.await()
                        if (debug) log.alert.."channel $index closed $completed"
                    } // waiting for each channel to complete at least once
                    patrolChannel.close()
                }
            }
        }

        log.wave.."KTHXBAI"
        System.exit(0)
        null
    }

    /**
     * Begins all the monitoring described in the patrol file
     */
    private suspend fun dispatchPatrols(
        file: File,
        _channels: ArrayList<KWatchChannel>,
        kind: KWatchEvent.Kind,
        _runnedOnce: HashMap<KWatchChannel, CompletableDeferred<Boolean>>?
    ) {

        val channels = file
            .readText()
            .asYamlArray()
            .mapNotNull {
                log.boom.safely {
                    WatchPoint(it as Map<String, Any>)
                }
            }
            .mapNotNull { watchPoint ->
                val watchPointFile = watchPoint.source.asFile()
                if (watchPointFile.exists()) {
                    watchPointFile.asWatchChannel(tag = watchPoint, scope = this)
                } else {
                    if (watchPoint.source.isBlank()) {
                        log.boom.."No file specified for ${watchPoint.name}"
                    } else {
                        log.boom.."File ${watchPoint.source} doesn't exist for ${watchPoint.name}"
                    }

                    null
                }
            }
        _channels += channels
        _runnedOnce?.apply {
            channels.forEach { this[it] = CompletableDeferred() }
        }

        if (kind == KWatchEvent.Kind.Initialized) {
            log.conf.."${file.name} loaded"
        } else {
            log.conf.."${file.name} changed"
        }

        channels.forEach { channel ->
            launch {
                channel.consumeEach { event ->
                    val watchPoint = event.tag as? WatchPoint ?: return@consumeEach

                    // omit directory events, react to initial events
                    val skipExecution = when (event.kind) {
                        KWatchEvent.Kind.Deleted, KWatchEvent.Kind.Created, KWatchEvent.Kind.Initialized -> false
                        KWatchEvent.Kind.Modified -> event.file.isDirectory
                    }

                    if (skipExecution) return@consumeEach

                    log.edit..event.file.path

                    log.boom.safely {
                        patrol.onInspection(this@PatrolCommand, watchPoint, dryRun, runOnce)
                    }

                    if (runOnce) {
                        _runnedOnce?.get(channel)?.complete(true)
                        channel.close()
                    }
                }
            }
        }
    }
}

