class EntryNotFoundException(key: String) extends Exception {
  override def getMessage: String = s"No value can be found for key \"${key}\"."
}

/**
 * A mutable store mapping string keys to values of type [[V]].
 *
 * @tparam V the type of values to be stored in the mutable map.
 *
 * @equations implementations need to fulfill the following equations:
 *
 * == Storing and Retrieving ==
 *
 * Looking up a previously stored value with the same key should result in
 * the original value (assume `m: MutableMap`, `k: String`, `v: V`):
 * {{{
 *   m.put(k, v); m.get(k)   ==   v
 * }}}
 *
 * Storing a different value for the same key overrides the old one (assume `v2: V`):
 * {{{
 *   m.put(k, v); m.put(k, v2); m.get(k)   ==   v2
 * }}}
 *
 * Looking up the same key without intermediate stores will result in the same value:
 * {{{
 *   m.put(k, v)
 *   val first = m.get(k)
 *   val second = m.get(k)
 *   first   ==   second   ==   v
 * }}}
 *
 * Looking up a key that has not been stored before results in an [[EntryNotFoundException]].
 * {{{
 *   m.get(k)   ==   throw EntryNotFoundException(k)
 * }}}
 *
 *
 * == Checking Presence ==
 * Asking whether a key that we never previously stored is defined, results in false:
 * {{{
 *   m.contains(k)   ==   false
 * }}}
 *
 * Asking whether a stored key is defined after storing it, results in true:
 * {{{
 *   m.put(k, v); m.contains(k)   ==   true
 * }}}
 *
 * Looking up a key that is not defined results in an [[EntryNotFoundException]].
 * {{{
 *   if !m.contains(k) then m.get(k)   ==   throw EntryNotFoundException(k)
 * }}}
 *
 * Looking up a key that is defined will return some value.
 * {{{
 *   if m.contains(k) then m.get(k)   ==    v   (for some value v)
 * }}}
 */
trait MutableMap[V] {

  /**
   * Stores the [[value]] of type [[V]] under the [[key]], identified by a string.
   * If a value already existed, then it is replaced by the new one.
   *
   * @precondition the [[key]] string should not be empty.
   *
   * @postcondition the map will contain [[value]] as an entry for [[key]].
   * @postcondition all entries for other keys are left unchanged.
   *
   * @param key non-empty string to perform the lookup with.
   * @param value the value to be stored in the map.
   */
  def put(key: String, value: V): Unit

  /**
   * Retrieves the value for the key.
   *
   * @precondition the [[key]] string should not be empty.
   * @precondition a value for [[key]] needs to be stored previously by using [[put]].
   *
   * @postcondition the returned value for [[key]] is the one, previously stored.
   * @postcondition this operation does not alter any entries.
   *
   * @param key non-empty string to perform the lookup with.
   * @return a value, if it has been stored previously.
   *
   * @throws EntryNotFoundException indicates no value has been stored for a certain key.
   *
   */
  def get(key: String): V

  /**
   * Checks whether a value has been stored previously for this key.
   *
   * @precondition the [[key]] string should not be empty.
   *
   * @postcondition this operation does not alter any entries.
   *
   * @param key non-empty string to perform the lookup with.
   * @return a boolean indicating whether a value is available for this key.
   */
  def contains(key: String): Boolean
}
