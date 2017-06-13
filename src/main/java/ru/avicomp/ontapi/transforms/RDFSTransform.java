/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.transforms;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * To perform preliminary fixing: transform the RDFS ontological graph to the OWL ontological graph.
 * After this conversion is completed there would be an owl-ontology but maybe with some declarations missed and
 * with the RDFS-garbage (rdfs:Class, rdf:Property).
 * It seems it can be considered as an OWL1,
 * while rdfs:Class and rdf:Property would not be removed by the owl-transformer (see {@link OWLTransform}).
 * After working no standalone rdfs:Class or rdf:Property are expected.
 * And Note: this transformer prefers owl:AnnotationProperty and owl:Class in controversial cases.
 * <p>
 * This transformer is optional:
 * if ontology graph already contains one of the five main owl-declarations (owl:Class,
 * owl:ObjectProperty, owl:DatatypeProperty, owl:AnnotationProperty, owl:NamedIndividual) and does not contain
 * rdf:Property and rdfs:Class, then it can not be a pure RDFS-ontology and we believe that there is nothing to do.
 * <p>
 * For some additional info see <a href='https://www.w3.org/TR/rdf-schema'>RDFS specification</a> and, maybe,
 * <a href='https://www.w3.org/TR/2012/REC-owl2-overview-20121211/#Relationship_to_OWL_1'>some words about OWL 1</a>.
 * And of course see our main cheat sheet:
 * <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL 2 Web Ontology Language Quick Reference Guide (Second Edition)</a>
 */
@SuppressWarnings("WeakerAccess")
public class RDFSTransform extends Transform {

    public RDFSTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        parseClasses();
        parseProperties();
    }

    public void parseClasses() {
        statements(null, RDF.type, RDFS.Class).map(Statement::getSubject).forEach(this::processRDFSClass);
    }

    public void parseProperties() {
        // only uri-resources:
        List<Resource> rest1 = statements(null, RDF.type, RDF.Property)
                .map(Statement::getSubject)
                .filter(RDFNode::isURIResource)
                .map(this::processRDFProperty)
                .filter(Objects::nonNull).collect(Collectors.toList());
        // parse the rest resources again:
        List<Resource> rest2 = rest1.stream()
                .map(this::processRDFProperty)
                .filter(Objects::nonNull).collect(Collectors.toList());
        // declare all remaining resources as annotation properties:
        rest2.forEach(r -> declare(r, OWL.AnnotationProperty));
    }

    protected Resource processRDFProperty(Resource property) {
        Resource range = property.getPropertyResourceValue(RDFS.range);
        Resource domain = property.getPropertyResourceValue(RDFS.domain);
        Resource superProperty = property.getPropertyResourceValue(RDFS.subPropertyOf);
        // if no rdfs:domain, rdfs:range and rdfs:subPropertyOf => owl:AnnotationProperty
        if (range == null && domain == null && superProperty == null) {
            declare(property, OWL.AnnotationProperty);
            return null;
        }
        // if rdfs:range and rdfs:domain are present => could be owl:ObjectProperty or owl:DatatypeProperty
        // ('P rdfs:domain C'&'P rdfs:range C' or 'R rdfs:domain C'&'R rdfs:range D')
        if (range != null && domain != null && isClass(domain)) {
            Resource type = isClass(range) ? OWL.ObjectProperty : isDataRange(range) ? OWL.DatatypeProperty : null;
            if (type != null) {
                declare(property, type);
                return null;
            }
        }
        // if superProperty is not null => try to get type from it
        if (superProperty != null) {
            Resource type = isAnnotationProperty(property) ? OWL.AnnotationProperty :
                    isObjectProperty(property) ? OWL.ObjectProperty :
                            isDataProperty(property) ? OWL.DatatypeProperty : null;
            if (type != null) {
                declare(property, type);
                return null;
            }
        }
        // can't determine type, return the same resource to next attempt
        return property;
    }

    public boolean isObjectProperty(Resource candidate) {
        return builtIn.objectProperties().contains(candidate.as(Property.class)) || hasType(candidate, OWL.ObjectProperty);
    }

    public boolean isDataProperty(Resource candidate) {
        return builtIn.datatypeProperties().contains(candidate.as(Property.class)) || hasType(candidate, OWL.DatatypeProperty);
    }

    public boolean isAnnotationProperty(Resource candidate) {
        return builtIn.annotationProperties().contains(candidate.as(Property.class)) || hasType(candidate, OWL.AnnotationProperty);
    }

    protected boolean isDataRange(Resource candidate) {
        return builtIn.datatypes().contains(candidate) || hasType(candidate, RDFS.Datatype);
    }

    protected boolean isClass(Resource candidate) {
        return builtIn.classes().contains(candidate) || hasType(candidate, OWL.Class);
    }

    protected void processRDFSClass(Resource resource) {
        if (hasType(resource, RDFS.Datatype)) return;
        declare(resource, OWL.Class);
    }

    @Override
    public boolean test() {
        return isRDFS() && !isOWL();
    }

    protected boolean isRDFS() {
        return containsType(RDFS.Class) || containsType(RDF.Property);
    }

    protected boolean isOWL() {
        return containsType(OWL.Class)
                || containsType(OWL.NamedIndividual)
                || containsType(OWL.AnnotationProperty)
                || containsType(OWL.DatatypeProperty)
                || containsType(OWL.ObjectProperty);
    }

}
