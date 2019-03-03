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

package io.skerna.reaction.impl

import io.skerna.lbase.synchronized
import io.skerna.reaction.*
import kotlin.jvm.Synchronized


/**
 * Create a reactSuspend that hasn't completed yet
 */
internal class ReactionImpl<T> : Reaction<T>, Handler<ReactionResult<T>> {

    private var failed: Boolean = false
    private var succeeded: Boolean = false
    private var handler: Handler<ReactionResult<T>>? = null
    private var result: T? = null
    private var throwable: Throwable? = null
    private var handlerException:Handler<Throwable>? = null

    /**
     * Has the reactSuspend completed?
     *
     *
     * It's completed if it's either succeeded or failed.
     *
     * @return true if completed, false if not
     */
    override fun isCompleted(): Boolean {
        return failed || succeeded
    }

    /**
     * The result of the operation. This will be null if the operation failed.
     */
    override fun result(): T? {
        if(failed()){
            this.handlerException?.handle(cause())
        }
        return result
    }
    /**
     * The result of the operation. This never be null if the operation failed.
     */
    override fun resultOrDefault(default: T): T {
        return result?:default
    }

    /**
     * An exception describing failure. This will be null if the operation succeeded.
     */
    override fun cause(): Throwable {
        return throwable?:NoStackTraceThrowable("Unknow error cause, reactSuspend not report the cause of the failure")
    }

    /**
     * Did it succeeed?
     */
    @Synchronized
    override fun succeeded(): Boolean {
        return succeeded
    }

    /**
     * Did it fail?
     */
    @Synchronized
    override fun failed(): Boolean {
        return failed
    }

    override fun setExceptionHandler(handler: Handler<Throwable>) = apply {
        this.handlerException = handler
    }

    override fun setExceptionHandler(exHandler: (asyncResult: Throwable) -> Unit)= apply {
        this.handlerException = Handler.create { exHandler(it) }
    }

    /**
     * Set a handler for the result. It will get called when it's complete
     */
    override fun setHandler(handler: Handler<ReactionResult<T>>) = apply{
        var callHandler: Boolean = isCompleted()

        synchronized(this){
            if (!callHandler) {
                this.handler = handler
            }
        }
        if (callHandler) {
            handler.handle(this)
        }
    }

    override fun setHandler(handler: (reactionResult: ReactionResult<T>) -> Unit) =  apply{
        var handlerParsed = Handler.create(handler)
        setHandler(handlerParsed)
    }

    @Synchronized
    override fun getHandler(): Handler<ReactionResult<T>> {
        return handler!!
    }


    override fun complete(result: T) {
        if (!tryComplete(result)) {
            throw IllegalStateException("Result is already complete: " + if (succeeded) "succeeded" else "failed")
        }
    }

    override fun complete() {
        if (!tryComplete()) {
            throw IllegalStateException("Result is already complete: " + if (succeeded) "succeeded" else "failed")
        }
    }

    override fun fail(cause: Throwable?) {
        if (!tryFail(cause)) {
            throw IllegalStateException("Result is already complete: " + if (succeeded) "succeeded" else "failed")
        }
    }

    override fun fail(failureMessage: String) {
        if (!tryFail(failureMessage)) {
            throw IllegalStateException("Result is already complete: " + if (succeeded) "succeeded" else "failed")
        }
    }


    override fun tryComplete(result: T?): Boolean {
        var h: Handler<ReactionResult<T>>? = handler
        synchronized(this){
            if (succeeded || failed) {
                return false
            }
            this.result = result
            succeeded = true
            handler = null
        }

        if (h != null) {
           h.handle(this)
        }
        return true
    }

    fun handle(ar: Reaction<T>) {
        if (ar.succeeded()) {
            complete(ar.result()!!)
        } else {
            fail(ar.cause())
        }
    }

    override fun completer(): Handler<ReactionResult<T>> {
        return this
    }

    /**
     * Succeed or fail this reactSuspend with the [ReactionResult] event.
     *
     * @param reactionResult the async result to getHandler
     */
    override fun handle(value: ReactionResult<T>) {
        if (value.succeeded()) {
            complete(value.result()!!)
        } else {
            fail(value.cause())
        }
    }


    override fun tryFail(cause: Throwable?): Boolean {
        var h: Handler<ReactionResult<T>>?=null
        synchronized(this){
            if (succeeded || failed) {
                return false
            }
            this.throwable = cause ?: NoStackTraceThrowable(null)
            failed = true
            h = handler
            handler = null
        }
        if(cause!=null){
            handlerException?.handle(cause)
        }
        if (h != null) {
            h?.handle(this)?:throw IllegalStateException("Handler not initialized")
        }
        return true
    }

    override fun tryFail(failureMessage: String): Boolean {
        return tryFail(NoStackTraceThrowable(failureMessage))
    }

    override fun toString(): String {
        synchronized(this){
            if (succeeded) {
                return "Reaction{result=$result}"
            }
            return if (failed) {
                "Reaction{cause=" + throwable!!.message + "}"
            } else "Reaction{unresolved}"
        }
    }

    /**
     * Try to set the result. When it happens, any handler will be called, if there is one, and the reactSuspend will be marked as completed.
     *
     * @return false when the reactSuspend is already completed
     */
    override fun tryComplete(): Boolean {
        return tryComplete(null)
    }
}
