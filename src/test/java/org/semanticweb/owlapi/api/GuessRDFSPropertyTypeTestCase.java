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

package org.semanticweb.owlapi.api;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.search.Searcher;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.semanticweb.owlapi.model.parameters.Imports.INCLUDED;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asUnorderedSet;

/**
 * This is a test of the property type guessing for rdf:Property instances of
 * the kind encountered when attempting to parse an rdfs schema.
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
@SuppressWarnings({"javadoc", "null"})
public class GuessRDFSPropertyTypeTestCase extends TestBase {

    private static final
    @Nonnull
    String CIDOC_FILE = "cidoc_crm_v5.0.4_official_release.rdfs.xml";
    private static final
    @Nonnull
    String CIDOC_PREFIX = "http://www.cidoc-crm.org/cidoc-crm/";
    private
    @Nonnull
    OWLOntology cidocOntology;
    private
    @Nonnull
    PrefixDocumentFormat prefixOWLDocumentFormat;

    @Before
    public void setUp() {
        cidocOntology = ontologyFromClasspathFile(CIDOC_FILE, config);
        assertNotNull(cidocOntology);
        OWLDocumentFormat format = cidocOntology.getFormat();
        assertNotNull(format);
        assertTrue(format.isPrefixOWLDocumentFormat());
        prefixOWLDocumentFormat = format.asPrefixOWLDocumentFormat();
        prefixOWLDocumentFormat.setDefaultPrefix(CIDOC_PREFIX);
    }

    @Test
    public void testObjectProperty() {
        testProperty("P11_had_participant", "E5_Event", "E39_Actor", "P12_occurred_in_the_presence_of");
    }

    @Test
    public void testDataProperty() {
        testProperty("P79_beginning_is_qualified_by", "E52_Time-Span", "http://www.w3.org/2000/01/rdf-schema#Literal",
                "P3_has_note");
    }

    public void testProperty(String propertyName, String expectedDomain, String expectedRange,
                             String expectedSuperProperty) {
        IRI p11IRI = prefixOWLDocumentFormat.getIRI(propertyName);
        Set<OWLEntity> hadParticipant = asUnorderedSet(cidocOntology.entitiesInSignature(p11IRI));
        assertEquals("should have found " + propertyName, 1, hadParticipant.size());
        OWLEntity entity = hadParticipant.iterator().next();
        assertTrue("EntityType", OWLProperty.class.isAssignableFrom(entity.getClass()));
        if (entity.isOWLObjectProperty()) {
            testProperty(entity.asOWLObjectProperty(), expectedDomain, expectedRange, expectedSuperProperty);
        }
        if (entity.isOWLDataProperty()) {
            testProperty(entity.asOWLDataProperty(), expectedDomain, expectedRange, expectedSuperProperty);
        }
    }

    private void testProperty(OWLObjectProperty p11property, String expectedDomain, String expectedRange,
                              String expectedSuperProperty) {
        Stream<OWLClassExpression> rangeStream = Searcher.range(cidocOntology.objectPropertyRangeAxioms(p11property));
        Collection<OWLClassExpression> ranges = asUnorderedSet(rangeStream);
        assertEquals("should have 1 range", 1, ranges.size());
        HasIRI range = (HasIRI) ranges.iterator().next();
        IRI rangeIRI = range.getIRI();
        IRI expectedIRI = IRI.create(expectedRange);
        if (!expectedIRI.isAbsolute()) {
            expectedIRI = prefixOWLDocumentFormat.getIRI(expectedRange);
        }
        assertEquals("range", expectedIRI, rangeIRI);
        Stream<OWLClassExpression> domainStream = Searcher.domain(cidocOntology.objectPropertyDomainAxioms(
                p11property));
        Collection<OWLClassExpression> domains = asUnorderedSet(domainStream);
        assertEquals("should have 1 domain", 1, domains.size());
        HasIRI domain = (HasIRI) domains.iterator().next();
        IRI domainIRI = domain.getIRI();
        assertEquals("domain should be E5_Event", prefixOWLDocumentFormat.getIRI(expectedDomain), domainIRI);
        Stream<OWLObjectPropertyExpression> superStream = Searcher.sup(cidocOntology.axioms(
                Filters.subObjectPropertyWithSub, p11property, INCLUDED));
        Collection<OWLObjectPropertyExpression> superProperties = asUnorderedSet(superStream);
        // Set<OWLPropertyExpression> superProperties =
        // p11_property.getSuperProperties(cidocOntology);
        assertEquals("should have 1 super Property", 1, superProperties.size());
        HasIRI superProperty = (HasIRI) superProperties.iterator().next();
        IRI superPropertyIRI = superProperty.getIRI();
        assertEquals("super property ", prefixOWLDocumentFormat.getIRI(expectedSuperProperty), superPropertyIRI);
    }

    private void testProperty(OWLDataProperty p11property, String expectedDomain, String expectedRange,
                              String expectedSuperProperty) {
        Stream<OWLClassExpression> rangeClasses = Searcher.range(cidocOntology.dataPropertyRangeAxioms(p11property));
        Collection<OWLClassExpression> ranges = asUnorderedSet(rangeClasses);
        assertEquals("should have 1 range", 1, ranges.size());
        HasIRI range = (HasIRI) ranges.iterator().next();
        IRI rangeIRI = range.getIRI();
        IRI expectedIRI = IRI.create(expectedRange);
        if (!expectedIRI.isAbsolute()) {
            expectedIRI = prefixOWLDocumentFormat.getIRI(expectedRange);
        }
        assertEquals("range", expectedIRI, rangeIRI);
        Stream<OWLClassExpression> domainStream = Searcher.domain(cidocOntology.dataPropertyDomainAxioms(p11property));
        Collection<OWLClassExpression> domains = asUnorderedSet(domainStream);
        // p11_property .getDomains(cidocOntology);
        assertEquals("should have 1 domain", 1, domains.size());
        HasIRI domain = (HasIRI) domains.iterator().next();
        IRI domainIRI = domain.getIRI();
        assertEquals("domain should be E5_Event", prefixOWLDocumentFormat.getIRI(expectedDomain), domainIRI);
        Stream<OWLObjectPropertyExpression> supStream = Searcher.sup(cidocOntology.axioms(
                Filters.subDataPropertyWithSub, p11property, INCLUDED));
        Collection<OWLObjectPropertyExpression> superProperties = asUnorderedSet(supStream);
        // Set<OWLPropertyExpression> superProperties =
        // p11_property.getSuperProperties(cidocOntology);
        assertEquals("should have 1 super Property", 1, superProperties.size());
        HasIRI superProperty = (HasIRI) superProperties.iterator().next();
        IRI superPropertyIRI = superProperty.getIRI();
        assertEquals("super property ", prefixOWLDocumentFormat.getIRI(expectedSuperProperty), superPropertyIRI);
    }

    @Test
    public void testObjectPropertyAndDataPropertySetsNonTriviallyDisjoint() {
        Set<OWLObjectProperty> objectProperties = asUnorderedSet(cidocOntology.objectPropertiesInSignature());
        Set<OWLDataProperty> dataProperties = asUnorderedSet(cidocOntology.dataPropertiesInSignature());
        assertFalse("should have some object Properties", objectProperties.isEmpty());
        assertFalse("should have some data Properties", dataProperties.isEmpty());
        assertTrue("object properties and data properties should be disjoint", Collections.disjoint(objectProperties,
                dataProperties));
    }

    @Test
    public void testAnnotationPropertyCount() {
        assertEquals("should only have 2 rdfs annotation properties", 2, cidocOntology.annotationPropertiesInSignature(
                INCLUDED).count());
    }
}
