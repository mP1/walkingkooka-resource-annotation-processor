[![Build Status](https://github.com/mP1/walkingkooka-resource-annotation-processor/actions/workflows/build.yaml/badge.svg)](https://github.com/mP1/walkingkooka-resource-annotation-processor/actions/workflows/build.yaml/badge.svg)
[![Coverage Status](https://coveralls.io/repos/github/mP1/walkingkooka-resource-annotation-processor/badge.svg)](https://coveralls.io/github/mP1/walkingkooka-resource-annotation-processor)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/mP1/walkingkooka-resource-annotation-processor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/walkingkooka-resource-annotation-processor/context:java)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/mP1/walkingkooka-resource-annotation-processor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/walkingkooka-resource-annotation-processor/alerts/)
![](https://tokei.rs/b1/github/mP1/walkingkooka-resource-annotation-processor)
[![J2CL compatible](https://img.shields.io/badge/J2CL-compatible-brightgreen.svg)](https://github.com/mP1/j2cl-central)



# walkingkooka-resource-annotation-processor

An annotation processor that generates a provider for any class marked with `walkingkooka.resource.TextResourceAware`. 
This is particularly useful for any runtime environment such as J2CL where `java.lang.Class#getResourceAsStream` is not 
supported but using files as text is still required.

Note it is not currently possible to specify the charset encoding for any text file. Theres is a 
[ticket](https://github.com/mP1/walkingkooka-resource-annotation-processor/issues/21) to add support to specify a Charset
and this will require changing using `FileObject#openInputStream` rather than `FileObject#getCharContent`.



```java
package sample;

@walkingkooka.resource.TextResourceAware(normalizeSpace = true, fileExtension = ".txt2")
public class TextFile {
  // members not copied to generated class
}
```

generates...

```java
package sample;

public final class TextFileProvider implements walkingkooka.resource.TextResource {
    
    @Override
    public String text() {
       return "content   of   sample/TextFile.txt2";
    }
}
```

usage

```java
new TextFileProvider().text(); // returns "content   of   sample/TextFile.txt2"
```




