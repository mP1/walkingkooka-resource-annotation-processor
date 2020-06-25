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

import walkingkooka.collect.set.*;
import walkingkooka.resource.*;
import walkingkooka.text.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.Diagnostic.*;
import javax.tools.*;
import java.io.*;
import java.util.*;

/**
 * An {@link AbstractProcessor} that creates a class that creates a class for the class including the annotation
 * {@link walkingkooka.resource.TextResourceAware}
 */
public final class TextResourceAwareProviderAnnotationProcessor extends AbstractProcessor {

    public TextResourceAwareProviderAnnotationProcessor() {
        super();
    }

    @Override
    public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public final Set<String> getSupportedAnnotationTypes() {
        return Sets.of(TextResourceAware.class.getName());
    }

    @Override
    public final synchronized void init(final ProcessingEnvironment environment) {
        super.init(environment);

        this.elements = environment.getElementUtils();
        this.filer = environment.getFiler();
        this.messager = environment.getMessager();
    }

    @Override
    public final boolean process(final Set<? extends TypeElement> annotations,
                                 final RoundEnvironment environment) {
        try {
            for (TypeElement annotation : annotations) {
                for (final Element annotated : environment.getElementsAnnotatedWith(annotation)) {
                    if (annotated instanceof TypeElement) {
                        this.process0((TypeElement) annotated, environment);
                    }
                }
            }
        } catch (final Exception cause) {
            this.error(cause.getMessage());
        }

        return false; // whether or not the set of annotation types are claimed by this processor
    }

    private void process0(final TypeElement root,
                          final RoundEnvironment environment) throws Exception {
        final String enclosing = root.getQualifiedName().toString();
        final String provider = enclosing + PROVIDER;
        final TypeElement exists = this.elements.getTypeElement(provider);

        // assume null means generated source does not exist...
        if (null == exists) {
            // without this check the generated class will be written multiple times resulting in an exception when attempting to create the file.
            final String typeName = root.getSimpleName().toString();
            final String packageName = CharSequences.subSequence(enclosing, 0, -typeName.length() - 1)
                    .toString();
            final String providerTypeSimpleName = typeName + PROVIDER;
            final Filer filer = this.filer;

            try (final Writer writer = filer.createSourceFile(provider).openWriter()) {
                final String providerTemplate = this.providerTemplate();

                final String text = this.readResource(packageName, typeName)
                        .getCharContent(false)
                        .toString();

                writer.write(providerTemplate.replace("$PACKAGE", packageName)
                        .replace("$NAME", providerTypeSimpleName)
                        .replace("$TEXT", CharSequences.quoteAndEscape(text)));
                writer.flush();
            }
        }
    }

    private final static String PROVIDER = "Provider";

    /**
     * Loads the template which has a few placeholders for package, type name and the text being returned.
     */
    private String providerTemplate() throws TextResourceException {
        return TextResources.classPath(this.getClass().getSimpleName() + ".txt", this.getClass())
                .text();
    }

    /**
     * Try twice so the resource can be found, mostly a hack to enable j2cl-maven-plugin to work as intended.
     */
    private FileObject readResource(final String packageName, final String typeName) throws IOException {
        FileObject resource;
        try {
            resource = filer.getResource(StandardLocation.SOURCE_PATH, packageName, typeName + ".txt");
        } catch (final FileNotFoundException retry) {
            resource = filer.getResource(StandardLocation.CLASS_PATH, packageName, typeName + ".txt");
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
