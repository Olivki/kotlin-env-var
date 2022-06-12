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
import net.ormr.envvar.Environment.get
import platform.windows.*

/**
 * Represents an environment containing the system variables available to the process.
 *
 * This makes use of [GetEnvironmentVariableW](https://docs.microsoft.com/en-us/windows/win32/api/processenv/nf-processenv-getenvironmentvariablew),
 * [SetEnvironmentVariableW](https://docs.microsoft.com/en-us/windows/win32/api/processenv/nf-processenv-setenvironmentvariablew)
 * and [GetEnvironmentStringsW](https://docs.microsoft.com/en-us/windows/win32/api/processenv/nf-processenv-getenvironmentstringsw).
 */
public actual object Environment {
    // based on https://docs.microsoft.com/en-us/windows/win32/api/processenv/nf-processenv-getenvironmentvariablew#parameters
    // the maximum length of a user defined environment variable on Windows is 32_767 characters, so we set the default
    // buffer size to (32_767 + 1), +1 to accommodate for the null terminator character.
    private const val DEFAULT_BUFFER_SIZE = 32_767 + 1

    /**
     * Returns the environment variable with the given [name], or `null` if none is found.
     *
     * Environment variables on Windows are case-insensitive, meaning that
     * `Environment["name"] == Environment["NAME"]` should be `true` on Windows systems.
     *
     * @throws [NativeException] if an error that is not [ERROR_ENVVAR_NOT_FOUND] was encountered when invoking
     * [GetEnvironmentVariableW]
     *
     * @see [GetEnvironmentVariableW]
     */
    public actual operator fun get(name: String): String? = memScoped {
        // while there is a maximum length for user defined environment variables, there is none defined for variables
        // created by programs, so we need to support dynamically resizing
        val defaultBuffer = allocArray<UShortVar>(DEFAULT_BUFFER_SIZE)
        // using 'GetEnvironmentVariableW' explicitly instead of 'GetEnvironmentVariable' to make sure we're retrieving
        // UTF-16 strings
        val result = GetEnvironmentVariableW(name, defaultBuffer, DEFAULT_BUFFER_SIZE.toUInt())
        when {
            // return value 0 signifies failure
            result == 0u -> when (val error = GetLastError()) {
                // ERROR_ENVVAR_NOT_FOUND signifies that the env var was not found in the block, so we return null
                ERROR_ENVVAR_NOT_FOUND.toUInt() -> null
                else -> throw NativeException(
                    error.toLong(),
                    getErrorMessage(error) ?: "Could not retrieve variable '$name', error: $error",
                )
            }
            // if the given buffers size is not large enough then the returned value is the required size to hold the
            // env var
            result > DEFAULT_BUFFER_SIZE.toUInt() -> {
                // this approach is susceptible to a race condition wherein the variable stored under 'name' may have
                // been modified between us invoking 'GetEnvironmentVariableW' the first time, and this time. this is
                // however, not very likely to happen, but it is a possibility. Unless this becomes a problem, we'll
                // stick to this approach, as it's vastly simpler than the race condition safe approach.
                val sizedBuffer = allocArray<UShortVar>(result.toLong())
                GetEnvironmentVariableW(name, sizedBuffer, result)
                sizedBuffer.toKStringFromUtf16()
            }
            // if none of the above branches match, then that means the function has succeeded.
            else -> defaultBuffer.toKStringFromUtf16()
        }
    }

    /**
     * Returns `true` if there exists an environment variable with [name], otherwise `false`.
     *
     * This is equivalent to doing `get(name) != null` and can therefore throw the same exceptions as [get] can.
     *
     * Environment variables on Windows are case-insensitive, meaning that
     * `("name" in Environment) == ("NAME" in Environment)` should be `true` on Windows systems.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     *
     * @throws [NativeException] if [get] throws an exception
     */
    public actual operator fun contains(name: String): Boolean = get(name) != null

    /**
     * Creates a new environment variable with [name] and [value], stored as `$name=$value`.
     *
     * As environment variables are case-insensitive on Windows, `Environment["name"] = "value"` and
     * `Environment["NAME"] = "value"` will override the same variable.
     *
     * Note that this does *not* create a new environment variable entry on the actual system, it only creates a new
     * variable for the currently running process.
     *
     * @throws [NativeException] if the `SetEnvironmentVariableW` function failed
     *
     * @see [SetEnvironmentVariableW]
     */
    public operator fun set(name: String, value: String) {
        // using 'SetEnvironmentVariableW' explicitly instead of 'SetEnvironmentVariable' to make sure we're storing
        // UTF-16 strings
        val result = SetEnvironmentVariableW(name, value)
        if (result == 0) {
            val error = GetLastError()
            throw NativeException(
                error.toLong(),
                getErrorMessage(error) ?: "Could not create environment variable '$name'='$value', error: $error",
            )
        }
    }

    /**
     * Returns a map containing all the environment variables defined for the program.
     *
     * If no environment variables could be retrieved, an empty map is returned.
     *
     * The returned map is read-only, and is ***NOT*** case-insensitive, `map["name"] == map["NAME"]` is `false`, unlike
     * how [get] works.
     *
     * @throws [InvalidEnvironmentVariableException] if a retrieved environment variable wasn't formatted as `$name=$value`
     *
     * @see [GetEnvironmentStringsW]
     */
    public actual fun toMap(): Map<String, String> {
        // we're explicitly using 'GetEnvironmentStringsW' rather than 'GetEnvironmentStrings' to make sure we're
        // always retrieving the UTF-16 variant, rather than the ASCII variant.
        val pointer = GetEnvironmentStringsW() ?: return emptyMap()
        var index = 0
        var value = pointer.pointed.value

        return buildMap {
            while (value != 0.toUShort()) {
                // this could be done more efficiently

                val variable = buildString {
                    while (value != 0.toUShort()) {
                        append(value.toInt().toChar())
                        index++
                        value = pointer[index]
                    }
                }

                if ('=' !in variable) {
                    throw InvalidEnvironmentVariableException("No '=' delimiter found in environment variable: $variable")
                }

                put(variable.substringBefore('='), variable.substringAfter('='))

                index++
                value = pointer[index]
            }

            FreeEnvironmentStringsW(pointer)
        }
    }
}

