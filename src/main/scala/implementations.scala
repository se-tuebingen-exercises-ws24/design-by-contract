import scala.annotation.tailrec
import scala.collection.{immutable, mutable}

def demo(m: MutableMap[Int]) = {
  // m.put("", 0) // error
  m.put("a", 0)
  assert(m.get("a") == 0)


  // m.get("b") // error
  m.put("a", 1)
  assert(m.get("a") == 1)

  //  shouldThrowIllegalArgumentException {
  //    m.put("", 42)
  //  }

  assert(m.contains("a") == true)
  assert(m.contains("b") == false)
}

// by name parameter
def shouldThrowIllegalArgumentException(prog: => Unit): Unit =
  try {
    prog
    assert(false, "Should have thrown")
  } catch {
    case exc: IllegalArgumentException => ()
    case e: AssertionError => throw e
    case e => assert(false, "Wrong exception")
  }

@main
def mainContracts() = demo(MoreEfficientListMapContract())













































/**
 * Implementation of [[MutableMap]] backed by a [[immutable.HashMap]].
 */
class ImmutableHashMap[V] extends MutableMap[V] {
  private var entries: immutable.HashMap[String, V] = immutable.HashMap.empty

  override def put(key: String, value: V): Unit =
    entries = entries.updated(key, value)

  override def get(key: String): V = entries.getOrElse(key, throw EntryNotFoundException(key))

  override def contains(key: String): Boolean = entries.isDefinedAt(key)
}












































// We can understand { ... } ensuring { x => P(x) } as
//   myEnsuring({ () => ... })(x => P(x))
// with the following implementation.
def myEnsuring[R](prog: () => R)(pred: R => Boolean): R = {
  val result = prog()
  assert(pred(result))
  result
}

/**
 * Implementation of [[MutableMap]] backed by a [[immutable.HashMap]].
 *
 * Illustrates the usage of contract mechanisms [[require]], [[assert]], and [[ensuring]].
 */
class ImmutableHashMapContract[V] extends MutableMap[V] {
  private var entries: immutable.HashMap[String, V] = immutable.HashMap.empty

  override def put(key: String, value: V): Unit = myEnsuring { () =>
    require(key != "", "Key must not be empty.") // it would be OK to omit this. Why? Discuss.

    // Here we discussed that after checking the precondition, we can rely
    // on the fact that key != ""
    // The type checker in Scala does not know this, though. Other languages
    // like TypeScript have "flow sensitive" types that reason about this.
    if (key == "") { sys.error("unreachable") } else { () }

    entries = entries.updated(key, value)
  } { x => contains(key) }

  override def get(key: String): V = unchanged {
    require(key != "", "Key must not be empty.")
    entries.getOrElse(key, throw EntryNotFoundException(key))
  }

  override def contains(key: String): Boolean = unchanged {
    require(key != "", "Key must not be empty.")
    entries.isDefinedAt(key)
  }

  private def unchanged[A](block: => A): A =
    val entriesBefore = entries
    val result = block
    assert(entriesBefore eq entries, "This method should not modify the map.")
    result
}













































/**
 * Implementation of [[MutableMap]] backed by a list of entries.
 *
 * Intermediate example that just leads to [[MoreEfficientListMap]].
 */
class InefficientListMap[V] extends MutableMap[V] {

  enum Store {
    case Empty
    case Entry(key: String, value: V, rest: Store)
  }

  private var entries: Store = Store.Empty

  override def put(key: String, value: V): Unit =
    entries = Store.Entry(key, value, entries)

  override def get(key: String): V = get(key, entries)

  @tailrec
  private def get(key: String, store: Store): V =
    store match {
      case Store.Empty => throw new EntryNotFoundException(key)
      case Store.Entry(key2, value, rest) if key2 == key => value
      case Store.Entry(key2, value, rest) => get(key, rest)
    }

  override def contains(key: String): Boolean = contains(key, entries)

  @tailrec
  private def contains(key: String, store: Store): Boolean =
    store match {
      case Store.Empty => false
      case Store.Entry(key2, value, rest) if key2 == key => true
      case Store.Entry(key2, value, rest) => contains(key, rest)
    }
}












































/**
 * Implementation of [[MutableMap]] backed by a list of entries and a separate set of keys.
 *
 * Motivates reasoning about implementation invariants.
 */
class MoreEfficientListMap[V] extends MutableMap[V] {

  enum Store {
    case Empty
    case Entry(key: String, value: V, rest: Store)
  }

  // invariant:
  // exists v. get(m, x) = v   <==>   x in keys(m)
  private var entries: Store = Store.Empty
  private var keys: Set[String] = Set.empty

  override def put(key: String, value: V): Unit =
    entries = Store.Entry(key, value, entries)
    // invariant is briefly violated
    // vvvvvvvvvvvvvv
    println("It would be bad if this state could be observed")
    // ^^^^^^^^^^^^^^
    // The next statement restores the invariant
    keys = keys + key
    // we leave the function in a state where the invariant holds again...
    // Please note: if we have parallelism (like when using preemptive multithreading)
    // another process could call `get` in the very moment, where the invariant is violated.
    // our implementation of `MutableMap` here is thus not "thread safe".
    //
    // Also: you need to be careful not to call other methods that rely on the
    // invariant, before restoring it. For example:
    //
    //   entries = Store.Entry(key, value, entries) // destroy invariant
    //   val entryAtKey = this.get(key)             // `get` relies on invariant
    //   keys = keys + key                          // restore invariant

  override def get(key: String): V =
    if (!keys.contains(key)) throw new EntryNotFoundException(key)
    else get(key, entries)

  @tailrec
  private def get(key: String, store: Store): V =
    store match {
      case Store.Empty => throw new EntryNotFoundException(key)
      case Store.Entry(key2, value, rest) if key2 == key => value
      case Store.Entry(key2, value, rest) => get(key, rest)
    }

  override def contains(key: String): Boolean = keys.contains(key)
}












































/**
 * Contract version of [[MoreEfficientListMap]].
 *
 * Establishes the invariants in code.
 */
class MoreEfficientListMapContract[V] extends MutableMap[V] {

  enum Store {
    case Empty
    case Entry(key: String, value: V, rest: Store)
  }

  private var entries: Store = Store.Empty
  private var keys: Set[String] = Set.empty

  override def put(key: String, value: V): Unit =
    require(key != "", "Key must not be empty.")
    entries = Store.Entry(key, value, entries)
    keys = keys + key
    // check the invariant after modifying the store
    invariant()

  override def get(key: String): V = unchanged {
    require(key != "", "Key must not be empty.")
    if (!keys.contains(key)) throw new EntryNotFoundException(key)
    else get(key, entries)
  }

  override def contains(key: String): Boolean = unchanged { keys.contains(key) }

  @tailrec
  private def get(key: String, store: Store): V =
    require(allKeys(store).contains(key)) // could be changed to assert: discuss.
    store match {
      case Store.Empty => throw AssertionError("Implementation error: contradiction with precondition.")
      case Store.Entry(key2, value, rest) if key2 == key => value
      case Store.Entry(key2, value, rest) => get(key, rest)
    }

  @tailrec
  private def allKeys(store: Store, keys: Set[String]): Set[String] = store match {
    case Store.Empty => keys
    case Store.Entry(key, value, rest) => allKeys(rest, keys + key)
  }

  private def invariant(): Unit = assert(allKeys(entries, Set.empty) == keys)

  private def unchanged[A](block: => A): A =
    val entriesBefore = entries
    val result = block
    assert(entriesBefore eq entries, "This method should not modify the map.")
    result

  // we check the invariant once when constructing the object:
  invariant()
}
