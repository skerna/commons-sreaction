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

class SucceededReaction<T>(private val result: T) : Reaction<T> {
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
        throw IllegalStateException("Result is already complete: succeeded")
    }

    override fun complete() {
        throw IllegalStateException("Result is already complete: succeeded")
    }

    override fun fail(cause: Throwable?) {
        throw IllegalStateException("Result is already complete: succeeded")
    }

    override fun fail(failureMessage: String) {
        throw IllegalStateException("Result is already complete: succeeded")
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

    override fun result(): T {
        return result
    }

    override fun cause(): Throwable {
        throw IllegalStateException("Result is already complete: succeeded")
    }

    override fun succeeded(): Boolean {
        return true
    }

    override fun failed(): Boolean {
        return false
    }

    override fun handle(asyncResult: AsyncResult<T>) {
        throw IllegalStateException("Result is already complete: succeeded")
    }

    override fun toString(): String {
        return "Reaction{result=$result}"
    }
}