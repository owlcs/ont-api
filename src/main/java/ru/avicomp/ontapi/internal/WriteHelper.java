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

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.owlapi.objects.OWLAnonymousIndividualImpl;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper for the axioms translation to the rdf-form (writing to graph).
 * <p>
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2 Mapping from the Structural Specification to RDF Graphs</a>
 * for handling common graph triples (operator 'T') see chapter "2.1 Translation of Axioms without Annotations"
 * for handling annotations (operator 'TANN') see chapters "2.2 Translation of Annotations" and "2.3 Translation of Axioms with Annotations".
 * <p>
 * Created by @szuev on 28.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class WriteHelper {

    public static RDFNode toRDFNode(OWLObject object) {
        if (object instanceof OWLLiteral) {
            return toLiteral((OWLLiteral) object);
        }
        return toResource(object);
    }

    public static Optional<Resource> resource(OWLObject o) throws OntApiException {
        return o == null ? Optional.empty() : Optional.of(toResource(o));
    }

    public static Resource toResource(OWLObject object) throws OntApiException {
        if (object instanceof OWLIndividual) {
            return toResource((OWLIndividual) object);
        }
        return toResource(toIRI(object));
    }

    private static Resource toResource(OWLIndividual individual) {
        return individual.isAnonymous() ? toResource(individual.asOWLAnonymousIndividual()) : toResource(individual.asOWLNamedIndividual().getIRI());
    }

    public static Resource toResource(OWLAnonymousIndividual individual) {
        return new ResourceImpl(toBlankNode(individual), null);
    }

    public static Node toBlankNode(OWLAnonymousIndividual individual) {
        BlankNodeId id = OWLAnonymousIndividualImpl.asONT(individual).getBlankNodeId();
        return NodeFactory.createBlankNode(id);
    }

    private static Resource toResource(IRI iri) {
        return ResourceFactory.createResource(OntApiException.notNull(iri, "Null iri").getIRIString());
    }

    public static Property toProperty(OWLPropertyExpression object) {
        return toProperty(toIRI(object));
    }

    private static Property toProperty(IRI iri) {
        return ResourceFactory.createProperty(OntApiException.notNull(iri, "Null iri").getIRIString());
    }

    public static Literal toLiteral(OWLLiteral literal) {
        return new LiteralImpl(toLiteralNode(literal), null);
    }

    public static Node toLiteralNode(OWLLiteral literal) {
        LiteralLabel lab = OWLLiteralImpl.asONT(literal).getLiteralLabel();
        return NodeFactory.createLiteral(lab);
    }

    public static Resource getType(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return OWL.Class;
        } else if (entity.isOWLDataProperty()) {
            return OWL.DatatypeProperty;
        } else if (entity.isOWLObjectProperty()) {
            return OWL.ObjectProperty;
        } else if (entity.isOWLNamedIndividual()) {
            return OWL.NamedIndividual;
        } else if (entity.isOWLAnnotationProperty()) {
            return OWL.AnnotationProperty;
        } else if (entity.isOWLDatatype()) {
            return RDFS.Datatype;
        }
        throw new OntApiException("Unsupported " + entity);
    }

    public static Class<? extends OntEntity> getEntityView(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return OntClass.class;
        } else if (entity.isOWLDataProperty()) {
            return OntNDP.class;
        } else if (entity.isOWLObjectProperty()) {
            return OntNOP.class;
        } else if (entity.isOWLNamedIndividual()) {
            return OntIndividual.Named.class;
        } else if (entity.isOWLAnnotationProperty()) {
            return OntNAP.class;
        } else if (entity.isOWLDatatype()) {
            return OntDT.class;
        }
        throw new OntApiException("Unsupported " + entity);
    }

    public static Class<? extends OntFR> getFRView(OWLFacet facet) {
        switch (facet) {
            case LENGTH:
                return OntFR.Length.class;
            case MIN_LENGTH:
                return OntFR.MinLength.class;
            case MAX_LENGTH:
                return OntFR.MaxLength.class;
            case MIN_INCLUSIVE:
                return OntFR.MinInclusive.class;
            case MAX_INCLUSIVE:
                return OntFR.MaxInclusive.class;
            case MIN_EXCLUSIVE:
                return OntFR.MinExclusive.class;
            case MAX_EXCLUSIVE:
                return OntFR.MaxExclusive.class;
            case PATTERN:
                return OntFR.Pattern.class;
            case FRACTION_DIGITS:
                return OntFR.FractionDigits.class;
            case TOTAL_DIGITS:
                return OntFR.TotalDigits.class;
            case LANG_RANGE:
                return OntFR.LangRange.class;
        }
        throw new OntApiException("Unsupported " + facet);
    }

    public static void writeAssertionTriple(OntGraphModel model,
                                            OWLObject subject,
                                            OWLPropertyExpression property,
                                            OWLObject object,
                                            Stream<OWLAnnotation> annotations) {
        OntObject s = addRDFNode(model, subject).as(OntObject.class);
        Property p = addRDFNode(model, property).as(Property.class);
        RDFNode o = addRDFNode(model, object);
        addAnnotations(s.addStatement(p, o), annotations);
    }

    public static void writeDeclarationTriple(OntGraphModel model,
                                              OWLEntity subject,
                                              Property predicate,
                                              RDFNode object,
                                              Stream<OWLAnnotation> annotations) {
        addAnnotations(toResource(subject)
                .inModel(model).addProperty(predicate, object).as(getEntityView(subject)).getRoot(), annotations);
    }

    public static void writeTriple(OntGraphModel model,
                                   OWLObject subject,
                                   Property predicate,
                                   OWLObject object,
                                   Stream<OWLAnnotation> annotations) {
        writeTriple(model, subject, predicate, addRDFNode(model, object), annotations);
    }

    public static void writeTriple(OntGraphModel model,
                                   OWLObject subject,
                                   Property predicate,
                                   RDFNode object,
                                   Stream<OWLAnnotation> annotations) {
        OntObject s = addRDFNode(model, subject).as(OntObject.class);
        addAnnotations(s.addStatement(predicate, object), annotations);
    }

    public static void writeList(OntGraphModel model,
                                 OWLObject subject,
                                 Property predicate,
                                 Stream<? extends OWLObject> objects,
                                 Stream<OWLAnnotation> annotations) {
        OntObject s = addRDFNode(model, subject).as(OntObject.class);
        addAnnotations(s.addStatement(predicate, addRDFList(model, objects)), annotations);
    }

    public static RDFList addRDFList(OntGraphModel model, Stream<? extends OWLObject> objects) {
        return model.createList(objects.map(o -> addRDFNode(model, o)).iterator());
    }

    public static OntNAP addAnnotationProperty(OntGraphModel model, OWLEntity entity) {
        String uri = entity.getIRI().getIRIString();
        return model.fetchOntEntity(OntNAP.class, uri);
    }

    public static OntOPE addObjectProperty(OntGraphModel model, OWLObjectPropertyExpression ope) {
        if (!ope.isOWLObjectProperty()) {
            return addInverseOf(model, (OWLObjectInverseOf) ope);
        }
        return model.fetchOntEntity(OntNOP.class, ope.getNamedProperty().getIRI().getIRIString());
    }

    public static OntNDP addDataProperty(OntGraphModel model, OWLDataPropertyExpression dpe) {
        if (!dpe.isOWLDataProperty()) throw new OntApiException("Unsupported " + dpe);
        String uri = dpe.asOWLDataProperty().getIRI().getIRIString();
        return model.fetchOntEntity(OntNDP.class, uri);
    }

    public static OntEntity addOntEntity(OntGraphModel model, OWLEntity entity) {
        Class<? extends OntEntity> view = getEntityView(entity);
        String uri = entity.getIRI().getIRIString();
        return model.fetchOntEntity(view, uri);
    }

    public static OntOPE.Inverse addInverseOf(OntGraphModel model, OWLObjectInverseOf io) {
        String uri = io.getInverseProperty().getNamedProperty().getIRI().getIRIString();
        return model.fetchOntEntity(OntNOP.class, uri).createInverse();
    }

    public static OntFR addFacetRestriction(OntGraphModel model, OWLFacetRestriction fr) {
        return model.createFacetRestriction(getFRView(fr.getFacet()), addLiteral(model, fr.getFacetValue()));
    }

    public static OntCE addClassExpression(OntGraphModel model, OWLClassExpression ce) {
        if (ce.isOWLClass()) {
            return addOntEntity(model, ce.asOWLClass()).as(OntClass.class);
        }
        ClassExpressionType type = ce.getClassExpressionType();
        CETranslator cet = OntApiException.notNull(CETranslator.valueOf(type), "Unsupported class-expression " + ce + "/" + type);
        return cet.translator.add(model, ce).as(OntCE.class);
    }

    public static OntDR addDataRange(OntGraphModel model, OWLDataRange dr) {
        if (dr.isOWLDatatype()) {
            return addOntEntity(model, dr.asOWLDatatype()).as(OntDT.class);
        }
        DataRangeType type = dr.getDataRangeType();
        DRTranslator drt = OntApiException.notNull(DRTranslator.valueOf(type), "Unsupported data-range expression " + dr + "/" + type);
        return drt.translator.add(model, dr).as(OntDR.class);
    }

    public static OntIndividual.Anonymous getAnonymousIndividual(OntGraphModel model, OWLAnonymousIndividual ai) {
        Resource res = toResource(OntApiException.notNull(ai, "Null anonymous individual.")).inModel(model);
        if (!res.canAs(OntIndividual.Anonymous.class)) {
            //throw new OntApiException("Anonymous individual should be created first: " + ai + ".");
            return Models.asAnonymousIndividual(res);
        }
        return res.as(OntIndividual.Anonymous.class);
    }

    public static OntIndividual addIndividual(OntGraphModel model, OWLIndividual i) {
        if (i.isAnonymous()) return getAnonymousIndividual(model, i.asOWLAnonymousIndividual());
        String uri = i.asOWLNamedIndividual().getIRI().getIRIString();
        return model.fetchOntEntity(OntIndividual.Named.class, uri);
    }

    /**
     * the main method to add OWLObject as RDFNode to the specified model.
     *
     * @param model {@link OntGraphModel}
     * @param o     {@link OWLObject}
     * @return {@link RDFNode} node, attached to the model.
     */
    public static RDFNode addRDFNode(OntGraphModel model, OWLObject o) {
        if (o instanceof OWLEntity) {
            return addOntEntity(model, (OWLEntity) o);
        }
        if (o instanceof OWLLiteral) {
            return addLiteral(model, (OWLLiteral) o);
        }
        if (o instanceof OWLObjectInverseOf) {
            return addInverseOf(model, (OWLObjectInverseOf) o);
        }
        if (o instanceof OWLFacetRestriction) {
            return addFacetRestriction(model, (OWLFacetRestriction) o);
        }
        if (o instanceof OWLClassExpression) {
            return addClassExpression(model, (OWLClassExpression) o);
        }
        if (o instanceof OWLDataRange) {
            return addDataRange(model, (OWLDataRange) o);
        }
        if (o instanceof OWLAnonymousIndividual) {
            return getAnonymousIndividual(model, (OWLAnonymousIndividual) o);
        }
        if (o instanceof SWRLObject) {
            return addSWRLObject(model, (SWRLObject) o);
        }
        return toRDFNode(o).inModel(model);
    }

    public static OntSWRL.Variable addSWRLVariable(OntGraphModel model, SWRLVariable var) {
        return model.createSWRLVariable(var.getIRI().getIRIString());
    }

    public static OntSWRL.Atom addSWRLAtom(OntGraphModel model, SWRLAtom atom) {
        SWRLAtomTranslator swrlt = OntApiException.notNull(SWRLAtomTranslator.valueOf(atom), "Unsupported swrl-atom " + atom);
        return swrlt.translator.add(model, atom).as(OntSWRL.Atom.class);
    }

    public static RDFNode addSWRLObject(OntGraphModel model, SWRLObject o) {
        if (o instanceof SWRLAtom) {
            return addSWRLAtom(model, (SWRLAtom) o);
        } else if (o instanceof SWRLArgument) {
            if (o instanceof SWRLVariable) {
                return addSWRLVariable(model, (SWRLVariable) o);
            }
            if (o instanceof SWRLLiteralArgument) {
                return addRDFNode(model, ((SWRLLiteralArgument) o).getLiteral());
            }
            if (o instanceof SWRLIndividualArgument) {
                return addRDFNode(model, ((SWRLIndividualArgument) o).getIndividual());
            }
        }
        throw new OntApiException("Unsupported SWRL-Object: " + o);
    }

    /**
     * pass annotations from owl-api to ont-api.
     *
     * @param statement   {@link OntStatement}
     * @param annotations Stream of {@link OWLAnnotation}'s
     */
    public static void addAnnotations(OntStatement statement, Stream<OWLAnnotation> annotations) {
        annotations.forEach(a -> {
            OntStatement st = statement.addAnnotation(addAnnotationProperty(statement.getModel(), a.getProperty()), addRDFNode(statement.getModel(), a.getValue()));
            addAnnotations(st, a.annotations());
        });
    }

    public static void addAnnotations(OntObject object, Stream<OWLAnnotation> annotations) {
        addAnnotations(OntApiException.notNull(object.getRoot(), "Can't determine root statement for " + object), annotations);
    }

    public static IRI toIRI(OWLObject object) {
        if (OntApiException.notNull(object, "Null owl-object specified.").isIRI()) return (IRI) object;
        if (object instanceof HasIRI) {
            return ((HasIRI) object).getIRI();
        }
        if (object instanceof OWLAnnotationObject) {
            return ((OWLAnnotationObject) object).asIRI().orElseThrow(() -> new OntApiException("Not iri: " + object));
        }
        if (object instanceof OWLClassExpression) {
            return toIRI((OWLClassExpression) object);
        }
        if (object instanceof OWLPropertyExpression) {
            return toIRI((OWLPropertyExpression) object);
        }
        throw new OntApiException("Unsupported owl-object: " + object);
    }

    private static IRI toIRI(OWLClassExpression expression) {
        HasIRI res = null;
        if (ClassExpressionType.OWL_CLASS.equals(expression.getClassExpressionType())) {
            res = (OWLClass) expression;
        }
        return OntApiException.notNull(res, "Unsupported class-expression: " + expression).getIRI();
    }

    private static IRI toIRI(OWLPropertyExpression expression) {
        if (expression.isOWLDataProperty())
            return expression.asOWLDataProperty().getIRI();
        if (expression.isOWLObjectProperty())
            return expression.asOWLObjectProperty().getIRI();
        if (expression.isOWLAnnotationProperty()) {
            return expression.asOWLAnnotationProperty().getIRI();
        }
        throw new OntApiException("Unsupported property-expression: " + expression);
    }

    public static Literal addLiteral(OntGraphModel model, OWLLiteral literal) {
        addDataRange(model, literal.getDatatype()).as(OntDT.class);
        return model.asRDFNode(toLiteralNode(literal)).asLiteral();
    }

    /**
     * for SWRLAtom
     */
    private enum SWRLAtomTranslator {
        BUILT_IN(SWRLBuiltInAtom.class, new Translator<SWRLBuiltInAtom, OntSWRL.Atom.BuiltIn>() {
            @Override
            OntSWRL.Atom.BuiltIn translate(OntGraphModel model, SWRLBuiltInAtom atom) {
                return model.createBuiltInSWRLAtom(model.createResource(atom.getPredicate().getIRIString()),
                        atom.arguments().map(a -> addSWRLObject(model, a).as(OntSWRL.DArg.class)).collect(Collectors.toList()));
            }
        }),
        OWL_CLASS(SWRLClassAtom.class, new Translator<SWRLClassAtom, OntSWRL.Atom.OntClass>() {
            @Override
            OntSWRL.Atom.OntClass translate(OntGraphModel model, SWRLClassAtom atom) {
                return model.createClassSWRLAtom(addClassExpression(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getArgument()).as(OntSWRL.IArg.class));
            }
        }),
        DATA_PROPERTY(SWRLDataPropertyAtom.class, new Translator<SWRLDataPropertyAtom, OntSWRL.Atom.DataProperty>() {
            @Override
            OntSWRL.Atom.DataProperty translate(OntGraphModel model, SWRLDataPropertyAtom atom) {
                return model.createDataPropertySWRLAtom(addDataProperty(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.DArg.class));
            }
        }),
        DATA_RANGE(SWRLDataRangeAtom.class, new Translator<SWRLDataRangeAtom, OntSWRL.Atom.DataRange>() {
            @Override
            OntSWRL.Atom.DataRange translate(OntGraphModel model, SWRLDataRangeAtom atom) {
                return model.createDataRangeSWRLAtom(addDataRange(model, atom.getPredicate()), addSWRLObject(model, atom.getArgument()).as(OntSWRL.DArg.class));
            }
        }),
        DIFFERENT_INDIVIDUALS(SWRLDifferentIndividualsAtom.class, new Translator<SWRLDifferentIndividualsAtom, OntSWRL.Atom.DifferentIndividuals>() {
            @Override
            OntSWRL.Atom.DifferentIndividuals translate(OntGraphModel model, SWRLDifferentIndividualsAtom atom) {
                return model.createDifferentIndividualsSWRLAtom(addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        OBJECT_PROPERTY(SWRLObjectPropertyAtom.class, new Translator<SWRLObjectPropertyAtom, OntSWRL.Atom.ObjectProperty>() {
            @Override
            OntSWRL.Atom.ObjectProperty translate(OntGraphModel model, SWRLObjectPropertyAtom atom) {
                return model.createObjectPropertySWRLAtom(addObjectProperty(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        SAME_INDIVIDUALS(SWRLSameIndividualAtom.class, new Translator<SWRLSameIndividualAtom, OntSWRL.Atom.SameIndividuals>() {
            @Override
            OntSWRL.Atom.SameIndividuals translate(OntGraphModel model, SWRLSameIndividualAtom atom) {
                return model.createSameIndividualsSWRLAtom(addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        ;

        private final Translator<? extends SWRLAtom, ? extends OntSWRL.Atom> translator;
        private final Class<? extends SWRLAtom> type;

        SWRLAtomTranslator(Class<? extends SWRLAtom> type, Translator<? extends SWRLAtom, ? extends OntSWRL.Atom> translator) {
            this.translator = translator;
            this.type = type;
        }

        private static SWRLAtomTranslator valueOf(SWRLAtom atom) {
            for (SWRLAtomTranslator t : values()) {
                if (t.type.isInstance(atom)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends SWRLAtom, TO extends OntSWRL.Atom> {
            @SuppressWarnings("unchecked")
            private Resource add(OntGraphModel model, SWRLAtom atom) {
                return translate(model, (FROM) atom);
            }

            abstract TO translate(OntGraphModel model, FROM atom);
        }
    }

    /**
     * Data Range translator
     */
    private enum DRTranslator {
        ONE_OF(DataRangeType.DATA_ONE_OF, new Translator<OWLDataOneOf, OntDR.OneOf>() {
            @Override
            OntDR.OneOf translate(OntGraphModel model, OWLDataOneOf expression) {
                return model.createOneOfDataRange(expression.values().map(l -> addLiteral(model, l)).collect(Collectors.toList()));
            }
        }),
        RESTRICTION(DataRangeType.DATATYPE_RESTRICTION, new Translator<OWLDatatypeRestriction, OntDR.Restriction>() {
            @Override
            OntDR.Restriction translate(OntGraphModel model, OWLDatatypeRestriction expression) {
                return model.createRestrictionDataRange(addRDFNode(model, expression.getDatatype()).as(OntDT.class),
                        expression.facetRestrictions().map(f -> addFacetRestriction(model, f)).collect(Collectors.toList()));
            }
        }),
        COMPLEMENT_OF(DataRangeType.DATA_COMPLEMENT_OF, new Translator<OWLDataComplementOf, OntDR.ComplementOf>() {
            @Override
            OntDR.ComplementOf translate(OntGraphModel model, OWLDataComplementOf expression) {
                return model.createComplementOfDataRange(addRDFNode(model, expression.getDataRange()).as(OntDR.class));
            }
        }),
        UNION_OF(DataRangeType.DATA_UNION_OF, new Translator<OWLDataUnionOf, OntDR.UnionOf>() {
            @Override
            OntDR.UnionOf translate(OntGraphModel model, OWLDataUnionOf expression) {
                return model.createUnionOfDataRange(expression.operands().map(dr -> addRDFNode(model, dr).as(OntDR.class)).collect(Collectors.toList()));
            }
        }),
        INTERSECTION_OF(DataRangeType.DATA_INTERSECTION_OF, new Translator<OWLDataIntersectionOf, OntDR.IntersectionOf>() {
            @Override
            OntDR.IntersectionOf translate(OntGraphModel model, OWLDataIntersectionOf expression) {
                return model.createIntersectionOfDataRange(expression.operands().map(dr -> addRDFNode(model, dr).as(OntDR.class)).collect(Collectors.toList()));
            }
        }),
        ;
        private final DataRangeType type;
        private final Translator<? extends OWLDataRange, ? extends OntDR> translator;

        DRTranslator(DataRangeType type, Translator<? extends OWLDataRange, ? extends OntDR> translator) {
            this.translator = translator;
            this.type = type;
        }

        public static DRTranslator valueOf(DataRangeType type) {
            for (DRTranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends OWLDataRange, TO extends OntDR> {
            @SuppressWarnings("unchecked")
            private Resource add(OntGraphModel model, OWLDataRange expression) {
                return translate(model, (FROM) expression);
            }

            abstract TO translate(OntGraphModel model, FROM expression);
        }
    }

    /**
     * Class Expression translator
     */
    private enum CETranslator {
        OBJECT_MAX_CARDINALITY(ClassExpressionType.OBJECT_MAX_CARDINALITY, new Translator<OWLObjectMaxCardinality, OntCE.ObjectMaxCardinality>() {
            @Override
            OntCE.ObjectMaxCardinality translate(OntGraphModel model, OWLObjectMaxCardinality expression) {
                OntOPE p = addObjectProperty(model, expression.getProperty());
                OntCE c = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectMaxCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_MAX_CARDINALITY(ClassExpressionType.DATA_MAX_CARDINALITY, new Translator<OWLDataMaxCardinality, OntCE.DataMaxCardinality>() {
            @Override
            OntCE.DataMaxCardinality translate(OntGraphModel model, OWLDataMaxCardinality expression) {
                OntNDP p = addDataProperty(model, expression.getProperty());
                OntDR d = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataMaxCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_MIN_CARDINALITY(ClassExpressionType.OBJECT_MIN_CARDINALITY, new Translator<OWLObjectMinCardinality, OntCE.ObjectMinCardinality>() {
            @Override
            OntCE.ObjectMinCardinality translate(OntGraphModel model, OWLObjectMinCardinality expression) {
                OntOPE p = addObjectProperty(model, expression.getProperty());
                OntCE c = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectMinCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_MIN_CARDINALITY(ClassExpressionType.DATA_MIN_CARDINALITY, new Translator<OWLDataMinCardinality, OntCE.DataMinCardinality>() {
            @Override
            OntCE.DataMinCardinality translate(OntGraphModel model, OWLDataMinCardinality expression) {
                OntNDP p = addDataProperty(model, expression.getProperty());
                OntDR d = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataMinCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_EXACT_CARDINALITY(ClassExpressionType.OBJECT_EXACT_CARDINALITY, new Translator<OWLObjectExactCardinality, OntCE.ObjectCardinality>() {
            @Override
            OntCE.ObjectCardinality translate(OntGraphModel model, OWLObjectExactCardinality expression) {
                OntOPE p = addObjectProperty(model, expression.getProperty());
                OntCE c = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_EXACT_CARDINALITY(ClassExpressionType.DATA_EXACT_CARDINALITY, new Translator<OWLDataExactCardinality, OntCE.DataCardinality>() {
            @Override
            OntCE.DataCardinality translate(OntGraphModel model, OWLDataExactCardinality expression) {
                OntNDP p = addDataProperty(model, expression.getProperty());
                OntDR d = expression.getFiller() == null ? null : addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_ALL_VALUES_FROM(ClassExpressionType.OBJECT_ALL_VALUES_FROM, new Translator<OWLObjectAllValuesFrom, OntCE.ObjectAllValuesFrom>() {
            @Override
            OntCE.ObjectAllValuesFrom translate(OntGraphModel model, OWLObjectAllValuesFrom expression) {
                OntOPE p = addObjectProperty(model, expression.getProperty());
                OntCE c = addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectAllValuesFrom(p, c);
            }
        }),
        DATA_ALL_VALUES_FROM(ClassExpressionType.DATA_ALL_VALUES_FROM, new Translator<OWLDataAllValuesFrom, OntCE.DataAllValuesFrom>() {
            @Override
            OntCE.DataAllValuesFrom translate(OntGraphModel model, OWLDataAllValuesFrom expression) {
                OntNDP p = addDataProperty(model, expression.getProperty());
                OntDR d = addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataAllValuesFrom(p, d);
            }
        }),
        OBJECT_SOME_VALUES_FROM(ClassExpressionType.OBJECT_SOME_VALUES_FROM, new Translator<OWLObjectSomeValuesFrom, OntCE.ObjectSomeValuesFrom>() {
            @Override
            OntCE.ObjectSomeValuesFrom translate(OntGraphModel model, OWLObjectSomeValuesFrom expression) {
                OntOPE p = addObjectProperty(model, expression.getProperty());
                OntCE c = addRDFNode(model, expression.getFiller()).as(OntCE.class);
                return model.createObjectSomeValuesFrom(p, c);
            }
        }),
        DATA_SOME_VALUES_FROM(ClassExpressionType.DATA_SOME_VALUES_FROM, new Translator<OWLDataSomeValuesFrom, OntCE.DataSomeValuesFrom>() {
            @Override
            OntCE.DataSomeValuesFrom translate(OntGraphModel model, OWLDataSomeValuesFrom expression) {
                OntNDP p = addDataProperty(model, expression.getProperty());
                OntDR d = addRDFNode(model, expression.getFiller()).as(OntDR.class);
                return model.createDataSomeValuesFrom(p, d);
            }
        }),
        OBJECT_HAS_VALUE(ClassExpressionType.OBJECT_HAS_VALUE, new Translator<OWLObjectHasValue, OntCE.ObjectHasValue>() {
            @Override
            OntCE.ObjectHasValue translate(OntGraphModel model, OWLObjectHasValue expression) {
                OntOPE p = addObjectProperty(model, expression.getProperty());
                OntIndividual i = addIndividual(model, expression.getFiller());
                return model.createObjectHasValue(p, i);
            }
        }),
        DATA_HAS_VALUE(ClassExpressionType.DATA_HAS_VALUE, new Translator<OWLDataHasValue, OntCE.DataHasValue>() {
            @Override
            OntCE.DataHasValue translate(OntGraphModel model, OWLDataHasValue expression) {
                OntNDP p = addDataProperty(model, expression.getProperty());
                Literal l = addLiteral(model, expression.getFiller());
                return model.createDataHasValue(p, l);
            }
        }),
        HAS_SELF(ClassExpressionType.OBJECT_HAS_SELF, new Translator<OWLObjectHasSelf, OntCE.HasSelf>() {
            @Override
            OntCE.HasSelf translate(OntGraphModel model, OWLObjectHasSelf expression) {
                return model.createHasSelf(addObjectProperty(model, expression.getProperty()));
            }
        }),
        UNION_OF(ClassExpressionType.OBJECT_UNION_OF, new Translator<OWLObjectUnionOf, OntCE.UnionOf>() {
            @Override
            OntCE.UnionOf translate(OntGraphModel model, OWLObjectUnionOf expression) {
                return model.createUnionOf(expression.operands().map(ce -> addRDFNode(model, ce).as(OntCE.class)).collect(Collectors.toList()));
            }
        }),
        INTERSECTION_OF(ClassExpressionType.OBJECT_INTERSECTION_OF, new Translator<OWLObjectIntersectionOf, OntCE.IntersectionOf>() {
            @Override
            OntCE.IntersectionOf translate(OntGraphModel model, OWLObjectIntersectionOf expression) {
                return model.createIntersectionOf(expression.operands().map(ce -> addRDFNode(model, ce).as(OntCE.class)).collect(Collectors.toList()));
            }
        }),
        ONE_OF(ClassExpressionType.OBJECT_ONE_OF, new Translator<OWLObjectOneOf, OntCE.OneOf>() {
            @Override
            OntCE.OneOf translate(OntGraphModel model, OWLObjectOneOf expression) {
                return model.createOneOf(expression.operands().map(i -> addIndividual(model, i)).collect(Collectors.toList()));
            }
        }),
        COMPLEMENT_OF(ClassExpressionType.OBJECT_COMPLEMENT_OF, new Translator<OWLObjectComplementOf, OntCE.ComplementOf>() {
            @Override
            OntCE.ComplementOf translate(OntGraphModel model, OWLObjectComplementOf expression) {
                return model.createComplementOf(addRDFNode(model, expression.getOperand()).as(OntCE.class));
            }
        }),
        ;

        private final ClassExpressionType type;
        private final Translator<? extends OWLClassExpression, ? extends OntCE> translator;

        CETranslator(ClassExpressionType type, Translator<? extends OWLClassExpression, ? extends OntCE> translator) {
            this.type = type;
            this.translator = translator;
        }

        public static CETranslator valueOf(ClassExpressionType type) {
            for (CETranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends OWLClassExpression, TO extends OntCE> {
            @SuppressWarnings("unchecked")
            private Resource add(OntGraphModel model, OWLClassExpression expression) {
                return translate(model, (FROM) expression);
            }

            abstract TO translate(OntGraphModel model, FROM expression);
        }
    }
}
