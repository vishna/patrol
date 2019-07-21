# Patrol

## Overview

Patrol is a Kotlin/JVM boilerplate code for building CLI tools that involve watch files - mainly code generators that live outside kapt realm.

Patrol is using [clikt](https://ajalt.github.io/clikt/) to parse CLI arguments and [watchservice-ktx](https://github.com/vishna/watchservice-ktx) to subscribe to file system notifications.

Patrol relies on having YAML configuration file, called patrol file, just like make expects `Makefile`.

`patrol --run-once`

Patrol by default will run constantly watching for file changes but it can also run once which can be useful for CI/CD systems or hooking up to some other script system.

`patrol --dry-run`

Patrol can implement `dry run` mode - it's useful for debugging issues without necessairly writing changes to the target directory.

## Getting started

This repository is hosted via [jitpack](https://jitpack.io/) since it's by far the easiest delivery method while also being pretty transparent to the developer.

Make sure you have added jitpack to the list of your repositories:

```kotlin
maven("https://jitpack.io")
```

Then simply add the `patrol` dependency

```kotlin
dependencies {
    compile("com.github.vishna:patrol:master-SNAPSHOT")
}
```

## Example usage

### Patrol File

```
---
- name: MyWatcher # mandatory
  source: /path/to/watch # mandatory
  target: /path/to/print/output/to # optional, made available though a map
```

### Patrol DSL

```kotlin
fun main(args: CommandArgs) = args.patrol {

    name {
        "nextgen-codegen"
    }

    help {
        "The dopest code generator."
    }

    onInspection { watchPoint, dryRun ->

        watchPoint.source // what you are observing

        if (!dryRun) {
            Log.save..watchPoint.name // pretty logs
        }
    }

    // this is optional but highly recommended
    bootstrap { patrolFile ->
        // provide template for the patrol file
        false // ...or not
    }
}
```