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

package io.skerna.futures

import kotlin.js.JsName


@JsName("FailedReaction")
class FailedReaction<T> : Reaction<T> {

    private val cause: Throwable


    /**
     * Create a react that has already failed
     * @param t the throwable
     */
    internal constructor(t: Throwable?) {
        cause = t ?: NoStackTraceThrowable(null)
    }

    /**
     * Create a react that has already failed
     * @param failureMessage the failure message
     */
    internal constructor(failureMessage: String) : this(NoStackTraceThrowable(failureMessage)) {}

    /**
     * Has the react completed?
     * <p>
     * It's completed if it's either succeeded or failed.
     *
     * @return true if completed, false if not
     */
    override fun isCompleted(): Boolean {
        return true
    }

    /**
     * @return the handler for the result
     */
    override fun getHandler(): Handler<AsyncResult<T>>? {
        return null
    }

    override fun setHandler(handler: Handler<AsyncResult<T>>): Reaction<T> {
        handler.handle(this)
        return this
    }

    override fun setHandler(handler: (asyncResult: AsyncResult<T>) -> Unit): Reaction<T> {
        handler(this)
        return this
    }

    override fun complete(result: T) {
        throw IllegalStateException("Result is already complete: failed")
    }

    override fun complete() {
        throw IllegalStateException("Result is already complete: failed")
    }

    override fun fail(cause: Throwable?) {
        throw IllegalStateException("Result is already complete: failed")
    }

    override fun fail(failureMessage: String) {
        throw IllegalStateException("Result is already complete: failed")
    }

    override fun tryComplete(result: T?): Boolean {
        return false
    }

    override fun tryComplete(): Boolean {
        return false
    }

    override fun tryFail(cause: Throwable?): Boolean {
        return false
    }

    override fun tryFail(failureMessage: String): Boolean {
        return false
    }

    override fun result(): T? {
        return null
    }

    override fun cause(): Throwable {
        return cause
    }

    override fun succeeded(): Boolean {
        return false
    }

    override fun failed(): Boolean {
        return true
    }

    override fun handle(asyncResult: AsyncResult<T>) {
        throw IllegalStateException("Result is already complete: failed")
    }

    override fun toString(): String {
        return "Reaction{cause=" + cause.message + "}"
    }


}