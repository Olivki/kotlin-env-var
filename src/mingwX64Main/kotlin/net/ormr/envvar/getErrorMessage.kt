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
 * ============================= OKIO LICENSE =============================
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ormr.envvar

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKStringFromUtf16
import platform.windows.*

private const val MESSAGE_MAX_SIZE = 2048

// from okio, https://github.com/square/okio/blob/master/okio/src/mingwX64Main/kotlin/okio/-Windows.kt#L39
// changes: return null in case of no error message being set, extract local buffer size to constant,
// use 'FormatMessageW' instead of 'FormatMessageA' and decode using Kotlin built-in functions
internal fun getErrorMessage(error: DWORD): String? {
    // no error message has been set
    if (error == 0u) return null

    return memScoped {
        val buffer = allocArray<WCHARVar>(MESSAGE_MAX_SIZE)
        FormatMessageW(
            dwFlags = (FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
            lpSource = null,
            dwMessageId = error,
            dwLanguageId = (SUBLANG_DEFAULT * 1024 + LANG_NEUTRAL).toUInt(), // MAKELANGID macro.
            lpBuffer = buffer,
            nSize = MESSAGE_MAX_SIZE.toUInt(),
            Arguments = null,
        )
        buffer.toKStringFromUtf16().trim()
    }
}