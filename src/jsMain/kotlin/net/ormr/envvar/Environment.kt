/*
 * Copyright 2022 Oliver Berg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ormr.envvar


/**
 * Represents an environment containing the system variables available to the process.
 *
 * This makes use of the [Node.js process.env](https://nodejs.org/api/process.html#processenv) object, which means that
 * this will only work on Node.js servers, and not in the browser.
 */
public actual object Environment {
    /**
     * Returns the environment variable with the given [name], or `null` if none is found.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     */
    public actual operator fun get(name: String): String? = process.env[name]?.unsafeCast<String>()

    /**
     * Creates a new environment variable with [name] and [value], stored as `$name=$value`.
     *
     * The new variable is only set for the current process.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     */
    public operator fun set(name: String, value: String) {
        process.env[name] = value
    }

    /**
     * Returns `true` if there exists an environment variable with [name], otherwise `false`.
     *
     * This is equivalent to doing `get(name) != null`.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     */
    public actual operator fun contains(name: String): Boolean = get(name) != null

    /**
     * Returns a map containing all the environment variables defined for the program.
     *
     * If no environment variables could be retrieved, an empty map is returned.
     *
     * The returned map is read-only.
     */
    public actual fun toMap(): Map<String, String> {
        val env = process.env ?: return emptyMap()
        return buildMap {
            for (key in keys(env)) {
                put(key, env[key].unsafeCast<String>())
            }
        }
    }
}

private external val process: Process

private external interface Process {
    val env: dynamic
}

private fun keys(json: dynamic) = js("Object").keys(json).unsafeCast<Array<String>>()