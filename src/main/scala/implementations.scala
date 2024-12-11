import scala.annotation.tailrec
import scala.collection.{immutable, mutable}

def demo(m: MutableMap[Int]) = {
  // m.put("", 0) // error
  m.put("a", 0)
  assert(m.get("a") == 0)


  // m.get("b") // error
  m.put("a", 1)
  assert(m.get("a") == 1)

  assert(m.contains("a") == true)
  assert(m.contains("b") == false)
}

@main
def mainContracts() = demo(ImmutableHashMap())













































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














































/**
 * Implementation of [[MutableMap]] backed by a [[immutable.HashMap]].
 *
 * Illustrates the usage of contract mechanisms [[require]], [[assert]], and [[ensuring]].
 */
class ImmutableHashMapContract[V] extends MutableMap[V] {
  private var entries: immutable.HashMap[String, V] = immutable.HashMap.empty

  override def put(key: String, value: V): Unit = {
    require(key != "", "Key must not be empty.") // it would be OK to omit this. Why? Discuss.
    entries = entries.updated(key, value)
  } ensuring { result => contains(key) }

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

  private var entries: Store = Store.Empty
  private var keys: Set[String] = Set.empty

  override def put(key: String, value: V): Unit =
    entries = Store.Entry(key, value, entries)
    keys = keys + key

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
  private def allKeys(store: Store, keys: Set[String] = Set.empty): Set[String] = store match {
    case Store.Empty => keys
    case Store.Entry(key, value, rest) => allKeys(rest, keys + key)
  }

  private def invariant(): Unit = assert(allKeys(entries) == keys)

  private def unchanged[A](block: => A): A =
    val entriesBefore = entries
    val result = block
    assert(entriesBefore eq entries, "This method should not modify the map.")
    result

  // we check the invariant once when constructing the object:
  invariant()
}
