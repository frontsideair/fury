/*
   ╔═══════════════════════════════════════════════════════════════════════════════════════════════════════════╗
   ║ Fury, version 0.5.0. Copyright 2018-19 Jon Pretty, Propensive Ltd.                                        ║
   ║                                                                                                           ║
   ║ The primary distribution site is: https://propensive.com/                                                 ║
   ║                                                                                                           ║
   ║ Licensed under  the Apache License,  Version 2.0 (the  "License"); you  may not use  this file  except in ║
   ║ compliance with the License. You may obtain a copy of the License at                                      ║
   ║                                                                                                           ║
   ║     http://www.apache.org/licenses/LICENSE-2.0                                                            ║
   ║                                                                                                           ║
   ║ Unless required  by applicable law  or agreed to in  writing, software  distributed under the  License is ║
   ║ distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. ║
   ║ See the License for the specific language governing permissions and limitations under the License.        ║
   ╚═══════════════════════════════════════════════════════════════════════════════════════════════════════════╝
*/

package fury

import fury.core.{Pool, RetryingPool}
import probably._

import scala.concurrent.ExecutionContext.Implicits.global

object RetryingPoolTest extends TestApp {
  
  private val ok: collection.mutable.Map[Symbol, Unit] = collection.concurrent.TrieMap()
  
  private val dummyPool: Pool[String, Symbol] = new RetryingPool[String, Symbol, DummyException](10L) {
    override def create(key: String): Symbol = Symbol(key)
    override def destroy(value: Symbol): Unit = ()
    override def isBad(value: Symbol): Boolean = !ok.contains(value)
  }

  override def tests(): Unit = {
    test("reuse existing entries") {
      dummyPool.borrow("a/b/c"){mayThrow}
      dummyPool.borrow("a/b/x"){mayThrow}
      dummyPool.borrow("a/b/c"){mayThrow}
      dummyPool.size
    }.assert(_ == 2)

    test("Return correct values") {
      var result1: Symbol = null
      var result2: Symbol = null
      var result3: Symbol = null
      dummyPool.borrow("a/b/c"){result1 = _}
      dummyPool.borrow("a/b/x"){result2 = _}
      dummyPool.borrow("a/b/c"){result3 = _}
      (result1, result2, result3)
    }.assert(_ == (Symbol("a/b/c"), Symbol("a/b/x"), Symbol("a/b/c")))
  }
  
  private def mayThrow: Symbol => Unit = sym => {
    if(ok.contains(sym)) () else {
      ok(sym) = ()
      throw DummyException(sym)
    }
  }
  
  private case class DummyException(target: Symbol) extends Exception

}