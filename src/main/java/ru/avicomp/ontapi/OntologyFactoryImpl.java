/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.transforms.TransformException;

import java.util.Objects;

/**
 * The ontology building and loading factory, the 'core' - the main point to create and load ontologies.
 * See also base interface {@link OWLOntologyFactory} and its single implementation
 * <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>.
 * <p>
 * Created by szuev on 24.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntologyFactoryImpl implements OntologyFactory {

    static {
        ErrorHandlerFactory.setDefaultErrorHandler(ErrorHandlerFactory.errorHandlerNoLogging);
    }

    protected final Builder ontologyBuilder;
    protected final Loader ontologyLoader;

    public OntologyFactoryImpl(Builder builder, Loader loader) {
        this.ontologyBuilder = Objects.requireNonNull(builder, "Null builder");
        this.ontologyLoader = Objects.requireNonNull(loader, "Null loader");
    }

    /**
     * Creates a fresh ontology (ONT) inside the given manager.
     * Note: the default format is Turtle, not RDF/XML as in OWL-API impl, since it is more widely used in Jena-world.
     *
     * @param manager {@link OntologyManager}, not null
     * @param id      {@link OWLOntologyID}, not null
     * @return {@link OntologyModel}
     * @see OntFormat#TURTLE
     */
    @Override
    public OntologyModel createOntology(OntologyManager manager, OWLOntologyID id) {
        OntologyModel res = this.ontologyBuilder.createOWLOntology(manager, id);
        OWLAdapter.get().asIMPL(manager).ontologyCreated(res);
        manager.setOntologyFormat(res, OntFormat.TURTLE.createOwlFormat());
        return res;
    }

    @Override
    public OntologyModel loadOntology(OntologyManager manager,
                                      OWLOntologyDocumentSource source,
                                      OntLoaderConfiguration configuration) throws OWLOntologyCreationException {
        return ontologyLoader.load(source, manager, configuration);
    }

    public static class ConfigMismatchException extends OWLOntologyCreationException {
        public ConfigMismatchException(String s) {
            super(s);
        }
    }

    public static class UnsupportedFormatException extends OWLOntologyCreationException {
        private OntFormat format;
        private IRI source;

        public UnsupportedFormatException(String message) {
            super(message);
        }

        public UnsupportedFormatException(Throwable cause) {
            super(cause);
        }

        public UnsupportedFormatException putFormat(OntFormat format) {
            this.format = OntApiException.notNull(format, "Null format");
            return this;
        }

        public UnsupportedFormatException putSource(IRI iri) {
            this.source = OntApiException.notNull(iri, "Null source");
            return this;
        }

        @Override
        public String getMessage() {
            Throwable cause = getCause();
            String msg = super.getMessage();
            if (format != null && source != null && cause != null) {
                String suffix = String.format("Format: %s. IRI: <%s>. Cause: '%s'", format, source, cause.getMessage());
                if (StringUtils.isEmpty(msg)) {
                    msg = suffix;
                } else {
                    msg += ". " + suffix;
                }
            }
            return msg;
        }
    }

    public static class BadRecursionException extends OWLOntologyCreationException {

        public BadRecursionException(String message) {
            super(message);
        }
    }

    public static class OWLTransformException extends OWLOntologyCreationException {

        public OWLTransformException(TransformException cause) {
            super(cause.getMessage(), cause.getCause() == null ? cause : cause.getCause());
        }
    }

}
