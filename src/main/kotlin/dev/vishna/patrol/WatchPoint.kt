package dev.vishna.patrol

import java.lang.IllegalStateException

/**
 * Describes what files/directories are under patrol's surveillance.
 *
 * NOTE: avoiding map property delegation here, to force exception as early
 * as the object is instantiated.
 */
class WatchPoint(map : Map<String, Any>) : Map<String, Any> by map {
    /**
     * Path or file the patrol is observing
     */
    val source : FilePath = map["source"] as String? ?: throw IllegalStateException("source not defined in $map")

    /**
     * Human digestible description of the watch point
     */
    val name : String = map["name"] as String? ?: throw IllegalStateException("name not defined in $map")
}