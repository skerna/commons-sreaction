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

@file:Suppress("USELESS_ELVIS")

package io.skerna.futures

import kotlin.js.JsName

public interface AsyncResult<T> {

  /**
   * The result of the operation. This will be null if the operation failed.
   *
   * @return the result or null if the operation failed.
   */
  @JsName("result")
  fun result():T?;


  @JsName("resultIsNull")
  fun resultIsNull():Boolean{
    return result() == null
  }

  /**
   * A Throwable describing failure. This will be null if the operation succeeded.
   *
   * @return the cause or null if the operation succeeded.
   */
  @JsName("cause")
  fun cause():Throwable

  /**
   * Did it succeed?
   *
   * @return true if it succeded or false otherwise
   */
  @JsName("succeeded")
  fun succeeded():Boolean;

  /**
   * Did it fail?
   *
   * @return true if it failed or false otherwise
   */
  @JsName("failed")
  fun failed():Boolean;

  /**
   * Apply a {@code mapper} function on this async result.<p>
   *
   * The {@code mapper} is called with the completed value and this mapper returns a value. This value will complete the result returned by this method call.<p>
   *
   * When this async result is failed, the failure will be propagated to the returned async result and the {@code mapper} will not be called.
   *
   * @param mapper the mapper function
   * @return the mapped async result
   */
  @JsName("map")
  fun<U>  map(mapper:Function<T,U>):AsyncResult<U> {
    return  object:AsyncResult<U> {
      override fun  result():U?{
        if (succeeded()) {
          return mapper(this@AsyncResult.result()!!);
        } else {
          return null;
        }
      }

      override fun cause():Throwable {
        return this@AsyncResult.cause()?:NoStackTraceThrowable("Unknow error cause, react not report the cause of the failure")
      }

      override fun succeeded():Boolean {
        return this@AsyncResult.succeeded();
      }

      override fun failed():Boolean {
        return this@AsyncResult.failed();
      }
    };
  }

  /**
   * Map the result of this async result to a specific {@code value}.<p>
   *
   * When this async result succeeds, this {@code value} will succeeed the async result returned by this method call.<p>
   *
   * When this async result fails, the failure will be propagated to the returned async result.
   *
   * @param value the value that eventually completes the mapped async result
   * @return the mapped async result
   */
  fun<V>  map(value:V):AsyncResult<V> {
    return map { value };
  }

  /**
   * Map the result of this async result to {@code null}.<p>
   *
   * This is a convenience for {@code asyncResult.map((T) null)} or {@code asyncResult.map((Void) null)}.<p>
   *
   * When this async result succeeds, {@code null} will succeeed the async result returned by this method call.<p>
   *
   * When this async result fails, the failure will be propagated to the returned async result.
   *
   * @return the mapped async result
   */
 // fun<V>  mapEmpty():AsyncResult<V> {
 //   return map((V)null);
 // }

  /**
   * Apply a {@code mapper} function on this async result.<p>
   *
   * The {@code mapper} is called with the failure and this mapper returns a value. This value will complete the result returned by this method call.<p>
   *
   * When this async result is succeeded, the value will be propagated to the returned async result and the {@code mapper} will not be called.
   *
   * @param mapper the mapper function
   * @return the mapped async result
   */
  fun  otherwise(mapper:(err:Throwable?)->T?):AsyncResult<T> {
    return object:AsyncResult<T> {

      override fun result():T? {
        if (this@AsyncResult.succeeded()) {
          return this@AsyncResult.result();
        } else if (this@AsyncResult.failed()) {
          return mapper(this@AsyncResult.cause());
        } else {
          return null;
        }
      }

      override fun  cause():Throwable {
        return NoStackTraceThrowable("Unknow error cause, react not report the cause of the failure");
      }

      override fun succeeded():Boolean {
        return this@AsyncResult.succeeded() || this@AsyncResult.failed();
      }

      override fun failed():Boolean {
        return false;
      }
    };
  }

  /**
   * Map the failure of this async result to a specific {@code value}.<p>
   *
   * When this async result fails, this {@code value} will succeeed the async result returned by this method call.<p>
   *
   * When this async succeeds, the result will be propagated to the returned async result.
   *
   * @param value the value that eventually completes the mapped async result
   * @return the mapped async result
   */
  fun otherwise(value:T):AsyncResult<T> {
    return otherwise { value }
  }

  /**
   * Map the failure of this async result to {@code null}.<p>
   *
   * This is a convenience for {@code asyncResult.otherwise((T) null)}.<p>
   *
   * When this async result fails, the {@code null} will succeeed the async result returned by this method call.<p>
   *
   * When this async succeeds, the result will be propagated to the returned async result.
   *
   * @return the mapped async result
   */
  fun otherwiseEmpty():AsyncResult<T>  {
    return otherwise{ null};
  }
}