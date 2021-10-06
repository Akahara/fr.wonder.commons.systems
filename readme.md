# fr.wonder.commons.systems

This project is a library providing more complex functionnalities than [fr.wonder.commons](https://github.com/Akahara/fr.wonder.commons) but used less often. It contains both ready-to-use utilities (`ProcessArguments`, every `*Utils` class) and code to generate random input data (`DebugValues` and `FooBar`).

### Debug package

The `DebugValues` class contains generators for random input data, it does not support random class instances yet. The `InstancePrinter` class can be used to get an idea of the data structure of a complex project, it can print the full field tree of an object instance (printing the values of every field recursively), it is supposed to be used in developpement only! The javadoc contains more information about its capabilities.

### Process package

The `ManifestUtils` and `Manifest` classes can be used to parse manifest files easily.\
The `ProcessArguments` class can be used to parse command line arguments and create entry points to a command line program, it can be very powerful and simple to set-up, I'll make a more in depth explanation in this readme later.\
The `ProcessUtils` class contains signals error codes, a function to redirect the output of a program to the standard output or a logger and some more small functions.\
The `SystemUtils` contains functions to retrieve the environment OS and query the user home directory.

### Reflection package

Both `PrimitiveUtils` and `ReflectUtils` classes allow for easier manipulations of primitive types and reflections methods, they both do quite a lot so see the javadoc for the complete description of their capabilities.\
The `FooBar` class contains some dummy classes and instances that can be very handy to test reflection code.

### Registry package

This package may be removed soon so I won't talk about it too much here.

## More & about

This library is currently built using java 9+, it should work with java 8 for the most part (maybe not reflection), you can safely use it as long as you include the project's licence in yours.
You can find more utilities in my common libraries ([math](https://github.com/Akahara/fr.wonder.commons.math) and [commons](https://github.com/Akahara/fr.wonder.commons)).
