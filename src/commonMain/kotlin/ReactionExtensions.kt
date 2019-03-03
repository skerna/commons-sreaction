/*
 * Copyright (c)  2019  SKERNA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.skerna.reaction

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun<T> Reaction<T>.asCoroutine() = suspendCoroutine<T>{ next ->
    setHandler { result ->
        if(result.succeeded()){
            next.resume(result.result()!!)
        }else{
            next.resumeWithException(result.cause())
        }
    }
}

@ExperimentalCoroutinesApi
fun<T> Deferred<T>.asReaction():Reaction<T> {
    val future = Reaction.react<T>()

    invokeOnCompletion {
        if (it != null) {
            future.fail(it)
        } else {
            future.complete(getCompleted())
        }
    }
    return future
}


/**
 * Transforma una corutina en un reactSuspend que acepta listener antes de establecer el Reaction result
 * como completado exitosamente o en error
 * @param errorListener
 * @param successListener
 *
 * Estos parametros son funciones que se disparan unicamente cuando la coroutina a terminado su ejecucion y se
 * a resuelto
 */
@ExperimentalCoroutinesApi
fun<T> Deferred<T>.asReaction(errorListener:(Throwable)->Unit, successListener:(()->Unit)?=null):Reaction<T> {
    val future = Reaction.react<T>()
    invokeOnCompletion {
        if (it != null) {
            try {
                errorListener(it)
                future.fail(it)
            } catch (ex:Exception){
                future.fail(ex)
            }
        } else {
            try {
                if(successListener != null){
                    successListener()
                }
                future.complete(getCompleted())
            }catch (ex:Exception){
                future.fail(ex)
            }

        }
    }
    return future
}






fun<T> Deferred<T>.asReactionBoolean():Reaction<Boolean> {
    val future = Reaction.react<Boolean>()

    invokeOnCompletion {
        if (it != null) {
            future.fail(it)
        } else {
            future.complete(true)
        }
    }

    return future
}

@ExperimentalCoroutinesApi
fun<T,R> Reaction<T>.mapWithCoroutine(block:suspend (data:T)->R):Reaction<R>{
    return GlobalScope.async {
        val a = asCoroutine()
        block(a)
    }.asReaction()
}