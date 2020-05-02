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

package io.skerna.commons.sreaction

import kotlin.js.JsName


interface Reaction<T> : ReactionResult<T>, Handler<ReactionResult<T>> {

    /**
     * Has the reaction completed?
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
    fun getHandler(): Handler<ReactionResult<T>>?

    /**
     * Set a handler of type exceptions
     * 
     * if the reaction has already  completed it will called immediately. else it will callend when the reaction
     * change state to completed
     * @param handler
     * @return Reaction<T>
     */
    fun setExceptionHandler(handler: Handler<Throwable>): Reaction<T>


    @JsName("setExceptionHandler")
    fun setExceptionHandler(exHandler: (asyncResult:Throwable)->Unit): Reaction<T>

    /**
     * Attach observer to completable result
     *
     * if the reaction has been completed, observer is notified immediately. otherwise it will called when sreaction
     * is completed, if handler is not defined the param is passed set as Main handler
     * @param handler
     */
    fun watchResult(handler: Handler<ReactionResult<T>>):Reaction<T>


    @JsName("watchResult")
    fun watchResult(handler: (reactionResult: ReactionResult<T>) -> Unit): Reaction<T> = apply{
        var handlerParsed = Handler.create(handler)
        watchResult(handlerParsed)
    }

    /**
     * Set a handler for the result.
     *
     *
     * If the reaction has already been completed it will be called immediately. Otherwise it will be called when the
     * reaction is completed.
     *
     * @param handler  the Handler that will be called with the result
     * @return a reference to this, so it can be used fluently
     */
    fun setHandler(handler: Handler<ReactionResult<T>>): Reaction<T>

    @JsName("setHandler")
    fun setHandler(handler: (reactionResult: ReactionResult<T>) -> Unit): Reaction<T> = apply {
        val handlerParsed = Handler.create(handler)
        setHandler(handlerParsed)
    }

    /**
     * Set the result. Any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @param result  the result
     */
    @JsName("complete")
    fun complete(result: T)

    /**
     * Set a null result. Any handler will be called, if there is one, and the reaction will be marked as completed.
     */
    @JsName("completeEmpy")
    fun complete()

    /**
     * Set the failure. Any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @param cause  the failure cause
     */
    @JsName("fail")
    fun fail(cause: Throwable?)

    /**
     * Try to set the failure. When it happens, any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @param failureMessage  the failure message
     */
    @JsName("failWithMessage")
    fun fail(failureMessage: String)

    /**
     * Set the failure. Any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @param result  the result
     * @return false when the reaction is already completed
     */
    @JsName("tryComplete")
    fun tryComplete(result: T?): Boolean

    /**
     * Try to set the result. When it happens, any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @return false when the reaction is already completed
     */
    @JsName("tryCompleteDefault")
    fun tryComplete(): Boolean

    /**
     * Try to set the failure. When it happens, any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @param cause  the failure cause
     * @return false when the reaction is already completed
     */
    @JsName("tryFail")
    fun tryFail(cause: Throwable?): Boolean

    /**
     * Try to set the failure. When it happens, any handler will be called, if there is one, and the reaction will be marked as completed.
     *
     * @param failureMessage  the failure message
     * @return false when the reaction is already completed
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
     * The result of the opreation . this never return null if the operation failed
     * @param default, default value to return in error case or null
     * @return T
     */
    override fun resultOrDefault(default:T): T

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
     * Compose this reaction with a provided `next` reaction.
     *
     *
     *
     * When this (the one on which `compose` is called) reaction succeeds, the `handler` will be called with
     * the completed value, this handler should complete the next reaction.
     *
     *
     *
     * If the `handler` throws an exception, the returned reaction will be failed with this exception.
     *
     *
     *
     * When this reaction fails, the failure will be propagated to the `next` reaction and the `handler`
     * will not be called.
     *
     * @param handler the handler
     * @param next the next reaction
     * @return the next reaction, used for chaining
     */
    fun <U> compose(handler: Handler<T>, next: Reaction<U>): Reaction<U> {
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    try {
                        handler.handle(reactionResult.result()!!)
                    } catch (err: Throwable) {
                        if (next.isCompleted()) {
                            throw err
                        }
                        next.fail(err)
                    }

                } else {
                    next.fail(reactionResult.cause())
                }
            }

        })
        return next
    }

    /**
     * Compose this reaction with a `mapper` function.
     *
     *
     *
     * When this reaction (the one on which `compose` is called) succeeds, the `mapper` will be called with
     * the completed value and this mapper returns another reaction object. This returned reaction completion will complete
     * the reaction returned by this method call.
     *
     *
     *
     * If the `mapper` throws an exception, the returned reaction will be failed with this exception.
     *
     *
     *
     * When this reaction fails, the failure will be propagated to the returned reaction and the `mapper`
     * will not be called.
     *
     * @param mapper the mapper function
     * @return the composed reaction
     */
    fun <U> compose(mapper: Function<T, Reaction<U>>?): Reaction<U> {
        if (mapper == null) {
            throw NullPointerException()
        }
        val ret = react<U>()
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    val apply: Reaction<U>
                    try {
                        apply = mapper(reactionResult.result()!!)
                    } catch (e: Throwable) {
                        ret.fail(e)
                        return
                    }

                    apply.setHandler(ret)
                } else {
                    ret.fail(reactionResult.cause())
                }
            }

        })
        return ret
    }

    /**
     * Apply a `mapper` function on this reaction.
     *
     *
     *
     * When this reaction succeeds, the `mapper` will be called with the completed value and this mapper
     * returns a value. This value will complete the reaction returned by this method call.
     *
     *
     *
     * If the `mapper` throws an exception, the returned reaction will be failed with this exception.
     *
     *
     *
     * When this reaction fails, the failure will be propagated to the returned reaction and the `mapper`
     * will not be called.
     *
     * @param mapper the mapper function
     * @return the mapped reaction
     */
    override fun <U> map(mapper: Function<T, U>): Reaction<U> {
        val ret = react<U>()
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    val mapped: U
                    try {
                        mapped = mapper(reactionResult.result()!!)
                    } catch (e: Throwable) {
                        ret.fail(e)
                        return
                    }

                    ret.complete(mapped)
                } else {
                    ret.fail(reactionResult.cause())
                }
            }

        })
        return ret
    }

    /**
     * Map the result of a reaction to a specific `value`.
     *
     *
     *
     * When this reaction succeeds, this `value` will complete the reaction returned by this method call.
     *
     *
     *
     * When this reaction fails, the failure will be propagated to the returned reaction.
     *
     * @param value the value that eventually completes the mapped reaction
     * @return the mapped reaction
     */
    override fun <V> map(value: V): Reaction<V> {
        val ret = react<V>()
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    ret.complete(value)
                } else {
                    ret.fail(reactionResult.cause())
                }
            }

        })
        return ret
    }

    /**
     * Map the result of a reaction to `null`.
     *
     *
     *
     * This is a conveniency for `reaction.map((T) null)` or `reaction.map((Void) null)`.
     *
     *
     *
     * When this reaction succeeds, `null` will complete the reaction returned by this method call.
     *
     *
     *
     * When this reaction fails, the failure will be propagated to the returned reaction.
     *
     * @return the mapped reaction
     */
    //  fun <V> mapEmpty(): Reaction<V> {
    //     return AsyncResult.mapEmpty()
    // }

    /**
     * Succeed or fail this reaction with the [ReactionResult] asyncResult.
     *
     * @param reactionResult the async result to getHandler
     */
    override fun handle(reactionResult: ReactionResult<T>)

    /**
     * @return an handler completing this reaction
     */
    fun completer(): Handler<ReactionResult<T>> {
        return this
    }

    /**
     * Handles a failure of this Reaction by returning the result of another Reaction.
     * If the mapper fails, then the returned reaction will be failed with this failure.
     *
     * @param mapper A function which takes the exception of a failure and returns a new reaction.
     * @return A recovered reaction
     */
    fun recover(mapper: Function<Throwable, Reaction<T>>?): Reaction<T> {
        if (mapper == null) {
            throw NullPointerException()
        }
        val ret = react<T>()
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    ret.complete(result()!!)
                } else {
                    val mapped: Reaction<T>
                    try {
                        mapped = mapper(reactionResult.cause())
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
     * Apply a `mapper` function on this reaction.
     *
     *
     *
     * When this reaction fails, the `mapper` will be called with the completed value and this mapper
     * returns a value. This value will complete the reaction returned by this method call.
     *
     *
     *
     * If the `mapper` throws an exception, the returned reaction will be failed with this exception.
     *
     *
     *
     * When this reaction succeeds, the result will be propagated to the returned reaction and the `mapper`
     * will not be called.
     *
     * @param mapper the mapper function
     * @return the mapped reaction
     */
    fun otherwise(mapper: Function<Throwable, T>?): Reaction<T> {
        if (mapper == null) {
            throw NullPointerException()
        }
        val ret = react<T>()
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    ret.complete(result()!!)
                } else {
                    val value: T
                    try {
                        value = mapper(reactionResult.cause())
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
     * Map the failure of a reaction to a specific `value`.
     *
     *
     *
     * When this reaction fails, this `value` will complete the reaction returned by this method call.
     *
     *
     *
     * When this reaction succeeds, the result will be propagated to the returned reaction.
     *
     * @param value the value that eventually completes the mapped reaction
     * @return the mapped reaction
     */
    override fun otherwise(value: T): Reaction<T> {
        val ret = react<T>()
        setHandler(object : Handler<ReactionResult<T>> {
            override fun handle(reactionResult: ReactionResult<T>) {
                if (reactionResult.succeeded()) {
                    ret.complete(result()!!)
                } else {
                    ret.complete(value)
                }
            }
        })
        return ret
    }

    /**
     * Map the failure of a reaction to `null`.
     *
     *
     *
     * This is a convenience for `reaction.otherwise((T) null)`.
     *
     *
     *
     * When this reaction fails, the `null` value will complete the reaction returned by this method call.
     *
     *
     *
     * When this reaction succeeds, the result will be propagated to the returned reaction.
     *
     * @return the mapped reaction
     */
    //   override fun otherwiseEmpty(): Reaction<T> {
//        return super@AsyncResult.otherwiseEmpty()
    //  }

    companion object {

        suspend fun <T> reactSuspend(action: suspend () -> T): Reaction<T> {
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
         * Create a reaction that hasn't completed yet and that is passed to the `handler` before it is returned.
         *
         * @param handler the handler
         * @param <T> the result type
         * @return the reaction.
        </T> */
        fun <T> react(handler: Handler<Reaction<T>>): Reaction<T> {
            val fut = react<T>()
            handler.handle(fut)
            return fut
        }

        /**
         * Create a reaction that hasn't completed yet
         *
         * @param <T>  the result type
         * @return  the reaction
        </T> */
        fun <T> react(): Reaction<T> {
            return factory.reaction()
        }

        /**
         * Create a succeeded reaction with a null result
         *
         * @param <T>  the result type
         * @return  the reaction
        </T> */
        fun succeededReact(): Reaction<Nothing?> {
            return factory.succeededReact()
        }

        /**
         * Created a succeeded reaction with the specified result.
         *
         * @param result  the result
         * @param <T>  the result type
         * @return  the reaction
        </T> */
        fun <T> succeededReact(result: T): Reaction<T> {
            return factory.succeededReact(result)
        }

        /**
         * Create a failed reaction with the specified failure cause.
         *
         * @param t  the failure cause as a Throwable
         * @param <T>  the result type
         * @return  the reaction
        </T> */
        fun <T> failedReact(t: Throwable): Reaction<T> {
            return factory.failedReact(t)
        }

        /**
         * Create a failed reaction with the specified failure message.
         *
         * @param failureMessage  the failure message
         * @param <T>  the result type
         * @return  the reaction
        </T> */
        fun <T> failedReact(failureMessage: String): Reaction<T> {
            return factory.failureReact(failureMessage)
        }

        val factory = ReactionFactory()
    }

}