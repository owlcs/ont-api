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
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.owlapi.objects.OWLAnonymousIndividualImpl;
import com.github.owlcs.ontapi.owlapi.objects.OWLLiteralImpl;
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

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public static Resource toResource(OWLIndividual individual) {
        return individual.isAnonymous() ?
                toResource(individual.asOWLAnonymousIndividual()) :
                toResource(individual.asOWLNamedIndividual().getIRI());
    }

    public static Resource toResource(OWLAnonymousIndividual individual) {
        return new ResourceImpl(toNode(individual), null);
    }

    public static Node toNode(HasIRI entity) {
        return toNode(entity.getIRI());
    }

    public static Node toNode(IRI iri) {
        return NodeFactory.createURI(iri.getIRIString());
    }

    public static Node toNode(OWLAnonymousIndividual individual) {
        BlankNodeId id = OWLAnonymousIndividualImpl.asONT(individual).getBlankNodeId();
        return NodeFactory.createBlankNode(id);
    }

    public static Node toNode(OWLLiteral literal) {
        LiteralLabel lab = OWLLiteralImpl.asONT(literal).getLiteralLabel();
        return NodeFactory.createLiteral(lab);
    }

    public static Resource toResource(IRI iri) {
        return ResourceFactory.createResource(OntApiException.notNull(iri, "Null iri").getIRIString());
    }

    public static Property toProperty(OWLPropertyExpression object) {
        return toProperty(toIRI(object));
    }

    private static Property toProperty(IRI iri) {
        return ResourceFactory.createProperty(OntApiException.notNull(iri, "Null iri").getIRIString());
    }

    public static Literal toLiteral(OWLLiteral literal) {
        return new LiteralImpl(toNode(literal), null);
    }

    /**
     * Gets an ONT-API entity resource-type from a {@link OWLEntity} object.
     *
     * @param entity {@link OWLEntity}, not {@code null}
     * @return a {@link Resource}-type of {@link OntEntity}
     */
    public static Resource getRDFType(OWLEntity entity) {
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

    /**
     * Gets an ONT-API entity class-type from a {@link OWLEntity} object.
     *
     * @param entity {@link OWLEntity}, not {@code null}
     * @return a {@code Class}-type of {@link OntEntity}
     */
    public static Class<? extends OntEntity> getEntityType(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return OntClass.Named.class;
        } else if (entity.isOWLDataProperty()) {
            return OntDataProperty.class;
        } else if (entity.isOWLObjectProperty()) {
            return OntObjectProperty.Named.class;
        } else if (entity.isOWLNamedIndividual()) {
            return OntIndividual.Named.class;
        } else if (entity.isOWLAnnotationProperty()) {
            return OntAnnotationProperty.class;
        } else if (entity.isOWLDatatype()) {
            return OntDataRange.Named.class;
        }
        throw new OntApiException("Unsupported " + entity);
    }

    /**
     * Gets the ONT-API facet type by OWL-API {@link OWLFacet}.
     *
     * @param facet {@link OWLFacet}, not {@code null}
     * @return {@code Class}-type of {@link OntFacetRestriction}
     * @see ReadHelper#getFacet(Class)
     */
    public static Class<? extends OntFacetRestriction> getFRType(OWLFacet facet) {
        switch (facet) {
            case LENGTH:
                return OntFacetRestriction.Length.class;
            case MIN_LENGTH:
                return OntFacetRestriction.MinLength.class;
            case MAX_LENGTH:
                return OntFacetRestriction.MaxLength.class;
            case MIN_INCLUSIVE:
                return OntFacetRestriction.MinInclusive.class;
            case MAX_INCLUSIVE:
                return OntFacetRestriction.MaxInclusive.class;
            case MIN_EXCLUSIVE:
                return OntFacetRestriction.MinExclusive.class;
            case MAX_EXCLUSIVE:
                return OntFacetRestriction.MaxExclusive.class;
            case PATTERN:
                return OntFacetRestriction.Pattern.class;
            case FRACTION_DIGITS:
                return OntFacetRestriction.FractionDigits.class;
            case TOTAL_DIGITS:
                return OntFacetRestriction.TotalDigits.class;
            case LANG_RANGE:
                return OntFacetRestriction.LangRange.class;
        }
        throw new OntApiException.IllegalArgument("Unsupported " + facet);
    }


    /**
     * Extracts {@link IRI} from {@link OWLObject}.
     *
     * @param object {@link OWLObject}, not {@code null}
     * @return {@link IRI}
     */
    public static IRI toIRI(OWLObject object) {
        if (OntApiException.notNull(object, "Null owl-object specified.").isIRI()) return (IRI) object;
        if (object instanceof HasIRI) {
            return ((HasIRI) object).getIRI();
        }
        if (object instanceof OWLAnnotationObject) {
            return ((OWLAnnotationObject) object).asIRI().orElseThrow(() -> new OntApiException.IllegalArgument("Not iri: " + object));
        }
        if (object instanceof OWLClassExpression) {
            return toIRI((OWLClassExpression) object);
        }
        if (object instanceof OWLPropertyExpression) {
            return toIRI((OWLPropertyExpression) object);
        }
        throw new OntApiException.IllegalArgument("Unsupported owl-object: " + object);
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
        throw new OntApiException.IllegalArgument("Unsupported property-expression: " + expression);
    }

    /**
     * Constructs or retrieves a {@code Node} from the given {@code OWLObject}.
     * Expressions and external anonymous individuals are ignored.
     * This is to perform optimization searching in a graph.
     *
     * @param obj {@link OWLObject}
     * @return {@code Node} or {@code null}
     */
    public static Node getSearchNode(OWLObject obj) {
        if (obj.isIRI()) {
            return toNode((IRI) obj);
        }
        if (obj instanceof HasIRI) {
            return toNode((HasIRI) obj);
        }
        if (obj instanceof OWLAnonymousIndividual) {
            return toNode((OWLAnonymousIndividual) obj);
        }
        if (obj instanceof OWLLiteral) {
            return toNode((OWLLiteral) obj);
        }
        return null;
    }

    public static void writeAssertionTriple(OntModel model,
                                            OWLObject subject,
                                            OWLPropertyExpression property,
                                            OWLObject object,
                                            Collection<OWLAnnotation> annotations) {
        OntObject s = addRDFNode(model, subject).as(OntObject.class);
        Property p = addRDFNode(model, property).as(Property.class);
        RDFNode o = addRDFNode(model, object);
        addAnnotations(s.addStatement(p, o), annotations);
    }

    public static void writeDeclarationTriple(OntModel model,
                                              OWLEntity subject,
                                              Property predicate,
                                              RDFNode object,
                                              Collection<OWLAnnotation> annotations) {
        addAnnotations(toResource(subject)
                .inModel(model).addProperty(predicate, object).as(getEntityType(subject)).getMainStatement(), annotations);
    }

    public static void writeTriple(OntModel model,
                                   OWLObject subject,
                                   Property predicate,
                                   OWLObject object,
                                   Collection<OWLAnnotation> annotations) {
        writeTriple(model, subject, predicate, addRDFNode(model, object), annotations);
    }

    public static void writeTriple(OntModel model,
                                   OWLObject subject,
                                   Property predicate,
                                   RDFNode object,
                                   Collection<OWLAnnotation> annotations) {
        OntObject s = addRDFNode(model, subject).as(OntObject.class);
        addAnnotations(s.addStatement(predicate, object), annotations);
    }

    /**
     * Writes an annotated list.
     *
     * @param model       {@link OntModel}, to write in, not {@code null}
     * @param subject     {@link OWLObject}, not {@code null}
     * @param predicate   {@link Property}, not {@code null}
     * @param objects     a {@code Collection} of operands, not {@code null}
     * @param annotations a {@code Collection} of annotations, not {@code null}
     */
    public static void writeList(OntModel model,
                                 OWLObject subject,
                                 Property predicate,
                                 Collection<? extends OWLObject> objects,
                                 Collection<OWLAnnotation> annotations) {
        OntObject s = addRDFNode(model, subject).as(OntObject.class);
        addAnnotations(s.addStatement(predicate, addRDFList(model, objects)), annotations);
    }

    public static RDFList addRDFList(OntModel model, Collection<? extends OWLObject> objects) {
        return model.createList(objects.stream().map(o -> addRDFNode(model, o)).iterator());
    }

    public static OntAnnotationProperty addAnnotationProperty(OntModel model, OWLEntity entity) {
        String uri = entity.getIRI().getIRIString();
        return fetchOntEntity(model, OntAnnotationProperty.class, uri);
    }

    public static OntObjectProperty addObjectProperty(OntModel model, OWLObjectPropertyExpression ope) {
        if (!ope.isOWLObjectProperty()) {
            return addInverseOf(model, (OWLObjectInverseOf) ope);
        }
        return fetchOntEntity(model, OntObjectProperty.Named.class, ope.getNamedProperty().getIRI().getIRIString());
    }

    public static OntDataProperty addDataProperty(OntModel model, OWLDataPropertyExpression dpe) {
        if (!dpe.isOWLDataProperty()) throw new OntApiException("Unsupported " + dpe);
        String uri = dpe.asOWLDataProperty().getIRI().getIRIString();
        return fetchOntEntity(model, OntDataProperty.class, uri);
    }

    public static OntEntity addOntEntity(OntModel model, OWLEntity entity) {
        Class<? extends OntEntity> view = getEntityType(entity);
        String uri = entity.getIRI().getIRIString();
        return fetchOntEntity(model, view, uri);
    }

    public static OntObjectProperty.Inverse addInverseOf(OntModel model, OWLObjectInverseOf io) {
        String uri = io.getInverseProperty().getNamedProperty().getIRI().getIRIString();
        return fetchOntEntity(model, OntObjectProperty.Named.class, uri).createInverse();
    }

    public static OntFacetRestriction addFacetRestriction(OntModel model, OWLFacetRestriction fr) {
        return model.createFacetRestriction(getFRType(fr.getFacet()), addLiteral(model, fr.getFacetValue()));
    }

    public static OntClass addClassExpression(OntModel model, OWLClassExpression ce) {
        if (ce.isOWLClass()) {
            return addOntEntity(model, ce.asOWLClass()).as(OntClass.Named.class);
        }
        ClassExpressionType type = ce.getClassExpressionType();
        CETranslator cet = OntApiException.notNull(CETranslator.valueOf(type),
                "Unsupported class-expression " + ce + "/" + type);
        return cet.translator.add(model, ce).as(OntClass.class);
    }

    public static OntDataRange addDataRange(OntModel model, OWLDataRange dr) {
        if (dr.isOWLDatatype()) {
            return addOntEntity(model, dr.asOWLDatatype()).as(OntDataRange.Named.class);
        }
        DataRangeType type = dr.getDataRangeType();
        DRTranslator drt = OntApiException.notNull(DRTranslator.valueOf(type),
                "Unsupported data-range expression " + dr + "/" + type);
        return drt.translator.add(model, dr).as(OntDataRange.class);
    }

    public static OntIndividual.Anonymous getAnonymousIndividual(OntModel model, OWLAnonymousIndividual ai) {
        Resource res = toResource(OntApiException.notNull(ai, "Null anonymous individual.")).inModel(model);
        if (!res.canAs(OntIndividual.Anonymous.class)) {
            //throw new OntApiException("Anonymous individual should be created first: " + ai + ".");
            return OntModels.asAnonymousIndividual(res);
        }
        return res.as(OntIndividual.Anonymous.class);
    }

    public static OntIndividual addIndividual(OntModel model, OWLIndividual i) {
        if (i.isAnonymous()) return getAnonymousIndividual(model, i.asOWLAnonymousIndividual());
        String uri = i.asOWLNamedIndividual().getIRI().getIRIString();
        return fetchOntEntity(model, OntIndividual.Named.class, uri);
    }

    public static <E extends OntEntity> E fetchOntEntity(OntModel model, Class<E> type, String uri) {
        E res = model.getOntEntity(type, uri);
        if (res == null || !res.isBuiltIn()) {
            res = model.createOntEntity(type, uri);
        }
        return res;
    }

    /**
     * The main method to add OWLObject as RDFNode to the specified model.
     *
     * @param model {@link OntModel}
     * @param o     {@link OWLObject}
     * @return {@link RDFNode} node, attached to the model.
     */
    public static RDFNode addRDFNode(OntModel model, OWLObject o) {
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

    public static OntSWRL.Variable addSWRLVariable(OntModel model, SWRLVariable var) {
        return model.createSWRLVariable(var.getIRI().getIRIString());
    }

    public static OntSWRL.Atom<?> addSWRLAtom(OntModel model, SWRLAtom atom) {
        SWRLAtomTranslator swrlt = OntApiException.notNull(SWRLAtomTranslator.valueOf(atom),
                "Unsupported swrl-atom " + atom);
        return swrlt.translator.add(model, atom).as(OntSWRL.Atom.class);
    }

    public static RDFNode addSWRLObject(OntModel model, SWRLObject o) {
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
     * Writes OWL-API annotations for the given ONT statement.
     *
     * @param statement   a {@link OntStatement}, not {@code null}
     * @param annotations a {@code Collection} of {@link OWLAnnotation annotation}s
     */
    public static void addAnnotations(OntStatement statement, Collection<OWLAnnotation> annotations) {
        annotations.forEach(a -> {
            OntStatement st = statement.addAnnotation(addAnnotationProperty(statement.getModel(), a.getProperty()),
                    addRDFNode(statement.getModel(), a.getValue()));
            addAnnotations(st, a.annotationsAsList());
        });
    }

    /**
     * Writes OWL-API annotations for the given {@code OntObject}.
     *
     * @param object      a {@link OntObject}, not {@code null}
     * @param annotations a {@code Collection} of {@link OWLAnnotation annotation}s
     */
    public static void addAnnotations(OntObject object, Collection<OWLAnnotation> annotations) {
        addAnnotations(OntApiException.notNull(object.getMainStatement(), "Can't determine the root statement for " + object),
                annotations);
    }

    public static Literal addLiteral(OntModel model, OWLLiteral literal) {
        addDataRange(model, literal.getDatatype()).as(OntDataRange.Named.class);
        return model.asRDFNode(toNode(literal)).asLiteral();
    }

    /**
     * for SWRLAtom
     */
    private enum SWRLAtomTranslator {
        BUILT_IN(SWRLBuiltInAtom.class, new Translator<SWRLBuiltInAtom, OntSWRL.Atom.WithBuiltin>() {
            @Override
            OntSWRL.Atom.WithBuiltin translate(OntModel model, SWRLBuiltInAtom atom) {
                return model.createBuiltInSWRLAtom(model.createResource(atom.getPredicate().getIRIString()),
                        atom.arguments().map(a -> addSWRLObject(model, a).as(OntSWRL.DArg.class)).collect(Collectors.toList()));
            }
        }),
        OWL_CLASS(SWRLClassAtom.class, new Translator<SWRLClassAtom, OntSWRL.Atom.WithClass>() {
            @Override
            OntSWRL.Atom.WithClass translate(OntModel model, SWRLClassAtom atom) {
                return model.createClassSWRLAtom(addClassExpression(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getArgument()).as(OntSWRL.IArg.class));
            }
        }),
        DATA_PROPERTY(SWRLDataPropertyAtom.class, new Translator<SWRLDataPropertyAtom, OntSWRL.Atom.WithDataProperty>() {
            @Override
            OntSWRL.Atom.WithDataProperty translate(OntModel model, SWRLDataPropertyAtom atom) {
                return model.createDataPropertySWRLAtom(addDataProperty(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.DArg.class));
            }
        }),
        DATA_RANGE(SWRLDataRangeAtom.class, new Translator<SWRLDataRangeAtom, OntSWRL.Atom.WithDataRange>() {
            @Override
            OntSWRL.Atom.WithDataRange translate(OntModel model, SWRLDataRangeAtom atom) {
                return model.createDataRangeSWRLAtom(addDataRange(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getArgument()).as(OntSWRL.DArg.class));
            }
        }),
        DIFFERENT_INDIVIDUALS(SWRLDifferentIndividualsAtom.class, new Translator<SWRLDifferentIndividualsAtom, OntSWRL.Atom.WithDifferentIndividuals>() {
            @Override
            OntSWRL.Atom.WithDifferentIndividuals translate(OntModel model, SWRLDifferentIndividualsAtom atom) {
                return model.createDifferentIndividualsSWRLAtom(addSWRLObject(model,
                        atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        OBJECT_PROPERTY(SWRLObjectPropertyAtom.class, new Translator<SWRLObjectPropertyAtom, OntSWRL.Atom.WithObjectProperty>() {
            @Override
            OntSWRL.Atom.WithObjectProperty translate(OntModel model, SWRLObjectPropertyAtom atom) {
                return model.createObjectPropertySWRLAtom(addObjectProperty(model, atom.getPredicate()),
                        addSWRLObject(model, atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        SAME_INDIVIDUALS(SWRLSameIndividualAtom.class, new Translator<SWRLSameIndividualAtom, OntSWRL.Atom.WithSameIndividuals>() {
            @Override
            OntSWRL.Atom.WithSameIndividuals translate(OntModel model, SWRLSameIndividualAtom atom) {
                return model.createSameIndividualsSWRLAtom(addSWRLObject(model,
                        atom.getFirstArgument()).as(OntSWRL.IArg.class),
                        addSWRLObject(model, atom.getSecondArgument()).as(OntSWRL.IArg.class));
            }
        }),
        ;

        private final Translator<? extends SWRLAtom, ? extends OntSWRL.Atom<?>> translator;
        private final Class<? extends SWRLAtom> type;

        SWRLAtomTranslator(Class<? extends SWRLAtom> type, Translator<? extends SWRLAtom, ? extends OntSWRL.Atom<?>> translator) {
            this.translator = translator;
            this.type = type;
        }

        private static SWRLAtomTranslator valueOf(SWRLAtom atom) {
            for (SWRLAtomTranslator t : values()) {
                if (t.type.isInstance(atom)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends SWRLAtom, TO extends OntSWRL.Atom<?>> {
            @SuppressWarnings("unchecked")
            private Resource add(OntModel model, SWRLAtom atom) {
                return translate(model, (FROM) atom);
            }

            abstract TO translate(OntModel model, FROM atom);
        }
    }

    /**
     * Data Range translator
     */
    private enum DRTranslator {
        ONE_OF(DataRangeType.DATA_ONE_OF, new Translator<OWLDataOneOf, OntDataRange.OneOf>() {
            @Override
            OntDataRange.OneOf translate(OntModel model, OWLDataOneOf expression) {
                return model.createDataOneOf(expression.values()
                        .map(l -> addLiteral(model, l)).collect(Collectors.toList()));
            }
        }),
        RESTRICTION(DataRangeType.DATATYPE_RESTRICTION, new Translator<OWLDatatypeRestriction, OntDataRange.Restriction>() {
            @Override
            OntDataRange.Restriction translate(OntModel model, OWLDatatypeRestriction expression) {
                return model.createDataRestriction(addRDFNode(model, expression.getDatatype()).as(OntDataRange.Named.class),
                        expression.facetRestrictions()
                                .map(f -> addFacetRestriction(model, f)).collect(Collectors.toList()));
            }
        }),
        COMPLEMENT_OF(DataRangeType.DATA_COMPLEMENT_OF, new Translator<OWLDataComplementOf, OntDataRange.ComplementOf>() {
            @Override
            OntDataRange.ComplementOf translate(OntModel model, OWLDataComplementOf expression) {
                return model.createDataComplementOf(addRDFNode(model, expression.getDataRange()).as(OntDataRange.class));
            }
        }),
        UNION_OF(DataRangeType.DATA_UNION_OF, new Translator<OWLDataUnionOf, OntDataRange.UnionOf>() {
            @Override
            OntDataRange.UnionOf translate(OntModel model, OWLDataUnionOf expression) {
                return model.createDataUnionOf(expression.operands()
                        .map(dr -> addRDFNode(model, dr).as(OntDataRange.class)).collect(Collectors.toList()));
            }
        }),
        INTERSECTION_OF(DataRangeType.DATA_INTERSECTION_OF, new Translator<OWLDataIntersectionOf, OntDataRange.IntersectionOf>() {
            @Override
            OntDataRange.IntersectionOf translate(OntModel model, OWLDataIntersectionOf expression) {
                return model.createDataIntersectionOf(expression.operands()
                        .map(dr -> addRDFNode(model, dr).as(OntDataRange.class)).collect(Collectors.toList()));
            }
        }),
        ;
        private final DataRangeType type;
        private final Translator<? extends OWLDataRange, ? extends OntDataRange> translator;

        DRTranslator(DataRangeType type, Translator<? extends OWLDataRange, ? extends OntDataRange> translator) {
            this.translator = translator;
            this.type = type;
        }

        public static DRTranslator valueOf(DataRangeType type) {
            for (DRTranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends OWLDataRange, TO extends OntDataRange> {
            @SuppressWarnings("unchecked")
            private Resource add(OntModel model, OWLDataRange expression) {
                return translate(model, (FROM) expression);
            }

            abstract TO translate(OntModel model, FROM expression);
        }
    }

    /**
     * Class Expression translator
     */
    private enum CETranslator {
        OBJECT_MAX_CARDINALITY(ClassExpressionType.OBJECT_MAX_CARDINALITY, new Translator<OWLObjectMaxCardinality, OntClass.ObjectMaxCardinality>() {
            @Override
            OntClass.ObjectMaxCardinality translate(OntModel model, OWLObjectMaxCardinality expression) {
                OntObjectProperty p = addObjectProperty(model, expression.getProperty());
                OntClass c = expression.getFiller() == null ? null :
                        addRDFNode(model, expression.getFiller()).as(OntClass.class);
                return model.createObjectMaxCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_MAX_CARDINALITY(ClassExpressionType.DATA_MAX_CARDINALITY, new Translator<OWLDataMaxCardinality, OntClass.DataMaxCardinality>() {
            @Override
            OntClass.DataMaxCardinality translate(OntModel model, OWLDataMaxCardinality expression) {
                OntDataProperty p = addDataProperty(model, expression.getProperty());
                OntDataRange d = expression.getFiller() == null ? null :
                        addRDFNode(model, expression.getFiller()).as(OntDataRange.class);
                return model.createDataMaxCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_MIN_CARDINALITY(ClassExpressionType.OBJECT_MIN_CARDINALITY, new Translator<OWLObjectMinCardinality, OntClass.ObjectMinCardinality>() {
            @Override
            OntClass.ObjectMinCardinality translate(OntModel model, OWLObjectMinCardinality expression) {
                OntObjectProperty p = addObjectProperty(model, expression.getProperty());
                OntClass c = expression.getFiller() == null ? null :
                        addRDFNode(model, expression.getFiller()).as(OntClass.class);
                return model.createObjectMinCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_MIN_CARDINALITY(ClassExpressionType.DATA_MIN_CARDINALITY, new Translator<OWLDataMinCardinality, OntClass.DataMinCardinality>() {
            @Override
            OntClass.DataMinCardinality translate(OntModel model, OWLDataMinCardinality expression) {
                OntDataProperty p = addDataProperty(model, expression.getProperty());
                OntDataRange d = expression.getFiller() == null ? null :
                        addRDFNode(model, expression.getFiller()).as(OntDataRange.class);
                return model.createDataMinCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_EXACT_CARDINALITY(ClassExpressionType.OBJECT_EXACT_CARDINALITY, new Translator<OWLObjectExactCardinality, OntClass.ObjectCardinality>() {
            @Override
            OntClass.ObjectCardinality translate(OntModel model, OWLObjectExactCardinality expression) {
                OntObjectProperty p = addObjectProperty(model, expression.getProperty());
                OntClass c = expression.getFiller() == null ? null :
                        addRDFNode(model, expression.getFiller()).as(OntClass.class);
                return model.createObjectCardinality(p, expression.getCardinality(), c);
            }
        }),
        DATA_EXACT_CARDINALITY(ClassExpressionType.DATA_EXACT_CARDINALITY, new Translator<OWLDataExactCardinality, OntClass.DataCardinality>() {
            @Override
            OntClass.DataCardinality translate(OntModel model, OWLDataExactCardinality expression) {
                OntDataProperty p = addDataProperty(model, expression.getProperty());
                OntDataRange d = expression.getFiller() == null ? null :
                        addRDFNode(model, expression.getFiller()).as(OntDataRange.class);
                return model.createDataCardinality(p, expression.getCardinality(), d);
            }
        }),
        OBJECT_ALL_VALUES_FROM(ClassExpressionType.OBJECT_ALL_VALUES_FROM, new Translator<OWLObjectAllValuesFrom, OntClass.ObjectAllValuesFrom>() {
            @Override
            OntClass.ObjectAllValuesFrom translate(OntModel model, OWLObjectAllValuesFrom expression) {
                OntObjectProperty p = addObjectProperty(model, expression.getProperty());
                OntClass c = addRDFNode(model, expression.getFiller()).as(OntClass.class);
                return model.createObjectAllValuesFrom(p, c);
            }
        }),
        DATA_ALL_VALUES_FROM(ClassExpressionType.DATA_ALL_VALUES_FROM, new Translator<OWLDataAllValuesFrom, OntClass.DataAllValuesFrom>() {
            @Override
            OntClass.DataAllValuesFrom translate(OntModel model, OWLDataAllValuesFrom expression) {
                OntDataProperty p = addDataProperty(model, expression.getProperty());
                OntDataRange d = addRDFNode(model, expression.getFiller()).as(OntDataRange.class);
                return model.createDataAllValuesFrom(p, d);
            }
        }),
        OBJECT_SOME_VALUES_FROM(ClassExpressionType.OBJECT_SOME_VALUES_FROM, new Translator<OWLObjectSomeValuesFrom, OntClass.ObjectSomeValuesFrom>() {
            @Override
            OntClass.ObjectSomeValuesFrom translate(OntModel model, OWLObjectSomeValuesFrom expression) {
                OntObjectProperty p = addObjectProperty(model, expression.getProperty());
                OntClass c = addRDFNode(model, expression.getFiller()).as(OntClass.class);
                return model.createObjectSomeValuesFrom(p, c);
            }
        }),
        DATA_SOME_VALUES_FROM(ClassExpressionType.DATA_SOME_VALUES_FROM, new Translator<OWLDataSomeValuesFrom, OntClass.DataSomeValuesFrom>() {
            @Override
            OntClass.DataSomeValuesFrom translate(OntModel model, OWLDataSomeValuesFrom expression) {
                OntDataProperty p = addDataProperty(model, expression.getProperty());
                OntDataRange d = addRDFNode(model, expression.getFiller()).as(OntDataRange.class);
                return model.createDataSomeValuesFrom(p, d);
            }
        }),
        OBJECT_HAS_VALUE(ClassExpressionType.OBJECT_HAS_VALUE, new Translator<OWLObjectHasValue, OntClass.ObjectHasValue>() {
            @Override
            OntClass.ObjectHasValue translate(OntModel model, OWLObjectHasValue expression) {
                OntObjectProperty p = addObjectProperty(model, expression.getProperty());
                OntIndividual i = addIndividual(model, expression.getFiller());
                return model.createObjectHasValue(p, i);
            }
        }),
        DATA_HAS_VALUE(ClassExpressionType.DATA_HAS_VALUE, new Translator<OWLDataHasValue, OntClass.DataHasValue>() {
            @Override
            OntClass.DataHasValue translate(OntModel model, OWLDataHasValue expression) {
                OntDataProperty p = addDataProperty(model, expression.getProperty());
                Literal l = addLiteral(model, expression.getFiller());
                return model.createDataHasValue(p, l);
            }
        }),
        HAS_SELF(ClassExpressionType.OBJECT_HAS_SELF, new Translator<OWLObjectHasSelf, OntClass.HasSelf>() {
            @Override
            OntClass.HasSelf translate(OntModel model, OWLObjectHasSelf expression) {
                return model.createHasSelf(addObjectProperty(model, expression.getProperty()));
            }
        }),
        UNION_OF(ClassExpressionType.OBJECT_UNION_OF, new Translator<OWLObjectUnionOf, OntClass.UnionOf>() {
            @Override
            OntClass.UnionOf translate(OntModel model, OWLObjectUnionOf expression) {
                return model.createObjectUnionOf(expression.operands()
                        .map(ce -> addRDFNode(model, ce).as(OntClass.class)).collect(Collectors.toList()));
            }
        }),
        INTERSECTION_OF(ClassExpressionType.OBJECT_INTERSECTION_OF, new Translator<OWLObjectIntersectionOf, OntClass.IntersectionOf>() {
            @Override
            OntClass.IntersectionOf translate(OntModel model, OWLObjectIntersectionOf expression) {
                return model.createObjectIntersectionOf(expression.operands()
                        .map(ce -> addRDFNode(model, ce).as(OntClass.class)).collect(Collectors.toList()));
            }
        }),
        ONE_OF(ClassExpressionType.OBJECT_ONE_OF, new Translator<OWLObjectOneOf, OntClass.OneOf>() {
            @Override
            OntClass.OneOf translate(OntModel model, OWLObjectOneOf expression) {
                return model.createObjectOneOf(expression.operands()
                        .map(i -> addIndividual(model, i)).collect(Collectors.toList()));
            }
        }),
        COMPLEMENT_OF(ClassExpressionType.OBJECT_COMPLEMENT_OF, new Translator<OWLObjectComplementOf, OntClass.ComplementOf>() {
            @Override
            OntClass.ComplementOf translate(OntModel model, OWLObjectComplementOf expression) {
                return model.createObjectComplementOf(addRDFNode(model, expression.getOperand()).as(OntClass.class));
            }
        }),
        ;

        private final ClassExpressionType type;
        private final Translator<? extends OWLClassExpression, ? extends OntClass> translator;

        CETranslator(ClassExpressionType type, Translator<? extends OWLClassExpression, ? extends OntClass> translator) {
            this.type = type;
            this.translator = translator;
        }

        public static CETranslator valueOf(ClassExpressionType type) {
            for (CETranslator t : values()) {
                if (t.type.equals(type)) return t;
            }
            return null;
        }

        private static abstract class Translator<FROM extends OWLClassExpression, TO extends OntClass> {
            @SuppressWarnings("unchecked")
            private Resource add(OntModel model, OWLClassExpression expression) {
                return translate(model, (FROM) expression);
            }

            abstract TO translate(OntModel model, FROM expression);
        }
    }
}
