## Contracts in Scala

This is the demonstration used in the lecture. 
The lecture roughly follows the following script:

### The Interface (Contracts in Documentation)
- First we discuss the interface as described in file [MutableMap.scala](MutableMap.scala).
- Then we discuss alternative pre- and postconditions.
- Question: what do possible implementations look like?
- We identify that the equations specify the implementation quite a bit.
- We can note that `@precondition` and `@postcondition` are just comments that are not checked, at all.

### A First Implementation
- We start with the implementation in `ImmutableHashMap`. We recall how subtyping allows us to call `demo` with an instance of `ImmutableHashMap`.
- We do not statically or dynamically check any pre- or postconditions.

### Dynamically Checked Contracts
- We now walk through `ImmutableHashMapContract` and look at the different ways to check contracts at runtime.
  * `require`
  * `assert`
  * `ensuring` (we briefly explain extension methods and type polymorphism)
  * `unchanged` (we briefly explain by-name parameters and their desugaring, and remind of reference equality with `eq`)

### Invariants
As a last step, to illustrate invariants in code, we walk through three additional implementations:
1. `InefficientListMap` shows a second implementation. Method `contains` searches through the bindings, which is linear.
2. `MoreEfficientListMap` improves complexity of `contains` by maintaining a separate `Set`. For correctness, we need to establish an **invariant**.
3. `MoreEfficientListMapContract` shows how to establish this invariant in code.

