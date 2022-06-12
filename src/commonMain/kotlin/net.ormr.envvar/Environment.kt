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
 */
public expect object Environment {
    /**
     * Returns the environment variable with the given [name], or `null` if none is found.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     */
    public operator fun get(name: String): String?

    /**
     * Returns `true` if there exists an environment variable with [name], otherwise `false`.
     *
     * This is equivalent to doing `get(name) != null` and can therefore throw the same exceptions as [get] can.
     *
     * Note that whether an environment variable name is case significant or not depends on the system.
     */
    public operator fun contains(name: String): Boolean

    /**
     * Returns a map containing all the environment variables defined for the program.
     *
     * What happens if the system does not support environment variables is implementation specific.
     *
     * The structure of the returned map is implementation specific.
     */
    public fun toMap(): Map<String, String>
}