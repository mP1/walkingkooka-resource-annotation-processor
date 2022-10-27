/*
 * Copyright 2019 Miroslav Pokorny (github.com/mP1)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package walkingkooka.resource.annotationprocessor;

import walkingkooka.collect.set.Sets;
import walkingkooka.reflect.ClassName;
import walkingkooka.reflect.JavaVisibility;
import walkingkooka.resource.TextResource;
import walkingkooka.resource.TextResourceAware;
import walkingkooka.resource.TextResourceException;
import walkingkooka.resource.TextResources;
import walkingkooka.text.CharSequences;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * An {@link AbstractProcessor} that creates a class that creates a class for the class including the annotation
 * {@link walkingkooka.resource.TextResourceAware}
 */
public final class TextResourceAwareProviderAnnotationProcessor extends AbstractProcessor {

    private final static String FILE_EXTENSION = ".txt";

    public TextResourceAwareProviderAnnotationProcessor() {
        super();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.of(TextResourceAware.class.getName());
    }

    @Override
    public synchronized void init(final ProcessingEnvironment environment) {
        super.init(environment);

        this.elements = environment.getElementUtils();
        this.filer = environment.getFiler();
        this.messager = environment.getMessager();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment environment) {
        try {
            for (TypeElement annotation : annotations) {
                for (final Element annotated : environment.getElementsAnnotatedWith(annotation)) {
                    if (annotated instanceof TypeElement) {
                        final TypeElement typeElement = (TypeElement) annotated;

                        final ClassName enclosing = ClassName.with(
                                typeElement.getQualifiedName()
                                        .toString()
                        );

                        final TextResourceAware textResourceAware = typeElement.getAnnotation(TextResourceAware.class);

                        this.generateJreProvider(
                                enclosing,
                                visibility(typeElement),
                                textResourceAware
                        );
                        this.generateJ2clProvider(
                                enclosing,
                                textResourceAware
                        );
                    }
                }
            }
        } catch (final Exception cause) {
            this.error(cause.getMessage());
        }

        return false; // whether or not the set of annotation types are claimed by this processor
    }

    /**
     * Identifies the {@link JavaVisibility} of the host class and returns the replacement keyword.
     */
    private static JavaVisibility visibility(final TypeElement type) {
        final JavaVisibility visibility;

        for (; ; ) {
            final Set<Modifier> modifier = type.getModifiers();
            if (modifier.contains(Modifier.PUBLIC)) {
                visibility = JavaVisibility.PUBLIC;
                break;
            }
            if (modifier.contains(Modifier.PROTECTED)) {
                visibility = JavaVisibility.PROTECTED;
                break;
            }
            if (modifier.contains(Modifier.PRIVATE)) {
                visibility = JavaVisibility.PRIVATE;
                break;
            }
            visibility = JavaVisibility.PACKAGE_PRIVATE;
            break;
        }

        return visibility;
    }

    /**
     * Generates a new class in the same package as the {@link TextResource} with "Provider" appended to the class name.
     * This new class will load the text using {@link TextResources#classPath(String, Class)}. This allows the resource
     * to be updated continuously and the new contents loaded, unlike the j2cl TextResource which has the resource text
     * baked into the class source it generates.
     */
    private void generateJreProvider(final ClassName enclosing,
                                     final JavaVisibility visibility,
                                     final TextResourceAware textResourceAware) throws Exception {
        final ClassName providerClassName = ClassName.with(enclosing + PROVIDER);
        final String providerClassNameString = providerClassName.toString();
        final TypeElement exists = this.elements.getTypeElement(providerClassNameString);

        // assume null means generated source does not exist...
        if (null == exists) {
            // without this check the generated class will be written multiple times resulting in an exception when attempting to create the file.
            final String providerTypeSimpleName = providerClassName.nameWithoutPackage();
            final Filer filer = this.filer;

            // write a class which will load the textResource using a ClassPathTextResource.
            try (final Writer writer = filer.createSourceFile(providerClassNameString).openWriter()) {
                final String providerTemplate = this.providerTemplate("jre");

                writer.write(
                        providerTemplate.replace("$PACKAGE", providerClassName.parentPackage().value())
                                .replace("$VISIBILITY", visibility.javaKeyword())
                                .replace("$NAME", providerTypeSimpleName)
                                .replace("$RESOURCE", enclosing.nameWithoutPackage() + "." + fileExtension(textResourceAware))
                );
                writer.flush();
            }
        }
    }

    private void generateJ2clProvider(final ClassName enclosing,
                                      final TextResourceAware textResourceAware) throws Exception {
        final ClassName providerClassName = ClassName.with(enclosing + PROVIDER + "J2cl");
        final String providerClassNameString = providerClassName.toString();
        final TypeElement exists = this.elements.getTypeElement(providerClassNameString);

        // assume null means generated source does not exist...
        if (null == exists) {
            final String enclosingSimpleName = enclosing.nameWithoutPackage();
            // without this check the generated class will be written multiple times resulting in an exception when attempting to create the file.
            final Filer filer = this.filer;

            // write a class which will load the textResource as a String literal.
            try (final Writer writer = filer.createSourceFile(providerClassNameString).openWriter()) {
                final String providerTemplate = this.providerTemplate("j2cl");

                final String packageName = providerClassName.parentPackage()
                        .value();

                String text = this.readResource(
                                packageName,
                                enclosingSimpleName,
                                '.' +
                                        fileExtension(textResourceAware)
                        ).getCharContent(false)
                        .toString();

                if (textResourceAware.normalizeSpace()) {
                    text = text.replaceAll("\\s+", " ");
                }

                writer.write(
                        providerTemplate.replace("$PACKAGE", packageName)
                                .replace("$NAME", providerClassName.nameWithoutPackage())
                                .replace("$TEXT", CharSequences.quoteAndEscape(text))
                );
                writer.flush();
            }
        }
    }

    private final static String PROVIDER = "Provider";

    private static String fileExtension(final TextResourceAware textResourceAware) {
        final String fileExtension = textResourceAware.fileExtension();
        if (CharSequences.isNullOrEmpty(fileExtension)) {
            throw new IllegalArgumentException("File extension must not be null or empty");
        }
        return fileExtension;
    }

    /**
     * Loads the template will be used to generate the java source for the class being generated.
     */
    private String providerTemplate(final String suffix) throws TextResourceException {
        return TextResources.classPath(
                this.getClass()
                        .getSimpleName() +
                        "-" +
                        suffix +
                        ".txt",
                this.getClass()
        ).text();
    }

    /**
     * Try twice so the resource can be found, mostly a hack to enable j2cl-maven-plugin to work as intended.
     */
    private FileObject readResource(final String packageName,
                                    final String typeName,
                                    final String fileExtension) throws IOException {
        FileObject resource;
        try {
            resource = filer.getResource(StandardLocation.SOURCE_PATH, packageName, typeName + fileExtension);
        } catch (final FileNotFoundException retry) {
            resource = filer.getResource(StandardLocation.CLASS_PATH, packageName, typeName + fileExtension);
        }
        return resource;
    }

    private Elements elements;
    private Filer filer;

    @Override
    public Set<String> getSupportedOptions() {
        return Sets.empty();
    }

    // reporting........................................................................................................

    /**
     * Reports an error during the compile process.
     */
    private void error(final String message) {
        this.messager.printMessage(Kind.ERROR, message);
    }

    private Messager messager;
}
