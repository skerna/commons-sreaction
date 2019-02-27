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

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.js.JsName


interface Reaction<T> : AsyncResult<T>, Handler<AsyncResult<T>> {

    /**
     * Has the react completed?
     *
     *
     * It's completed if it's either succeeded or failed.
     *
     * @return true if completed, false if not
     */
    @JsName("isCompleted")
    fun isCompleted(): Boolean

    /**
     * @return the handler for the result
     */
    @JsName("getHandler")
    fun getHandler(): Handler<AsyncResult<T>>?

    /**
     * Set a handler for the result.
     *
     *
     * If the react has already been completed it will be called immediately. Otherwise it will be called when the
     * react is completed.
     *
     * @param handler  the Handler that will be called with the result
     * @return a reference to this, so it can be used fluently
     */
    fun setHandler(handler: Handler<AsyncResult<T>>): Reaction<T>

    @JsName("setHandler")
    fun setHandler(handler: (asyncResult: AsyncResult<T>) -> Unit): Reaction<T>

    /**
     * Set the result. Any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @param result  the result
     */
    @JsName("complete")
    fun complete(result: T)

    /**
     * Set a null result. Any handler will be called, if there is one, and the react will be marked as completed.
     */
    @JsName("completeEmpy")
    fun complete()

    /**
     * Set the failure. Any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @param cause  the failure cause
     */
    @JsName("fail")
    fun fail(cause: Throwable?)

    /**
     * Try to set the failure. When it happens, any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @param failureMessage  the failure message
     */
    @JsName("failWithMessage")
    fun fail(failureMessage: String)

    /**
     * Set the failure. Any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @param result  the result
     * @return false when the react is already completed
     */
    @JsName("tryComplete")
    fun tryComplete(result: T?): Boolean

    /**
     * Try to set the result. When it happens, any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @return false when the react is already completed
     */
    @JsName("tryCompleteDefault")
    fun tryComplete(): Boolean

    /**
     * Try to set the failure. When it happens, any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @param cause  the failure cause
     * @return false when the react is already completed
     */
    @JsName("tryFail")
    fun tryFail(cause: Throwable?): Boolean

    /**
     * Try to set the failure. When it happens, any handler will be called, if there is one, and the react will be marked as completed.
     *
     * @param failureMessage  the failure message
     * @return false when the react is already completed
     */
    @JsName("tryFailWithMessage")
    fun tryFail(failureMessage: String): Boolean

    /**
     * The result of the operation. This will be null if the operation failed.
     *
     * @return the result or null if the operation failed.
     */
    override fun result(): T?

    /**
     * A Throwable describing failure. This will be null if the operation succeeded.
     *
     * @return the cause or null if the operation succeeded.
     */
    override fun cause(): Throwable

    /**
     * Did it succeed?
     *
     * @return true if it succeded or false otherwise
     */
    override fun succeeded(): Boolean

    /**
     * Did it fail?
     *
     * @return true if it failed or false otherwise
     */
    override fun failed(): Boolean

    /**
     * Compose this react with a provided `next` react.
     *
     *
     *
     * When this (the one on which `compose` is called) react succeeds, the `handler` will be called with
     * the completed value, this handler should complete the next react.
     *
     *
     *
     * If the `handler` throws an exception, the returned react will be failed with this exception.
     *
     *
     *
     * When this react fails, the failure will be propagated to the `next` react and the `handler`
     * will not be called.
     *
     * @param handler the handler
     * @param next the next react
     * @return the next react, used for chaining
     */
    fun <U> compose(handler: Handler<T>, next: Reaction<U>): Reaction<U> {
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    try {
                        handler.handle(asyncResult.result()!!)
                    } catch (err: Throwable) {
                        if (next.isCompleted()) {
                            throw err
                        }
                        next.fail(err)
                    }

                } else {
                    next.fail(asyncResult.cause())
                }
            }

        })
        return next
    }

    /**
     * Compose this react with a `mapper` function.
     *
     *
     *
     * When this react (the one on which `compose` is called) succeeds, the `mapper` will be called with
     * the completed value and this mapper returns another react object. This returned react completion will complete
     * the react returned by this method call.
     *
     *
     *
     * If the `mapper` throws an exception, the returned react will be failed with this exception.
     *
     *
     *
     * When this react fails, the failure will be propagated to the returned react and the `mapper`
     * will not be called.
     *
     * @param mapper the mapper function
     * @return the composed react
     */
    fun <U> compose(mapper: Function<T, Reaction<U>>?): Reaction<U> {
        if (mapper == null) {
            throw NullPointerException()
        }
        val ret = react<U>()
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    val apply: Reaction<U>
                    try {
                        apply = mapper(asyncResult.result()!!)
                    } catch (e: Throwable) {
                        ret.fail(e)
                        return
                    }

                    apply.setHandler(ret)
                } else {
                    ret.fail(asyncResult.cause())
                }
            }

        })
        return ret
    }

    /**
     * Apply a `mapper` function on this react.
     *
     *
     *
     * When this react succeeds, the `mapper` will be called with the completed value and this mapper
     * returns a value. This value will complete the react returned by this method call.
     *
     *
     *
     * If the `mapper` throws an exception, the returned react will be failed with this exception.
     *
     *
     *
     * When this react fails, the failure will be propagated to the returned react and the `mapper`
     * will not be called.
     *
     * @param mapper the mapper function
     * @return the mapped react
     */
    override fun <U> map(mapper: Function<T, U>): Reaction<U> {
        val ret = react<U>()
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    val mapped: U
                    try {
                        mapped = mapper(asyncResult.result()!!)
                    } catch (e: Throwable) {
                        ret.fail(e)
                        return
                    }

                    ret.complete(mapped)
                } else {
                    ret.fail(asyncResult.cause())
                }
            }

        })
        return ret
    }

    /**
     * Map the result of a react to a specific `value`.
     *
     *
     *
     * When this react succeeds, this `value` will complete the react returned by this method call.
     *
     *
     *
     * When this react fails, the failure will be propagated to the returned react.
     *
     * @param value the value that eventually completes the mapped react
     * @return the mapped react
     */
    override fun <V> map(value: V): Reaction<V> {
        val ret = react<V>()
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    ret.complete(value)
                } else {
                    ret.fail(asyncResult.cause())
                }
            }

        })
        return ret
    }

    /**
     * Map the result of a react to `null`.
     *
     *
     *
     * This is a conveniency for `react.map((T) null)` or `react.map((Void) null)`.
     *
     *
     *
     * When this react succeeds, `null` will complete the react returned by this method call.
     *
     *
     *
     * When this react fails, the failure will be propagated to the returned react.
     *
     * @return the mapped react
     */
    //  fun <V> mapEmpty(): Reaction<V> {
    //     return AsyncResult.mapEmpty()
    // }

    /**
     * Succeed or fail this react with the [AsyncResult] asyncResult.
     *
     * @param asyncResult the async result to getHandler
     */
    override fun handle(asyncResult: AsyncResult<T>)

    /**
     * @return an handler completing this react
     */
    fun completer(): Handler<AsyncResult<T>> {
        return this
    }

    /**
     * Handles a failure of this Reaction by returning the result of another Reaction.
     * If the mapper fails, then the returned react will be failed with this failure.
     *
     * @param mapper A function which takes the exception of a failure and returns a new react.
     * @return A recovered react
     */
    fun recover(mapper: Function<Throwable, Reaction<T>>?): Reaction<T> {
        if (mapper == null) {
            throw NullPointerException()
        }
        val ret = react<T>()
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    ret.complete(result()!!)
                } else {
                    val mapped: Reaction<T>
                    try {
                        mapped = mapper(asyncResult.cause())
                    } catch (e: Throwable) {
                        ret.fail(e)
                        return
                    }

                    mapped.setHandler(ret)
                }
            }

        })
        return ret
    }

    /**
     * Apply a `mapper` function on this react.
     *
     *
     *
     * When this react fails, the `mapper` will be called with the completed value and this mapper
     * returns a value. This value will complete the react returned by this method call.
     *
     *
     *
     * If the `mapper` throws an exception, the returned react will be failed with this exception.
     *
     *
     *
     * When this react succeeds, the result will be propagated to the returned react and the `mapper`
     * will not be called.
     *
     * @param mapper the mapper function
     * @return the mapped react
     */
    fun otherwise(mapper: Function<Throwable, T>?): Reaction<T> {
        if (mapper == null) {
            throw NullPointerException()
        }
        val ret = react<T>()
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    ret.complete(result()!!)
                } else {
                    val value: T
                    try {
                        value = mapper(asyncResult.cause())
                    } catch (e: Throwable) {
                        ret.fail(e)
                        return
                    }

                    ret.complete(value)
                }
            }

        })
        return ret
    }

    /**
     * Map the failure of a react to a specific `value`.
     *
     *
     *
     * When this react fails, this `value` will complete the react returned by this method call.
     *
     *
     *
     * When this react succeeds, the result will be propagated to the returned react.
     *
     * @param value the value that eventually completes the mapped react
     * @return the mapped react
     */
    override fun otherwise(value: T): Reaction<T> {
        val ret = react<T>()
        setHandler(object : Handler<AsyncResult<T>> {
            override fun handle(asyncResult: AsyncResult<T>) {
                if (asyncResult.succeeded()) {
                    ret.complete(result()!!)
                } else {
                    ret.complete(value)
                }
            }
        })
        return ret
    }

    /**
     * Map the failure of a react to `null`.
     *
     *
     *
     * This is a convenience for `react.otherwise((T) null)`.
     *
     *
     *
     * When this react fails, the `null` value will complete the react returned by this method call.
     *
     *
     *
     * When this react succeeds, the result will be propagated to the returned react.
     *
     * @return the mapped react
     */
    //   override fun otherwiseEmpty(): Reaction<T> {
//        return super@AsyncResult.otherwiseEmpty()
    //  }

    companion object {

        suspend fun <T> react(action: suspend () -> T): Reaction<T> {
            val fut = react<T>()
            try {
                var result:T? = action()
                // if null complete wihout result
                if(result == null){
                    fut.complete()
                }else {
                    fut.complete(result)
                }
            } catch (ex: Exception) {
                fut.fail(ex)
            }

            return fut
        }

        fun <T> react(action: () -> T): Reaction<T> {
            val fut = react<T>()
            try {
                //Check if action return null
                val result:T? = action()
                // if null complete wihout result
                if(result == null){
                    fut.complete()
                }else{
                    fut.complete(result)
                }
            } catch (ex: Exception) {
                fut.fail(ex)
            }
            return fut
        }

        /**
         * Create a react that hasn't completed yet and that is passed to the `handler` before it is returned.
         *
         * @param handler the handler
         * @param <T> the result type
         * @return the react.
        </T> */
        fun <T> react(handler: Handler<Reaction<T>>): Reaction<T> {
            val fut = react<T>()
            handler.handle(fut)
            return fut
        }

        /**
         * Create a react that hasn't completed yet
         *
         * @param <T>  the result type
         * @return  the react
        </T> */
        fun <T> react(): Reaction<T> {
            return factory.reaction()
        }

        /**
         * Create a succeeded react with a null result
         *
         * @param <T>  the result type
         * @return  the react
        </T> */
        fun succeededReact(): Reaction<Nothing?> {
            return factory.succeededReact()
        }

        /**
         * Created a succeeded react with the specified result.
         *
         * @param result  the result
         * @param <T>  the result type
         * @return  the react
        </T> */
        fun <T> succeededReact(result: T): Reaction<T> {
            return factory.succeededReact(result)
        }

        /**
         * Create a failed react with the specified failure cause.
         *
         * @param t  the failure cause as a Throwable
         * @param <T>  the result type
         * @return  the react
        </T> */
        fun <T> failedReact(t: Throwable): Reaction<T> {
            return factory.failedReact(t)
        }

        /**
         * Create a failed react with the specified failure message.
         *
         * @param failureMessage  the failure message
         * @param <T>  the result type
         * @return  the react
        </T> */
        fun <T> failedReact(failureMessage: String): Reaction<T> {
            return factory.failureReact(failureMessage)
        }

        val factory = ReactionFactory()
    }

}