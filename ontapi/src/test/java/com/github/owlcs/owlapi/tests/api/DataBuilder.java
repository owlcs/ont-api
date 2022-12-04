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

import com.github.owlcs.owlapi.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DataBuilder {

    private final OWLDataFactory df;
    private final OWLOntologyManager m;

    private final OWLAnnotationProperty ap;
    private final OWLObjectProperty op;
    private final OWLDataProperty dp;
    private final OWLLiteral lit;
    private final OWLLiteral plainlit;
    private final IRI iri;
    private final Collection<OWLAnnotation> as;
    private final OWLClass ce;
    private final OWLNamedIndividual i;
    private final OWLNamedIndividual j;
    private final OWLDatatype d;
    private final Collection<OWLDataProperty> dps;
    private final Collection<OWLObjectProperty> ops;
    private final Collection<OWLClass> classes;
    private final Collection<OWLNamedIndividual> inds;
    private final Collection<SWRLAtom> body;
    private final Collection<SWRLAtom> head;
    private final Collection<SWRLAtom> body2;
    private final Collection<SWRLAtom> head2;

    public DataBuilder() {
        this(clear(OWLManager.createOWLOntologyManager()));
    }

    public DataBuilder(OWLOntologyManager m) {
        this.m = Objects.requireNonNull(m);
        this.df = m.getOWLDataFactory();

        // constants:
        this.ap = df.getOWLAnnotationProperty("urn:test#", "ann");
        this.op = df.getOWLObjectProperty("urn:test#", "op");
        this.dp = df.getOWLDataProperty("urn:test#", "dp");
        this.lit = df.getOWLLiteral(false);
        this.plainlit = df.getOWLLiteral("string", "en");
        this.iri = IRI.create("urn:test#", "iri");
        this.as = Collections.singletonList(df.getOWLAnnotation(ap, df.getOWLLiteral("test")));
        this.ce = df.getOWLClass("urn:test#", "c");
        this.i = df.getOWLNamedIndividual("urn:test#", "i");
        this.j = df.getOWLNamedIndividual("urn:test#", "j");
        this.d = df.getOWLDatatype("urn:test#", "datatype");
        this.dps = Arrays.asList(df.getOWLDataProperty(iri), dp);
        this.ops = Arrays.asList(df.getOWLObjectProperty(iri), op);
        this.classes = Arrays.asList(df.getOWLClass(iri), ce);
        this.inds = Arrays.asList(i, df.getOWLNamedIndividual(iri));
        SWRLAtom v1 = df.getSWRLBuiltInAtom(IRI.create("urn:swrl:var#", "v1"),
                Arrays.asList(df.getSWRLVariable("urn:swrl:var#", "var3"), df.getSWRLVariable("urn:swrl:var#", "var4")));
        SWRLAtom v2 = df.getSWRLBuiltInAtom(IRI.create("urn:swrl:var#", "v2"),
                Arrays.asList(df.getSWRLVariable("urn:swrl:var#", "var5"), df.getSWRLVariable("urn:swrl:var#", "var6")));
        this.body = Collections.singletonList(v1);
        this.head = Collections.singletonList(v2);
        SWRLDArgument var1 = df.getSWRLVariable("urn:swrl:var#", "var1");
        List<SWRLDArgument> var1list = Collections.singletonList(var1);
        SWRLIArgument var2 = df.getSWRLVariable("urn:swrl:var#", "var2");
        this.body2 = Arrays.asList(v1,
                df.getSWRLClassAtom(ce, var2),
                df.getSWRLDataRangeAtom(d, var1),
                df.getSWRLBuiltInAtom(iri, var1list),
                df.getSWRLDifferentIndividualsAtom(var2, df.getSWRLIndividualArgument(i)),
                df.getSWRLSameIndividualAtom(var2, df.getSWRLIndividualArgument(df.getOWLNamedIndividual(iri))),
                df.getSWRLBuiltInAtom(iri, var1list));
        this.head2 = Arrays.asList(v2,
                df.getSWRLDataPropertyAtom(dp, var2, df.getSWRLLiteralArgument(lit)),
                df.getSWRLObjectPropertyAtom(op, var2, var2));
    }

    private static OWLOntologyManager clear(OWLOntologyManager m) {
        // no parsers and storers
        m.getOntologyParsers().clear();
        m.getOntologyStorers().clear();
        return m;
    }

    public SWRLRule bigRule() {
        return df.getSWRLRule(body2, head2, as);
    }

    public OWLHasKeyAxiom hasKey() {
        Set<OWLPropertyExpression> set = new HashSet<>();
        set.add(df.getOWLObjectProperty(iri));
        set.add(op);
        set.add(dp);
        return df.getOWLHasKeyAxiom(ce, set, as);
    }

    public OWLSymmetricObjectPropertyAxiom symm() {
        return df.getOWLSymmetricObjectPropertyAxiom(op, as);
    }

    public OWLTransitiveObjectPropertyAxiom trans() {
        return df.getOWLTransitiveObjectPropertyAxiom(op, as);
    }

    public SWRLRule rule() {
        return df.getSWRLRule(body, head);
    }

    public OWLSubObjectPropertyOfAxiom subObject() {
        return df.getOWLSubObjectPropertyOfAxiom(op, df.getOWLTopObjectProperty(), as);
    }

    public OWLSubDataPropertyOfAxiom subData() {
        return df.getOWLSubDataPropertyOfAxiom(dp, df.getOWLTopDataProperty());
    }

    public OWLSubClassOfAxiom subClass() {
        return df.getOWLSubClassOfAxiom(ce, df.getOWLThing(), as);
    }

    public OWLSubAnnotationPropertyOfAxiom subAnn() {
        return df.getOWLSubAnnotationPropertyOfAxiom(ap, df.getRDFSLabel(), as);
    }

    public OWLSameIndividualAxiom same() {
        return df.getOWLSameIndividualAxiom(inds, as);
    }

    public OWLReflexiveObjectPropertyAxiom ref() {
        return df.getOWLReflexiveObjectPropertyAxiom(op, as);
    }

    public OWLSubPropertyChainOfAxiom chain() {
        return df.getOWLSubPropertyChainOfAxiom(new ArrayList<>(ops), op, as);
    }

    public OWLObjectPropertyRangeAxiom oRange() {
        return df.getOWLObjectPropertyRangeAxiom(op, ce, as);
    }

    public OWLObjectPropertyDomainAxiom oDom() {
        return df.getOWLObjectPropertyDomainAxiom(op, ce, as);
    }

    public OWLObjectPropertyAssertionAxiom opaInv() {
        return df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectInverseOf(op), i, i, as);
    }

    public OWLObjectPropertyAssertionAxiom opaInvj() {
        return df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectInverseOf(op), i, j, as);
    }

    public OWLObjectPropertyAssertionAxiom opa() {
        return df.getOWLObjectPropertyAssertionAxiom(op, i, i, as);
    }

    public OWLNegativeObjectPropertyAssertionAxiom nop() {
        return df.getOWLNegativeObjectPropertyAssertionAxiom(op, i, i, as);
    }

    public OWLNegativeDataPropertyAssertionAxiom ndp() {
        return df.getOWLNegativeDataPropertyAssertionAxiom(dp, i, lit, as);
    }

    public OWLIrreflexiveObjectPropertyAxiom irr() {
        return df.getOWLIrreflexiveObjectPropertyAxiom(op, as);
    }

    public OWLInverseObjectPropertiesAxiom iop() {
        return df.getOWLInverseObjectPropertiesAxiom(op, op, as);
    }

    public OWLInverseFunctionalObjectPropertyAxiom ifp() {
        return df.getOWLInverseFunctionalObjectPropertyAxiom(op, as);
    }

    public OWLFunctionalObjectPropertyAxiom fop() {
        return df.getOWLFunctionalObjectPropertyAxiom(op, as);
    }

    public OWLFunctionalDataPropertyAxiom fdp() {
        return df.getOWLFunctionalDataPropertyAxiom(dp, as);
    }

    public OWLEquivalentObjectPropertiesAxiom eOp() {
        return df.getOWLEquivalentObjectPropertiesAxiom(ops, as);
    }

    public OWLEquivalentDataPropertiesAxiom eDp() {
        return df.getOWLEquivalentDataPropertiesAxiom(dps, as);
    }

    public OWLEquivalentClassesAxiom ec() {
        return df.getOWLEquivalentClassesAxiom(classes, as);
    }

    public OWLDisjointUnionAxiom du() {
        return df.getOWLDisjointUnionAxiom(ce, classes, as);
    }

    public OWLDisjointObjectPropertiesAxiom dOp() {
        return df.getOWLDisjointObjectPropertiesAxiom(ops, as);
    }

    public OWLDisjointDataPropertiesAxiom dDp() {
        return df.getOWLDisjointDataPropertiesAxiom(dps, as);
    }

    public OWLDisjointClassesAxiom dc() {
        return df.getOWLDisjointClassesAxiom(ce, df.getOWLClass(iri));
    }

    public OWLDifferentIndividualsAxiom assDi() {
        return df.getOWLDifferentIndividualsAxiom(i, df.getOWLNamedIndividual(iri));
    }

    public OWLDeclarationAxiom decI() {
        return df.getOWLDeclarationAxiom(i, as);
    }

    public OWLDeclarationAxiom decAp() {
        return df.getOWLDeclarationAxiom(ap, as);
    }

    public OWLDeclarationAxiom decDt() {
        return df.getOWLDeclarationAxiom(d, as);
    }

    public OWLDeclarationAxiom decDp() {
        return df.getOWLDeclarationAxiom(dp, as);
    }

    public OWLDeclarationAxiom decOp() {
        return df.getOWLDeclarationAxiom(op, as);
    }

    public OWLDeclarationAxiom decC() {
        return df.getOWLDeclarationAxiom(ce, as);
    }

    public OWLDatatypeDefinitionAxiom dDef() {
        return df.getOWLDatatypeDefinitionAxiom(d, df.getDoubleOWLDatatype(), as);
    }

    public OWLDataPropertyRangeAxiom dRange() {
        return df.getOWLDataPropertyRangeAxiom(dp, d, as);
    }

    public OWLDataPropertyDomainAxiom dDom() {
        return df.getOWLDataPropertyDomainAxiom(dp, ce, as);
    }

    public OWLDataPropertyAssertionAxiom assDPlain() {
        return df.getOWLDataPropertyAssertionAxiom(dp, i, plainlit, as);
    }

    public OWLDataPropertyAssertionAxiom assD() {
        return df.getOWLDataPropertyAssertionAxiom(dp, i, lit, as);
    }

    public OWLDataPropertyRangeAxiom dRangeRestrict() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDatatypeMinMaxExclusiveRestriction(5.0D, 6.0D), as);
    }

    public OWLDataPropertyRangeAxiom dNot() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataComplementOf(df.getOWLDataOneOf(lit)), as);
    }

    public OWLDataPropertyRangeAxiom dOneOf() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataOneOf(lit), as);
    }

    public OWLClassAssertionAxiom assDEq() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataExactCardinality(1, dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDMax() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataMaxCardinality(1, dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDMin() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataMinCardinality(1, dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDHas() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataHasValue(dp, lit), i, as);
    }

    public OWLClassAssertionAxiom assDAll() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataAllValuesFrom(dp, d), i, as);
    }

    public OWLClassAssertionAxiom assDSome() {
        return df.getOWLClassAssertionAxiom(df.getOWLDataSomeValuesFrom(dp, d), i, as);
    }

    public OWLClassAssertionAxiom assOneOf() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectOneOf(i), i, as);
    }

    public OWLClassAssertionAxiom assHasSelf() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectHasSelf(op), i, as);
    }

    public OWLClassAssertionAxiom assEq() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectExactCardinality(1, op, ce), i, as);
    }

    public OWLClassAssertionAxiom assMax() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectMaxCardinality(1, op, ce), i, as);
    }

    public OWLClassAssertionAxiom assMin() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectMinCardinality(1, op, ce), i, as);
    }

    public OWLClassAssertionAxiom assHas() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectHasValue(op, i), i, as);
    }

    public OWLClassAssertionAxiom assAll() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectAllValuesFrom(op, ce), i, as);
    }

    public OWLClassAssertionAxiom assSome() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectSomeValuesFrom(op, ce), i, as);
    }

    public OWLClassAssertionAxiom assNotAnon() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(ce), df.getOWLAnonymousIndividual("id"), as);
    }

    public OWLClassAssertionAxiom assNot() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(ce), i, as);
    }

    public OWLDataPropertyRangeAxiom dRangeOr() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataUnionOf(d, df.getOWLDataOneOf(lit)), as);
    }

    public OWLDataPropertyRangeAxiom dRangeAnd() {
        return df.getOWLDataPropertyRangeAxiom(dp, df.getOWLDataIntersectionOf(d, df.getOWLDataOneOf(lit)), as);
    }

    public OWLClassAssertionAxiom assOr() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectUnionOf(classes), i, as);
    }

    public OWLClassAssertionAxiom assAnd() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectIntersectionOf(classes), i, as);
    }

    public OWLClassAssertionAxiom ass() {
        return df.getOWLClassAssertionAxiom(ce, i, as);
    }

    public OWLAnnotationPropertyRangeAxiom annRange() {
        return df.getOWLAnnotationPropertyRangeAxiom(ap, iri, as);
    }

    public OWLAnnotationPropertyDomainAxiom annDom() {
        return df.getOWLAnnotationPropertyDomainAxiom(ap, iri, as);
    }

    public OWLAsymmetricObjectPropertyAxiom asymm() {
        return df.getOWLAsymmetricObjectPropertyAxiom(op, as);
    }

    public OWLAnnotationAssertionAxiom ann() {
        return df.getOWLAnnotationAssertionAxiom(ap, iri, lit, as);
    }


    public OWLClassAssertionAxiom assMinTop() {
        return df.getOWLClassAssertionAxiom(df.getOWLObjectMinCardinality(1, op, df.getOWLThing()), i, as);
    }


    public OWLOntology onto() {
        try {
            return m.createOntology(IRI.create("urn:test#", "test"));
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public List<OWLAxiom> all() {
        return Arrays.asList(ann(), asymm(), annDom(), annRange(), ass(), assAnd(), assOr(), dRangeAnd(), dRangeOr(),
                assNot(), assNotAnon(), assSome(), assAll(), assHas(), assMin(), assMax(), assEq(), assHasSelf(),
                assOneOf(), assDSome(), assDAll(), assDHas(), assDMin(), assDMax(), assDEq(), dOneOf(), dNot(),
                dRangeRestrict(), assD(), assDPlain(), dDom(), dRange(), dDef(), decC(), decOp(), decDp(), decDt(), decAp(),
                decI(), assDi(), dc(), dDp(), dOp(), du(), ec(), eDp(), eOp(), fdp(), fop(), ifp(), iop(), irr(), ndp(),
                nop(), opa(), opaInv(), opaInvj(), oDom(), oRange(), chain(), ref(), same(), subAnn(), subClass(),
                subData(), subObject(), rule(), symm(), trans(), hasKey(), bigRule());
    }
}
