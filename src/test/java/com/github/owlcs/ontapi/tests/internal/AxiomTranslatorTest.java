/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.tests.ModelData;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.*;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * To test {@link AxiomTranslator}.
 * Created by @ssz on 31.10.2020.
 */
public class AxiomTranslatorTest {

    public static Stream<Arguments> resourceData() {
        return Stream.of(
                of(OWLDeclarationAxiom.class, ModelData.PIZZA,
                        (f, d) -> f.getOWLDeclarationAxiom(f.getOWLObjectProperty(d.getNS() + "hasCountryOfOrigin")))
                , of(OWLSubClassOfAxiom.class, ModelData.PIZZA, (f, d) -> {
                    OWLClass sub = f.getOWLClass(d.getNS() + "PizzaTopping");
                    OWLClass sup = f.getOWLClass(d.getNS() + "Food");
                    return f.getOWLSubClassOfAxiom(sub, sup);
                })
                , of(OWLSubClassOfAxiom.class, ModelData.PIZZA, (f, d) -> {
                    OWLClass sub = f.getOWLClass(d.getNS() + "RealItalianPizza");
                    OWLObjectProperty p = f.getOWLObjectProperty(d.getNS() + "hasBase");
                    OWLClass c = f.getOWLClass(d.getNS() + "ThinAndCrispyBase");
                    OWLClassExpression sup = f.getOWLObjectAllValuesFrom(p, c);
                    return f.getOWLSubClassOfAxiom(sub, sup);
                })
                , of(OWLDisjointClassesAxiom.class, ModelData.PIZZA, (f, d) -> {
                    OWLClass c1 = f.getOWLClass(d.getNS() + "American");
                    OWLClass c2 = f.getOWLClass(d.getNS() + "AmericanHot");
                    return f.getOWLDisjointClassesAxiom(c1, c2);
                })
                , of(OWLDifferentIndividualsAxiom.class, ModelData.TRAVEL, (f, d) -> {
                    OWLNamedIndividual i1 = f.getOWLNamedIndividual(d.getNS() + "ThreeStarRating");
                    OWLNamedIndividual i2 = f.getOWLNamedIndividual(d.getNS() + "TwoStarRating");
                    return f.getOWLDifferentIndividualsAxiom(i1, i2);
                })
                , of(OWLObjectPropertyRangeAxiom.class, ModelData.PIZZA, (f, d) -> {
                    OWLObjectProperty property = f.getOWLObjectProperty(d.getNS() + "hasIngredient");
                    OWLClass clazz = f.getOWLClass(d.getNS() + "Food");
                    return f.getOWLObjectPropertyRangeAxiom(property, clazz);
                })
                , of(OWLDataPropertyRangeAxiom.class, ModelData.FAMILY, (f, d) -> {
                    OWLDataProperty p = f.getOWLDataProperty(d.getNS() + "hasMarriageYear");
                    OWLDatatype r = f.getIntegerOWLDatatype();
                    return f.getOWLDataPropertyRangeAxiom(p, r);
                })
                , of(OWLObjectPropertyDomainAxiom.class, ModelData.PIZZA, (f, d) -> {
                    OWLObjectProperty property = f.getOWLObjectProperty(d.getNS() + "hasIngredient");
                    OWLClass clazz = f.getOWLClass(d.getNS() + "Food");
                    return f.getOWLObjectPropertyDomainAxiom(property, clazz);
                })
                , of(OWLDataPropertyDomainAxiom.class, ModelData.FAMILY, (f, d) -> {
                    OWLDataProperty p = f.getOWLDataProperty(d.getNS() + "hasMarriageYear");
                    OWLClass c = f.getOWLClass(d.getNS() + "Marriage");
                    return f.getOWLDataPropertyDomainAxiom(p, c);
                })
                , of(OWLSubObjectPropertyOfAxiom.class, ModelData.PIZZA, (f, d) -> {
                    OWLObjectProperty sub = f.getOWLObjectProperty(d.getNS() + "isBaseOf");
                    OWLObjectProperty sup = f.getOWLObjectProperty(d.getNS() + "isIngredientOf");
                    return f.getOWLSubObjectPropertyOfAxiom(sub, sup);
                })
                , of(OWLSubDataPropertyOfAxiom.class, ModelData.FAMILY, (f, d) -> {
                    OWLDataProperty sub = f.getOWLDataProperty(d.getNS() + "formerlyKnownAs");
                    OWLDataProperty sup = f.getOWLDataProperty(d.getNS() + "knownAs");
                    return f.getOWLSubDataPropertyOfAxiom(sub, sup);
                })
                , of(OWLSubAnnotationPropertyOfAxiom.class, ModelData.NCBITAXON_CUT, (f, d) -> {
                    OWLAnnotationProperty sub = f.getOWLAnnotationProperty(SKOS.editorialNote.getURI());
                    OWLAnnotationProperty sup = f.getOWLAnnotationProperty(SKOS.note.getURI());
                    return f.getOWLSubAnnotationPropertyOfAxiom(sub, sup);
                })
                , of(OWLAnnotationAssertionAxiom.class, ModelData.NCBITAXON_CUT, (f, d) -> {
                    OWLNamedIndividual s = f.getOWLNamedIndividual(d.getNS() + "46063");
                    OWLAnnotationProperty p = f.getOWLAnnotationProperty(SKOS.prefLabel.getURI());
                    OWLLiteral o = f.getOWLLiteral("Chelone", "en");
                    return f.getOWLAnnotationAssertionAxiom(p, s.getIRI(), o);
                })
                , of(OWLDataPropertyAssertionAxiom.class, ModelData.FAMILY, (f, d) -> {
                    OWLNamedIndividual s = f.getOWLNamedIndividual(d.getNS() + "louise_templar_1993");
                    OWLDataProperty p = f.getOWLDataProperty(d.getNS() + "hasBirthYear");
                    OWLLiteral o = f.getOWLLiteral(1993);
                    return f.getOWLDataPropertyAssertionAxiom(p, s, o);
                })
                , of(OWLObjectPropertyAssertionAxiom.class, ModelData.FAMILY, (f, d) -> {
                    OWLNamedIndividual s = f.getOWLNamedIndividual(d.getNS() + "louise_templar_1993");
                    OWLObjectProperty p = f.getOWLObjectProperty(d.getNS() + "hasFather");
                    OWLNamedIndividual o = f.getOWLNamedIndividual(d.getNS() + "stephen_templar_1960");
                    return f.getOWLObjectPropertyAssertionAxiom(p, s, o);
                })
                , of(OWLClassAssertionAxiom.class, ModelData.FAMILY, (f, d) -> {
                    OWLNamedIndividual s = f.getOWLNamedIndividual(d.getNS() + "louise_templar_1993");
                    OWLClass c = f.getOWLClass(d.getNS() + "Woman");
                    return f.getOWLClassAssertionAxiom(c, s);
                })
                , of(OWLInverseFunctionalObjectPropertyAxiom.class, ModelData.PIZZA,
                        (f, d) -> f.getOWLInverseFunctionalObjectPropertyAxiom(f.getOWLObjectProperty(d.getNS() + "isBaseOf")))
        );
    }

    public static Stream<Arguments> dynamicData() {
        String ns = "http://x#";
        String u1 = ns + "u1";
        String u2 = ns + "u2";
        BlankNodeId b1 = BlankNodeId.create();
        BlankNodeId b2 = BlankNodeId.create();
        return Stream.of(
                of(OWLDisjointObjectPropertiesAxiom.class,
                        m -> createDisjointObjectPropertiesModel(m, ns, u1, u2),
                        f -> f.getOWLDisjointObjectPropertiesAxiom(f.getOWLObjectProperty(u1), f.getOWLObjectProperty(u2)))
                , of(OWLSameIndividualAxiom.class,
                        m -> createSameIndividualsModel(m, ns, b1, b2),
                        f -> f.getOWLSameIndividualAxiom(f.getOWLAnonymousIndividual(b1), f.getOWLAnonymousIndividual(b2)))
                , of(OWLDatatypeDefinitionAxiom.class,
                        m -> createDatatypeDefinitionModel(m, ns, u1, u2),
                        f -> f.getOWLDatatypeDefinitionAxiom(f.getOWLDatatype(u1), f.getOWLDatatype(u2)))
                , of(OWLInverseObjectPropertiesAxiom.class,
                        m -> createInverseObjectPropertiesModel(m, ns, u1, u2),
                        f -> f.getOWLInverseObjectPropertiesAxiom(f.getOWLObjectProperty(u1), f.getOWLObjectProperty(u2)))
        );
    }

    private static <X> Arguments of(Class<X> type, ModelData data, BiFunction<OWLDataFactory, ModelData, X> createAxiom) {
        return Arguments.of(type, data, (Function<OWLDataFactory, X>) f -> createAxiom.apply(f, data));
    }

    private static <X> Arguments of(Class<X> type,
                                    Function<OntologyManager, OntModel> createModel,
                                    Function<DataFactory, X> createAxiom) {
        return Arguments.of(type, createModel, createAxiom);
    }

    private static OntModel createDisjointObjectPropertiesModel(OntologyManager m, String ns, String p1, String p2) {
        OntModel res = m.createOntology()
                .asGraphModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        res.createDisjointObjectProperties(res.createObjectProperty(p1), res.createObjectProperty(p2));
        return res;
    }

    private static OntModel createSameIndividualsModel(OntologyManager m, String ns, BlankNodeId b1, BlankNodeId b2) {
        OntModel res = m.createOntology()
                .asGraphModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        OntIndividual i1 = res.createResource(new AnonId(b1))
                .addProperty(RDF.type, res.createOntClass(ns + "C1"))
                .as(OntIndividual.class);
        OntIndividual i2 = res.createResource(new AnonId(b2))
                .addProperty(RDF.type, res.createOntClass(ns + "C2"))
                .as(OntIndividual.class);
        i1.addSameAsStatement(i2);
        return res;
    }

    private static OntModel createDatatypeDefinitionModel(OntologyManager m, String ns, String d1, String d2) {
        OntModel res = m.createOntology()
                .asGraphModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        res.createDatatype(d1).addEquivalentClass(res.createDatatype(d2));
        return res;
    }

    private static OntModel createInverseObjectPropertiesModel(OntologyManager m, String ns, String p1, String p2) {
        OntModel res = m.createOntology()
                .asGraphModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        res.createObjectProperty(p1).addInverseOfStatement(res.createObjectProperty(p2));
        return res;
    }

    private OntologyManager newManager() {
        return OntManagers.createManager();
    }

    @ParameterizedTest(name = "[{index}] ::: type={0}, model={1}")
    @MethodSource("resourceData")
    public <X extends OWLAxiom> void testContainsAndFind(Class<X> type, ModelData data, Function<OWLDataFactory, X> get) {
        testContainsAndFind(m -> ((Ontology) data.fetch(m)).asGraphModel(), type, get);
    }

    @ParameterizedTest(name = "[{index}] ::: type={0}")
    @MethodSource("dynamicData")
    public <X extends OWLAxiom> void testContainsAndFind(Class<X> type,
                                                         Function<OntologyManager, OntModel> createModel,
                                                         Function<DataFactory, X> createAxiom) {
        testContainsAndFind(m -> createModel.apply((OntologyManager) m), type, f -> createAxiom.apply((DataFactory) f));
        testContainsAndFind(createModel, type);
    }

    private <X extends OWLAxiom> void testContainsAndFind(Function<OntologyManager, OntModel> create, Class<X> type) {
        OntologyManager m = newManager();
        testContainsAndFind(x -> create.apply(m)
                , type
                , f -> m.ontologies().findFirst().orElseThrow(AssertionError::new)
                        .axioms(AxiomType.getTypeForClass(type))
                        .findFirst().orElseThrow(AssertionError::new));
    }

    private <X extends OWLAxiom> void testContainsAndFind(Function<OWLOntologyManager, OntModel> createOntModel,
                                                          Class<X> axiomType,
                                                          Function<OWLDataFactory, X> getAxiom) {
        OWLOntologyManager m = newManager();
        OWLDataFactory df = m.getOWLDataFactory();
        AxiomTranslator<X> tr = AxiomParserProvider.get(axiomType);
        Assertions.assertNotNull(tr);

        OntModel ont = createOntModel.apply(m);
        X key = getAxiom.apply(df);
        ONTObjectFactory f = AxiomTranslator.getObjectFactory(ont);
        InternalConfig c = AxiomTranslator.getConfig(ont);
        Optional<ONTObject<X>> ax = tr.findONTObject(key, ont, f, c);
        Assertions.assertTrue(ax.isPresent());
        Assertions.assertTrue(tr.containsONTObject(key, ont, f, c));

        X key2 = key.getAnnotatedAxiom(Collections.singleton(df.getRDFSComment("XXXX")));
        Assertions.assertFalse(tr.containsONTObject(key2, ont, f, c));
        Assertions.assertFalse(tr.findONTObject(key2, ont, f, c).isPresent());

        Model toDelete = ModelFactory.createModelForGraph(ax.get().toGraph());
        ont.remove(toDelete);

        Assertions.assertFalse(tr.containsONTObject(key, ont, f, c));
        Assertions.assertFalse(tr.findONTObject(key, ont, f, c).isPresent());
    }

}
