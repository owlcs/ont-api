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

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.OntApiException;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.EscapeUtils;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * An {@code OWLObjectRenderer}
 *
 * @see org.semanticweb.owlapi.util.SimpleRenderer
 * @see PrefixMapping
 */
@ParametersAreNonnullByDefault
public class PrefixMappingRenderer implements OWLObjectVisitor, OWLObjectRenderer {
    protected final PrefixMapping pm;
    protected StringBuilder sb;

    public PrefixMappingRenderer(PrefixMapping pm) {
        this.pm = pm;
        reset();
    }

    public void reset() {
        sb = new StringBuilder();
    }

    protected String shortForm(OWLEntity entity) {
        return shortForm(entity.getIRI());
    }

    protected String shortForm(IRI iri) {
        return shortForm(iri.getIRIString());
    }

    protected String shortForm(String uri) {
        String res = pm.shortForm(uri);
        return uri.equals(res) ? String.format("<%s>", uri) : res;
    }

    @Override
    public void setShortFormProvider(ShortFormProvider provider) {
        throw new OntApiException.Unsupported();
    }

    @Override
    public String render(OWLObject object) {
        reset();
        object.accept(this);
        return sb.toString();
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    protected void render(Stream<? extends OWLObject> objects) {
        render(objects.iterator());
    }

    protected void render(Iterator<? extends OWLObject> objects) {
        while (objects.hasNext()) {
            objects.next().accept(this);
            if (objects.hasNext()) {
                space();
            }
        }
    }

    protected void writeAnnotations(HasAnnotations axiom) {
        axiom.annotations().forEach(a -> {
            a.accept(this);
            space();
        });
    }

    protected void space() {
        sb.append(' ');
    }

    protected void begin(String topic) {
        sb.append(topic);
        begin();
    }

    protected void begin() {
        sb.append('(');
    }

    protected void end() {
        sb.append(')');
    }

    @Override
    public void visit(OWLOntology ontology) {
        begin("Ontology");
        sb.append(ontology.getOntologyID());
        space();
        sb.append("[Axioms: ").append(ontology.getAxiomCount()).append("]");
        space();
        sb.append("[Logical axioms: ").append(ontology.getLogicalAxiomCount()).append("]");
        end();
    }

    @Override
    public void visit(IRI iri) {
        sb.append(shortForm(iri));
    }

    @Override
    public void visit(OWLLiteral literal) {
        String txt = EscapeUtils.escapeString(literal.getLiteral());
        sb.append('"').append(txt).append('"');
        OWLDatatype dt = literal.getDatatype();
        if (dt.isRDFPlainLiteral() || OWL2Datatype.RDF_LANG_STRING.getIRI().equals(dt.getIRI())) {
            if (literal.hasLang()) {
                sb.append('@').append(literal.getLang());
            }
        } else if (!dt.isString()) {
            sb.append("^^");
            dt.accept(this);
        }
    }

    @Override
    public void visit(OWLAnonymousIndividual individual) {
        sb.append(individual.getID());
    }

    @Override
    public void visit(OWLClass clazz) {
        sb.append(shortForm(clazz));
    }

    @Override
    public void visit(OWLDatatype datatype) {
        sb.append(shortForm(datatype));
    }

    @Override
    public void visit(OWLDataProperty property) {
        sb.append(shortForm(property));
    }

    @Override
    public void visit(OWLObjectProperty property) {
        sb.append(shortForm(property));
    }

    @Override
    public void visit(OWLAnnotationProperty property) {
        sb.append(shortForm(property));
    }

    @Override
    public void visit(OWLNamedIndividual individual) {
        sb.append(shortForm(individual));
    }

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        begin("SubClassOf");
        writeAnnotations(axiom);
        axiom.getSubClass().accept(this);
        space();
        axiom.getSuperClass().accept(this);
        end();
    }

    @Override
    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        begin("NegativeObjectPropertyAssertion");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getSubject().accept(this);
        space();
        axiom.getObject().accept(this);
        end();
    }

    @Override
    public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        begin("AsymmetricObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        begin("ReflexiveObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLDisjointClassesAxiom axiom) {
        begin("DisjointClasses");
        writeAnnotations(axiom);
        render(axiom.classExpressions());
        end();
    }

    @Override
    public void visit(OWLDataPropertyDomainAxiom axiom) {
        begin("DataPropertyDomain");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getDomain().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        begin("ObjectPropertyDomain");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getDomain().accept(this);
        end();
    }

    @Override
    public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        begin("EquivalentObjectProperties");
        writeAnnotations(axiom);
        render(axiom.properties());
        end();
    }

    @Override
    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        begin("NegativeDataPropertyAssertion");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getSubject().accept(this);
        space();
        axiom.getObject().accept(this);
        end();
    }

    @Override
    public void visit(OWLDifferentIndividualsAxiom axiom) {
        begin("DifferentIndividuals");
        writeAnnotations(axiom);
        render(axiom.individuals());
        end();
    }

    @Override
    public void visit(OWLDisjointDataPropertiesAxiom axiom) {
        begin("DisjointDataProperties");
        writeAnnotations(axiom);
        render(axiom.properties());
        end();
    }

    @Override
    public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
        begin("DisjointObjectProperties");
        writeAnnotations(axiom);
        render(axiom.properties());
        end();
    }

    @Override
    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        begin("ObjectPropertyRange");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getRange().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        begin("ObjectPropertyAssertion");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getSubject().accept(this);
        space();
        axiom.getObject().accept(this);
        end();
    }

    @Override
    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        begin("FunctionalObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLSubObjectPropertyOfAxiom axiom) {
        begin("SubObjectPropertyOf");
        writeAnnotations(axiom);
        axiom.getSubProperty().accept(this);
        space();
        axiom.getSuperProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLDisjointUnionAxiom axiom) {
        begin("DisjointUnion");
        writeAnnotations(axiom);
        axiom.getOWLClass().accept(this);
        space();
        render(axiom.classExpressions());
        end();
    }

    @Override
    public void visit(OWLDeclarationAxiom axiom) {
        begin("Declaration");
        writeAnnotations(axiom);
        visit(axiom.getEntity());
        end();
    }

    public void visit(OWLEntity entity) {
        begin(entity.getEntityType().getName());
        entity.accept(this);
        end();
    }

    @Override
    public void visit(OWLAnnotationAssertionAxiom axiom) {
        begin("AnnotationAssertion");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getSubject().accept(this);
        space();
        axiom.getValue().accept(this);
        end();
    }

    @Override
    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        begin("SymmetricObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataPropertyRangeAxiom axiom) {
        begin("DataPropertyRange");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getRange().accept(this);
        end();
    }

    @Override
    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        begin("FunctionalDataProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
        begin("EquivalentDataProperties");
        writeAnnotations(axiom);
        render(axiom.properties());
        end();
    }

    @Override
    public void visit(OWLClassAssertionAxiom axiom) {
        begin("ClassAssertion");
        writeAnnotations(axiom);
        axiom.getClassExpression().accept(this);
        space();
        axiom.getIndividual().accept(this);
        end();
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        begin("EquivalentClasses");
        writeAnnotations(axiom);
        render(axiom.classExpressions());
        end();
    }

    @Override
    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        begin("DataPropertyAssertion");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        space();
        axiom.getSubject().accept(this);
        space();
        axiom.getObject().accept(this);
        end();
    }

    @Override
    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        begin("TransitiveObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        begin("IrreflexiveObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLSubDataPropertyOfAxiom axiom) {
        begin("SubDataPropertyOf");
        writeAnnotations(axiom);
        axiom.getSubProperty().accept(this);
        space();
        axiom.getSuperProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        begin("InverseFunctionalObjectProperty");
        writeAnnotations(axiom);
        axiom.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLSameIndividualAxiom axiom) {
        begin("SameIndividual");
        writeAnnotations(axiom);
        render(axiom.individuals());
        end();
    }

    @Override
    public void visit(OWLSubPropertyChainOfAxiom axiom) {
        begin("SubObjectPropertyOf");
        writeAnnotations(axiom);
        begin("ObjectPropertyChain");
        render(axiom.getPropertyChain().iterator());
        end();
        space();
        axiom.getSuperProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectIntersectionOf ce) {
        begin("ObjectIntersectionOf");
        render(ce.operands());
        end();
    }

    @Override
    public void visit(OWLObjectUnionOf ce) {
        begin("ObjectUnionOf");
        render(ce.operands());
        end();
    }

    @Override
    public void visit(OWLObjectComplementOf ce) {
        begin("ObjectComplementOf");
        ce.getOperand().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectSomeValuesFrom ce) {
        begin("ObjectSomeValuesFrom");
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectAllValuesFrom ce) {
        begin("ObjectAllValuesFrom");
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectHasValue ce) {
        begin("ObjectHasValue");
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectMinCardinality ce) {
        begin("ObjectMinCardinality");
        sb.append(ce.getCardinality());
        space();
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectExactCardinality ce) {
        begin("ObjectExactCardinality");
        sb.append(ce.getCardinality());
        space();
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectMaxCardinality ce) {
        begin("ObjectMaxCardinality");
        sb.append(ce.getCardinality());
        space();
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectHasSelf ce) {
        begin("ObjectHasSelf");
        ce.getProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectOneOf ce) {
        begin("ObjectOneOf");
        render(ce.individuals());
        end();
    }

    @Override
    public void visit(OWLDataSomeValuesFrom ce) {
        begin("DataSomeValuesFrom");
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataAllValuesFrom ce) {
        begin("DataAllValuesFrom");
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataHasValue ce) {
        begin("DataHasValue");
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataMinCardinality ce) {
        begin("DataMinCardinality");
        sb.append(ce.getCardinality());
        space();
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataExactCardinality ce) {
        begin("DataExactCardinality");
        sb.append(ce.getCardinality());
        space();
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataMaxCardinality ce) {
        begin("DataMaxCardinality");
        sb.append(ce.getCardinality());
        space();
        ce.getProperty().accept(this);
        space();
        ce.getFiller().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataComplementOf node) {
        begin("DataComplementOf");
        node.getDataRange().accept(this);
        end();
    }

    @Override
    public void visit(OWLDataOneOf node) {
        begin("DataOneOf");
        render(node.values());
        end();
    }

    @Override
    public void visit(OWLDatatypeRestriction node) {
        begin("DataRangeRestriction");
        node.getDatatype().accept(this);
        node.facetRestrictions().forEach(r -> {
            space();
            r.accept(this);
        });
        end();
    }

    @Override
    public void visit(OWLFacetRestriction node) {
        begin("facetRestriction");
        sb.append(node.getFacet());
        space();
        node.getFacetValue().accept(this);
        end();
    }

    @Override
    public void visit(OWLObjectInverseOf property) {
        begin("ObjectInverseOf");
        property.getInverse().accept(this);
        end();
    }

    @Override
    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        begin("InverseObjectProperties");
        writeAnnotations(axiom);
        axiom.getFirstProperty().accept(this);
        space();
        axiom.getSecondProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLHasKeyAxiom axiom) {
        begin("HasKey");
        writeAnnotations(axiom);
        axiom.getClassExpression().accept(this);
        space();
        begin();
        render(axiom.objectPropertyExpressions());
        end();
        space();
        begin();
        render(axiom.dataPropertyExpressions());
        end();
        end();
    }

    @Override
    public void visit(OWLDataIntersectionOf node) {
        begin("DataIntersectionOf");
        render(node.operands());
        end();
    }

    @Override
    public void visit(OWLDataUnionOf node) {
        begin("DataUnionOf");
        render(node.operands());
        end();
    }

    @Override
    public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
        begin("AnnotationPropertyDomain");
        axiom.getProperty().accept(this);
        space();
        axiom.getDomain().accept(this);
        end();
    }

    @Override
    public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
        begin("AnnotationPropertyRange");
        axiom.getProperty().accept(this);
        space();
        axiom.getRange().accept(this);
        end();
    }

    @Override
    public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        begin("SubAnnotationPropertyOf");
        writeAnnotations(axiom);
        axiom.getSubProperty().accept(this);
        space();
        axiom.getSuperProperty().accept(this);
        end();
    }

    @Override
    public void visit(OWLAnnotation node) {
        begin("Annotation");
        writeAnnotations(node);
        node.getProperty().accept(this);
        space();
        node.getValue().accept(this);
        end();
    }

    @Override
    public void visit(SWRLRule rule) {
        begin("DLSafeRule");
        writeAnnotations(rule);
        begin("Body");
        render(rule.body());
        end();
        space();
        begin("Head");
        render(rule.head());
        end();
        end();
    }

    @Override
    public void visit(SWRLClassAtom node) {
        begin("ClassAtom");
        node.getPredicate().accept(this);
        space();
        node.getArgument().accept(this);
        end();
    }

    @Override
    public void visit(SWRLDataRangeAtom node) {
        begin("DataRangeAtom");
        node.getPredicate().accept(this);
        space();
        node.getArgument().accept(this);
        end();
    }

    @Override
    public void visit(SWRLDifferentIndividualsAtom node) {
        begin("DifferentFromAtom");
        node.getFirstArgument().accept(this);
        space();
        node.getSecondArgument().accept(this);
        end();
    }

    @Override
    public void visit(SWRLSameIndividualAtom node) {
        begin("SameAsAtom");
        node.getFirstArgument().accept(this);
        space();
        node.getSecondArgument().accept(this);
        end();
    }

    @Override
    public void visit(SWRLObjectPropertyAtom node) {
        begin("ObjectPropertyAtom");
        node.getPredicate().accept(this);
        space();
        node.getFirstArgument().accept(this);
        space();
        node.getSecondArgument().accept(this);
        end();
    }

    @Override
    public void visit(SWRLDataPropertyAtom node) {
        begin("DataPropertyAtom");
        node.getPredicate().accept(this);
        space();
        node.getFirstArgument().accept(this);
        space();
        node.getSecondArgument().accept(this);
        end();
    }

    @Override
    public void visit(SWRLBuiltInAtom node) {
        begin("BuiltInAtom");
        node.getPredicate().accept(this);
        space();
        render(node.getArguments().iterator());
        end();
    }

    @Override
    public void visit(OWLDatatypeDefinitionAxiom axiom) {
        begin("DatatypeDefinition");
        writeAnnotations(axiom);
        axiom.getDatatype().accept(this);
        space();
        axiom.getDataRange().accept(this);
        end();
    }

    @Override
    public void visit(SWRLVariable node) {
        begin("Variable");
        node.getIRI().accept(this);
        end();
    }

    @Override
    public void visit(SWRLIndividualArgument node) {
        node.getIndividual().accept(this);
    }

    @Override
    public void visit(SWRLLiteralArgument node) {
        node.getLiteral().accept(this);
    }

}
