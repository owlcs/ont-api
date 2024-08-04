/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.testutils;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.stream.Stream;

/**
 * Created by @ssz on 01.05.2020.
 */
public class OWLEntityUtils {

    public static <X extends OWLEntity> Resource getResourceType(Class<X> type) {
        if (type == OWLClass.class)
            return OWL.Class;
        if (type == OWLDatatype.class)
            return RDFS.Datatype;
        if (type == OWLNamedIndividual.class)
            return OWL.NamedIndividual;
        if (type == OWLAnnotationProperty.class)
            return OWL.AnnotationProperty;
        if (type == OWLDataProperty.class)
            return OWL.DatatypeProperty;
        if (type == OWLObjectProperty.class)
            return OWL.ObjectProperty;
        throw new IllegalArgumentException("Wrong argument: " + type);
    }

    @SuppressWarnings("unchecked")
    public static <X extends OWLEntity> EntityType<X> getEntityType(Class<X> type) {
        if (type == OWLClass.class)
            return (EntityType<X>) EntityType.CLASS;
        if (type == OWLDatatype.class)
            return (EntityType<X>) EntityType.DATATYPE;
        if (type == OWLNamedIndividual.class)
            return (EntityType<X>) EntityType.NAMED_INDIVIDUAL;
        if (type == OWLAnnotationProperty.class)
            return (EntityType<X>) EntityType.ANNOTATION_PROPERTY;
        if (type == OWLDataProperty.class)
            return (EntityType<X>) EntityType.DATA_PROPERTY;
        if (type == OWLObjectProperty.class)
            return (EntityType<X>) EntityType.OBJECT_PROPERTY;
        throw new IllegalArgumentException("Wrong argument: " + type);
    }

    @SuppressWarnings("unchecked")
    public static <X extends OWLEntity> Stream<X> signature(OWLOntology x, Class<X> type) {
        if (type == OWLClass.class)
            return (Stream<X>) x.classesInSignature();
        if (type == OWLDatatype.class)
            return (Stream<X>) x.datatypesInSignature();
        if (type == OWLNamedIndividual.class)
            return (Stream<X>) x.individualsInSignature();
        if (type == OWLAnnotationProperty.class)
            return (Stream<X>) x.annotationPropertiesInSignature();
        if (type == OWLDataProperty.class)
            return (Stream<X>) x.dataPropertiesInSignature();
        if (type == OWLObjectProperty.class)
            return (Stream<X>) x.objectPropertiesInSignature();
        throw new IllegalArgumentException("Wrong argument: " + type);
    }
}
