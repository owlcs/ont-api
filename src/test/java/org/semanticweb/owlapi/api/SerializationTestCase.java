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

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@SuppressWarnings({"javadoc"})
public class SerializationTestCase extends TestBase {

    /**
     * Invite {@link #TEST_STRING_1}, it is to avoid illegal punning Class <-> Datatype
     * (default {@link ru.avicomp.ontapi.jena.impl.conf.OntPersonality} does not allow such things,
     * mode = {@link OntModelConfig.StdMode#MEDIUM}).
     * And NOTE: neither {@link ru.avicomp.ontapi.jena.impl.conf.OntPersonality} and
     * {@link OWLOntologyLoaderConfiguration} in the manager (OWL-5.0.5) are not serializable.
     * So the list of axioms from origin and copy ontologies could be different.
     */
    private static final String TEST_STRING_1 = "testString";
    private static final String TEST_STRING_2 = "testString2";
    private final OWL2Datatype owl2datatype = OWL2Datatype.XSD_INT;
    private final OWLDataProperty dp = df.getOWLDataProperty("urn:test#", "dp");
    private final OWLObjectProperty op = df.getOWLObjectProperty("urn:test#", "op");
    private final IRI iri = IRI.create("urn:test#", "iri");
    private final OWLLiteral owlliteral = df.getOWLLiteral(true);
    private final OWLAnnotationSubject as = IRI.create("urn:test#", "i");
    private final OWLDatatype owldatatype = df.getOWLDatatype(owl2datatype);
    private final OWLDataRange dr = df.getOWLDatatypeRestriction(owldatatype);
    private final OWLAnnotationProperty ap = df.getOWLAnnotationProperty("urn:test#", "ap");
    private final OWLFacet owlfacet = OWLFacet.MIN_EXCLUSIVE;
    private final OWLClassExpression c = df.getOWLClass("urn:test#", "classexpression");
    private final PrefixManager prefixmanager = new DefaultPrefixManager();
    private final OWLIndividual ai = df.getOWLAnonymousIndividual();
    private final OWLAnnotationValue owlannotationvalue = owlliteral;
    private final Set<OWLObjectPropertyExpression> setop = new HashSet<>();
    private final Set<OWLDataPropertyExpression> setdp = new HashSet<>();
    private final List<OWLObjectPropertyExpression> listowlobjectproperties = new ArrayList<>();
    private final Set<OWLIndividual> setowlindividual = new HashSet<>();
    private final Set<OWLPropertyExpression> setowlpropertyexpression = new HashSet<>();


    @SuppressWarnings("null")
    @Test
    public void testrun() throws Exception {
        m.getIRIMappers().set(new AutoIRIMapper(new File("."), false));
        OWLOntology o = m.loadOntologyFromOntologyDocument(getClass().getResourceAsStream("/owlapi/pizza.owl"));

        o.applyChange(new AddImport(o, df.getOWLImportsDeclaration(iri)));
        o.add(df.getOWLDeclarationAxiom(df.getOWLClass(iri)));
        o.add(sub(c, df.getOWLClass(TEST_STRING_2, prefixmanager)));
        o.add(df.getOWLEquivalentClassesAxiom(df.getOWLClass(iri), c));
        o.add(df.getOWLDisjointClassesAxiom(df.getOWLClass(iri), c));
        o.add(df.getOWLSubObjectPropertyOfAxiom(op, op));
        o.add(df.getOWLSubPropertyChainOfAxiom(listowlobjectproperties, op));
        o.add(df.getOWLEquivalentObjectPropertiesAxiom(setop));
        o.add(df.getOWLDisjointObjectPropertiesAxiom(setop));
        o.add(df.getOWLInverseObjectPropertiesAxiom(op, op));
        o.add(df.getOWLObjectPropertyDomainAxiom(op, c));
        o.add(df.getOWLObjectPropertyRangeAxiom(op, c));
        o.add(df.getOWLFunctionalObjectPropertyAxiom(op));
        o.add(df.getOWLAnnotationAssertionAxiom(ap, as, owlannotationvalue));
        o.add(df.getOWLAnnotationPropertyDomainAxiom(ap, iri));
        o.add(df.getOWLAnnotationPropertyRangeAxiom(ap, iri));
        o.add(df.getOWLSubAnnotationPropertyOfAxiom(ap, ap));
        o.add(df.getOWLInverseFunctionalObjectPropertyAxiom(op));
        o.add(df.getOWLReflexiveObjectPropertyAxiom(op));
        o.add(df.getOWLIrreflexiveObjectPropertyAxiom(op));
        o.add(df.getOWLSymmetricObjectPropertyAxiom(op));
        o.add(df.getOWLAsymmetricObjectPropertyAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        o.add(df.getOWLSubDataPropertyOfAxiom(dp, dp));
        o.add(df.getOWLEquivalentDataPropertiesAxiom(setdp));
        o.add(df.getOWLDisjointDataPropertiesAxiom(setdp));
        o.add(df.getOWLDataPropertyDomainAxiom(dp, c));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, dr));
        o.add(df.getOWLFunctionalDataPropertyAxiom(dp));
        o.add(df.getOWLHasKeyAxiom(c, setowlpropertyexpression));
        o.add(df.getOWLDatatypeDefinitionAxiom(owldatatype, dr));
        o.add(df.getOWLSameIndividualAxiom(setowlindividual));
        o.add(df.getOWLDifferentIndividualsAxiom(setowlindividual));
        o.add(df.getOWLClassAssertionAxiom(c, ai));
        o.add(df.getOWLObjectPropertyAssertionAxiom(op, ai, ai));
        o.add(df.getOWLNegativeObjectPropertyAssertionAxiom(op, ai, ai));
        o.add(df.getOWLDataPropertyAssertionAxiom(dp, ai, owlliteral));
        o.add(df.getOWLNegativeDataPropertyAssertionAxiom(dp, ai, owlliteral));
        o.add(df.getOWLInverseObjectPropertiesAxiom(op, df.getOWLObjectInverseOf(op)));
        o.add(sub(c, df.getOWLDataExactCardinality(1, dp)));
        o.add(sub(c, df.getOWLDataMaxCardinality(1, dp)));
        o.add(sub(c, df.getOWLDataMinCardinality(1, dp)));
        o.add(sub(c, df.getOWLObjectExactCardinality(1, op)));
        o.add(sub(c, df.getOWLObjectMaxCardinality(1, op)));
        o.add(sub(c, df.getOWLObjectMinCardinality(1, op)));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDatatype(TEST_STRING_1, prefixmanager)));
        o.add(df.getOWLDataPropertyAssertionAxiom(dp, ai, df.getOWLLiteral(TEST_STRING_1, owldatatype)));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataOneOf(owlliteral)));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataUnionOf(dr)));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataIntersectionOf(dr)));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDatatypeRestriction(owldatatype, owlfacet, owlliteral)));
        o.add(df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDatatypeRestriction(owldatatype, df.getOWLFacetRestriction(owlfacet, 1))));
        o.add(sub(c, df.getOWLObjectIntersectionOf(c, df.getOWLClass(TEST_STRING_2, prefixmanager))));
        o.add(sub(c, df.getOWLDataSomeValuesFrom(dp, dr)));
        o.add(sub(c, df.getOWLDataAllValuesFrom(dp, dr)));
        o.add(sub(c, df.getOWLDataHasValue(dp, owlliteral)));
        o.add(sub(c, df.getOWLObjectComplementOf(df.getOWLClass(iri))));
        o.add(sub(c, df.getOWLObjectOneOf(df.getOWLNamedIndividual(iri))));
        o.add(sub(c, df.getOWLObjectAllValuesFrom(op, c)));
        o.add(sub(c, df.getOWLObjectSomeValuesFrom(op, c)));
        o.add(sub(c, df.getOWLObjectHasValue(op, ai)));
        o.add(sub(c, df.getOWLObjectUnionOf(df.getOWLClass(iri))));
        o.add(df.getOWLAnnotationAssertionAxiom(iri, df.getOWLAnnotation(ap, owlannotationvalue)));
        o.add(df.getOWLAnnotationAssertionAxiom(df.getOWLNamedIndividual(iri).getIRI(), df.getOWLAnnotation(ap, owlannotationvalue)));

        LOGGER.info("Total axioms(pizza) count: " + o.getAxiomCount());
        LOGGER.info("Total ontologies count: " + m.ontologies().count());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(out);
        stream.writeObject(m);
        stream.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(in);
        OWLOntologyManager copy = (OWLOntologyManager) inStream.readObject();

        copy.ontologies().forEach(ont -> {
            OWLOntologyID id = ont.getOntologyID();
            LOGGER.debug("Test " + id);
            OWLOntology init = m.getOntology(id);
            Assert.assertNotNull("Can't find init ontology with id " + id, init);

            AxiomType.AXIOM_TYPES.forEach(t -> {
                if (AxiomType.DECLARATION.equals(t)) return;
                Set<OWLAxiom> expected = init.axioms(t).collect(Collectors.toSet());
                Set<OWLAxiom> actual = ont.axioms(t).collect(Collectors.toSet());
                Assert.assertThat("Incorrect axioms for type <" + t + "> (expected=" + expected.size() +
                        ", actual=" + actual.size() + ")", actual, IsEqual.equalTo(expected));
            });
        });
    }

    protected OWLAxiom sub(OWLClassExpression cl, OWLClassExpression d) {
        return df.getOWLSubClassOfAxiom(cl, d);
    }
}
