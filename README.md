[![Build Status](https://travis-ci.com/mP1/walkingkooka-resource-annotation-processor.svg?branch=master)](https://travis-ci.com/mP1/walkingkooka-resource-annotation-processor.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/mP1/walkingkooka-resource-annotation-processor/badge.svg?branch=master)](https://coveralls.io/github/mP1/walkingkooka-resource-annotation-processor?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/mP1/walkingkooka-resource-annotation-processor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/walkingkooka-resource-annotation-processor/context:java)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/mP1/walkingkooka-resource-annotation-processor.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/walkingkooka-resource-annotation-processor/alerts/)
[![J2CL compatible](https://img.shields.io/badge/J2CL-compatible-brightgreen.svg)](https://github.com/mP1/j2cl-central)



# walkingkooka-resource-annotation-processor

An annotation processor that generates a provider for any class marked with `walkingkooka.resource.TextResourceAware`. This is particularly useful for any runtime environment such as
J2CL where `java.lang.Class#getResourceAsStream` is not supported but using files as text is still required.

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


## Getting the source

You can either download the source using the "ZIP" button at the top
of the github page, or you can make a clone using git:

```
git clone git://github.com/mP1/walkingkooka-resource-annotation-processor.git
```


