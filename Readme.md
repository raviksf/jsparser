## Antlr4 Syntax Tree use example

This is a sample that uses the Antlr4 parser to
create a syntax tree for javascript function, and
shows how to traverse the syntax tree to insert additional
code or checking statements to create another syntactically
corret javascript function.

A sample javascript example is included in the test directory.

To build simply type:

```
mvn package
```

To run - build the application, make sure the Antl4 jar e.g.
antlr4-runtime-4.7.jar or your current anltr version is
in the class path - and pass as an argument to the built Jar file
a path to a file containing the javascript function.

```
 java -jar simple-jsparser-0.0.1-SNAPSHOT.jar ../src/test/sample.js 
```


### License and Disclaimer:


```
 Copyright (C) 2017 Ravinder Krishnaswamy

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice 
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH 
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, 
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS 
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER 
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF 
THIS SOFTWARE.
```
