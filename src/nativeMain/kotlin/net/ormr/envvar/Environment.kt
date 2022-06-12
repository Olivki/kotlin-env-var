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

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Represents an environment containing the system variables available to the process.
 *
 * This makes use of [getenv](https://man7.org/linux/man-pages/man3/getenv.3.html), [putenv](https://man7.org/linux/man-pages/man3/putenv.3.html)
 * and [environ](https://man7.org/linux/man-pages/man7/environ.7.html).
 */
public actual object Environment {
    /**
     * Returns the environment variable with the given [name], or `null` if none is found.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     *
     * @see [getenv]
     */
    public actual operator fun get(name: String): String? = getenv(name)?.toKStringFromUtf8()

    /**
     * Creates a new environment variable with [name] and [value], stored as `$name=$value`.
     *
     * The new variable is only set for the current process.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     *
     * @throws [NativeException] if the [putenv] function failed
     *
     * @see [putenv]
     */
    public operator fun set(name: String, value: String) {
        // will display as an error in IDE on Windows, but is required to compile it on Linux
        val result = putenv("$name=$value".cstr)
        if (result != 0) {
            val error = errno
            throw NativeException(
                error.toLong(),
                getErrorMessage(error) ?: "Could not create environment variable '$name'='$value', error: $error",
            )
        }
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
     *
     * @throws [InvalidEnvironmentVariableException] if a retrieved environment variable wasn't formatted as `$name=$value`
     *
     * @see [environ]
     */
    public actual fun toMap(): Map<String, String> {
        val pointer = environ ?: return emptyMap()
        var index = 0
        var value = pointer.pointed.value ?: return emptyMap()

        return buildMap {
            while (true) {
                val variable = value.toKStringFromUtf8()

                if ('=' !in variable) {
                    throw InvalidEnvironmentVariableException("No '=' delimiter found in environment variable: $variable")
                }

                put(variable.substringBefore('='), variable.substringAfter('='))

                index++
                value = pointer[index] ?: break
            }
        }
    }
}