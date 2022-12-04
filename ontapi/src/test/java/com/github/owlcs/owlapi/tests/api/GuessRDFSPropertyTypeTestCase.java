/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.owlapi.tests.api;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.search.Searcher;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a test of the property type guessing for rdf:Property instances of
 * the kind encountered when attempting to parse a rdfs schema.
 * <p/>
 * The CIDOC schema is a good test case, because it every property has a domain
 * and a range specified; some properties have a range of Literal (and hence are
 * data properties), and there are subclass relations specified for both data
 * and object properties.
 * <p/>
 * There should be no Annotation Properties.
 * <p/>
 * See <a href="http://www.cidoc-crm.org/">The CIDOC Web Site</a> for more
 * details.
 */
public class GuessRDFSPropertyTypeTestCase extends TestBase {

    private static final String CIDOC_FILE = "cidoc_crm_v5.0.4_official_release.rdfs.xml";
    private static final String CIDOC_PREFIX = "http://www.cidoc-crm.org/cidoc-crm/";
    private OWLOntology cidocOntology;
    private PrefixDocumentFormat prefixOWLDocumentFormat;

    @BeforeEach
    public void setUp() {
        cidocOntology = ontologyFromClasspathFile(CIDOC_FILE, config);
        Assertions.assertNotNull(cidocOntology);
        OWLDocumentFormat format = cidocOntology.getFormat();
        Assertions.assertNotNull(format);
        Assertions.assertTrue(format.isPrefixOWLDocumentFormat());
        prefixOWLDocumentFormat = format.asPrefixOWLDocumentFormat();
        prefixOWLDocumentFormat.setDefaultPrefix(CIDOC_PREFIX);
    }

    @Test
    public void testObjectProperty() {
        testProperty("P11_had_participant", "E5_Event", "E39_Actor", "P12_occurred_in_the_presence_of");
    }

    @Test
    public void testDataProperty() {
        testProperty("P79_beginning_is_qualified_by",
                "E52_Time-Span", "http://www.w3.org/2000/01/rdf-schema#Literal", "P3_has_note");
    }

    public void testProperty(String propertyName,
                             String expectedDomain,
                             String expectedRange,
                             String expectedSuperProperty) {
        IRI p11IRI = prefixOWLDocumentFormat.getIRI(propertyName);
        Set<OWLEntity> hadParticipant = cidocOntology.entitiesInSignature(p11IRI).collect(Collectors.toSet());
        Assertions.assertEquals(1, hadParticipant.size(), "should have found " + propertyName);
        OWLEntity entity = hadParticipant.iterator().next();
        Assertions.assertTrue(OWLProperty.class.isAssignableFrom(entity.getClass()));
        if (entity.isOWLObjectProperty()) {
            testProperty(entity.asOWLObjectProperty(), expectedDomain, expectedRange, expectedSuperProperty);
        }
        if (entity.isOWLDataProperty()) {
            testProperty(entity.asOWLDataProperty(), expectedDomain, expectedRange, expectedSuperProperty);
        }
    }

    private void testProperty(OWLObjectProperty p11property,
                              String expectedDomain,
                              String expectedRange,
                              String expectedSuperProperty) {
        Stream<OWLClassExpression> rangeStream = Searcher.range(cidocOntology.objectPropertyRangeAxioms(p11property));
        Collection<OWLClassExpression> ranges = rangeStream.collect(Collectors.toSet());
        Assertions.assertEquals(1, ranges.size());
        HasIRI range = (HasIRI) ranges.iterator().next();
        IRI rangeIRI = range.getIRI();
        IRI expectedIRI = IRI.create(expectedRange);
        if (!expectedIRI.isAbsolute()) {
            expectedIRI = prefixOWLDocumentFormat.getIRI(expectedRange);
        }
        Assertions.assertEquals(expectedIRI, rangeIRI);
        Stream<OWLClassExpression> domainStream = Searcher.domain(cidocOntology.objectPropertyDomainAxioms(p11property));
        Collection<OWLClassExpression> domains = domainStream.collect(Collectors.toSet());
        Assertions.assertEquals(1, domains.size());
        HasIRI domain = (HasIRI) domains.iterator().next();
        IRI domainIRI = domain.getIRI();
        Assertions.assertEquals(prefixOWLDocumentFormat.getIRI(expectedDomain), domainIRI);
        Stream<OWLObjectPropertyExpression> superStream = Searcher.sup(cidocOntology.axioms(
                Filters.subObjectPropertyWithSub, p11property, Imports.INCLUDED));
        Collection<OWLObjectPropertyExpression> superProperties = superStream.collect(Collectors.toSet());
        Assertions.assertEquals(1, superProperties.size());
        HasIRI superProperty = (HasIRI) superProperties.iterator().next();
        IRI superPropertyIRI = superProperty.getIRI();
        Assertions.assertEquals(prefixOWLDocumentFormat.getIRI(expectedSuperProperty), superPropertyIRI);
    }

    private void testProperty(OWLDataProperty p11property,
                              String expectedDomain,
                              String expectedRange,
                              String expectedSuperProperty) {
        Stream<OWLClassExpression> rangeClasses = Searcher.range(cidocOntology.dataPropertyRangeAxioms(p11property));
        Collection<OWLClassExpression> ranges = rangeClasses.collect(Collectors.toSet());
        Assertions.assertEquals(1, ranges.size());
        HasIRI range = (HasIRI) ranges.iterator().next();
        IRI rangeIRI = range.getIRI();
        IRI expectedIRI = IRI.create(expectedRange);
        if (!expectedIRI.isAbsolute()) {
            expectedIRI = prefixOWLDocumentFormat.getIRI(expectedRange);
        }
        Assertions.assertEquals(expectedIRI, rangeIRI);
        Stream<OWLClassExpression> domainStream = Searcher.domain(cidocOntology.dataPropertyDomainAxioms(p11property));
        Collection<OWLClassExpression> domains = domainStream.collect(Collectors.toSet());
        // p11_property .getDomains(cidocOntology);
        Assertions.assertEquals(1, domains.size());
        HasIRI domain = (HasIRI) domains.iterator().next();
        IRI domainIRI = domain.getIRI();
        Assertions.assertEquals(prefixOWLDocumentFormat.getIRI(expectedDomain), domainIRI);
        Stream<OWLObjectPropertyExpression> supStream = Searcher.sup(cidocOntology.axioms(
                Filters.subDataPropertyWithSub, p11property, Imports.INCLUDED));
        Collection<OWLObjectPropertyExpression> superProperties = supStream.collect(Collectors.toSet());
        Assertions.assertEquals(1, superProperties.size());
        HasIRI superProperty = (HasIRI) superProperties.iterator().next();
        IRI superPropertyIRI = superProperty.getIRI();
        Assertions.assertEquals(prefixOWLDocumentFormat.getIRI(expectedSuperProperty), superPropertyIRI);
    }

    @Test
    public void testObjectPropertyAndDataPropertySetsNonTriviallyDisjoint() {
        Set<OWLObjectProperty> objectProperties = cidocOntology.objectPropertiesInSignature().collect(Collectors.toSet());
        Set<OWLDataProperty> dataProperties = cidocOntology.dataPropertiesInSignature().collect(Collectors.toSet());
        Assertions.assertFalse(objectProperties.isEmpty());
        Assertions.assertFalse(dataProperties.isEmpty());
        Assertions.assertTrue(Collections.disjoint(objectProperties,
                dataProperties));
    }

    @Test
    public void testAnnotationPropertyCount() {
        Assertions.assertEquals(2,
                cidocOntology.annotationPropertiesInSignature(Imports.INCLUDED).count());
    }
}
