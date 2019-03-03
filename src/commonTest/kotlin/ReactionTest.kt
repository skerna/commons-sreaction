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

import kotlin.test.Test
import kotlin.test.assertEquals

class ReactionTest {


    @Test
    fun testResultOrDefault()  {
        println("Test reaction action with excetion")
        val result = Reaction.react { 2 * 100 / getDepMathValue() }
                .setExceptionHandler(GlobalExceptionHandler)
                .resultOrDefault(0)
        assertEquals(result, 0, "expected 0 result")
    }



    @Test
    fun testHandleExceptions()  {
        println("Test reaction action with excetion")
        val result = Reaction.react { executeUnsafeCode() }
                .setExceptionHandler(GlobalExceptionHandler)
                .result() ?: 0
        assertEquals(result, 0, "expected 0 result")
    }


    @Test
    fun testFailedReaction()  {
        println("Test reaction action with excetion")
        Reaction.react { 2 * 100 / getDepMathValue() }
                .setHandler {
                    println(it.cause())
                    assertEquals(true, it.failed(), "expected 0 result")
                }
    }

    @Test
    fun testSuccessReaction()  {
        println("Test success reaction")
        val r = Reaction.succeededReact(10)

        r.setHandler {
            assertEquals(10,it.result(),"expected 10 value")
        }
    }

    @Test
    fun test() = runBlockingAction {
        println("Test Reaction with null returns")
        val reaction = runTestingFuture()
        assertEquals(true,reaction.resultIsNull(),"expected null result")

    }

    suspend fun runTestingFuture() = Reaction.reactSuspend(suspend {
        null
    })


    fun executeUnsafeCode(): Int {
        throw IllegalStateException("Test exception")
    }

    /**
     * Return dep math value
     */
    private fun getDepMathValue(): Int {
        return 0
    }

}
