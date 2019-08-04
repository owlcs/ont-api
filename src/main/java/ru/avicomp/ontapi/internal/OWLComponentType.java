/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OwlObjects;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.*;
import java.util.stream.Stream;

/**
 * Enum of mostly used OWL-API object types, that are not {@link OWLAxiom} or {@link OWLAnnotation},
 * to be used in various filter operations.
 * Includes both ONT-API and OWL-API ways.
 * Created by @ssz on 13.07.2019.
 */
public enum OWLComponentType {
    IRI(org.semanticweb.owlapi.model.IRI.class, Resource.class, true) {
        @Override
        List<OWLComponentType> includes() {
            return Collections.emptyList();
        }

        @Override
        Stream<IRI> components(OWLObject o) {
            // this differs from the OWL-API behaviour (see https://github.com/owlcs/owlapi/issues/865)
            return OwlObjects.iris(o);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.asIRI(n.as(OntObject.class));
        }

        @Override
        ExtendedIterator<Resource> listObjects(OntGraphModel model) {
            return Iter.create(() -> {
                Set<Resource> res = new HashSet<>();
                model.getBaseModel().listStatements().forEachRemaining(s -> {
                    res.add(s.getPredicate().inModel(model));
                    if (s.getSubject().isURIResource()) {
                        res.add(s.getSubject().inModel(model));
                    }
                    if (s.getObject().isURIResource()) {
                        res.add(s.getResource().inModel(model));
                    }
                });
                return res.iterator();
            });
        }

    },
    ANNOTATION_PROPERTY(OWLAnnotationProperty.class, OntNAP.class, true) {
        @Override
        Stream<OWLAnnotationProperty> components(OWLObject o) {
            return o.annotationPropertiesInSignature();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntNAP.class));
        }
    },
    DATATYPE_PROPERTY(OWLDataProperty.class, OntNDP.class, true) {
        @Override
        Stream<OWLDataProperty> components(OWLObject o) {
            return o.dataPropertiesInSignature();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntNDP.class));
        }
    },
    NAMED_OBJECT_PROPERTY(OWLObjectProperty.class, OntNOP.class, true) {
        @Override
        Stream<OWLObjectProperty> components(OWLObject o) {
            return o.objectPropertiesInSignature();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntNOP.class));
        }
    },
    NAMED_INDIVIDUAL(OWLNamedIndividual.class, OntIndividual.Named.class, true) {
        @Override
        Stream<OWLNamedIndividual> components(OWLObject o) {
            return o.individualsInSignature();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntIndividual.Named.class));
        }
    },
    CLASS(OWLClass.class, OntClass.class, true) {
        @Override
        Stream<OWLClass> components(OWLObject o) {
            return o.classesInSignature();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntClass.class));
        }
    },
    DATATYPE(OWLDatatype.class, OntDT.class, true) {
        @Override
        Stream<OWLDatatype> components(OWLObject o) {
            return o.datatypesInSignature();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntDT.class));
        }
    },
    ENTITY(OWLEntity.class, OntEntity.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(ANNOTATION_PROPERTY, DATATYPE_PROPERTY, NAMED_OBJECT_PROPERTY,
                    NAMED_INDIVIDUAL, CLASS, DATATYPE);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntEntity.class));
        }

        @Override
        Stream<OWLEntity> components(OWLObject o) {
            return o.signature();
        }
    },
    LITERAL(OWLLiteral.class, Literal.class, true) {
        @Override
        List<OWLComponentType> includes() {
            return Collections.singletonList(DATATYPE);
        }

        @Override
        ExtendedIterator<? extends RDFNode> listObjects(OntGraphModel model) {
            return model.getBaseModel().listObjects()
                    .filterKeep(RDFNode::isLiteral)
                    .mapWith(x -> x.asLiteral().inModel(model));
        }

        @Override
        public Stream<OWLObject> select(OWLObject container) {
            return OwlObjects.objects(owl, container);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.asLiteral());
        }
    },
    INDIVIDUAL(OWLIndividual.class, OntIndividual.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(NAMED_INDIVIDUAL, ANONYMOUS_INDIVIDUAL);
        }

        @Override
        Stream<OWLIndividual> components(OWLObject o) {
            return Stream.concat(o.anonymousIndividuals(), o.individualsInSignature());
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntIndividual.class));
        }
    },
    ANONYMOUS_INDIVIDUAL(OWLAnonymousIndividual.class, OntIndividual.Anonymous.class, true) {
        @Override
        Stream<OWLAnonymousIndividual> components(OWLObject o) {
            return o.anonymousIndividuals();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntIndividual.Anonymous.class));
        }
    },
    ANONYMOUS_DATA_RANGE(OWLDataRange.class, OntDR.class, false) {
        /**
         * {@inheritDoc}
         * + literals ({@code DataOneOf}, {@code DatatypeRestriction})
         */
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(DATA_RANGE, LITERAL, FACET_RESTRICTION);
        }

        @SuppressWarnings("unchecked")
        @Override
        Stream<OWLDataRange> components(OWLObject o) {
            //see https://github.com/owlcs/owlapi/issues/867
            return (Stream<OWLDataRange>) super.components(o).filter(x -> !(x instanceof OWLDatatype));
        }

        @SuppressWarnings("unchecked")
        @Override
        ExtendedIterator<OntDR> listObjects(OntGraphModel model) {
            return (ExtendedIterator<OntDR>) super.listObjects(model).filterKeep(RDFNode::isAnon);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntDR.class));
        }
    },
    DATA_RANGE(OWLDataRange.class, OntDR.class, false) {
        /**
         * {@inheritDoc}
         * + literals ({@code DataOneOf}, {@code DatatypeRestriction})
         */
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(DATATYPE, ANONYMOUS_DATA_RANGE);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntDR.class));
        }
    },
    ANONYMOUS_CLASS_EXPRESSION(OWLAnonymousClassExpression.class, OntCE.class, false) {
        /**
         * {@inheritDoc}
         * This component may contain:
         * <ul>
         *     <li>individual - as part of {@code ObjectOneOf}, {@code ObjectHasValue}</li>
         *     <li>object property - as part of {@code ObjectAllValuesFrom} and other restrictions</li>
         *     <li>datatype property and data range - as part of Data Property Restrictions (e.g. {@code DataExactCardinality})</li>
         * </ul>
         * It does not contain directly the literal type (although there are {@code ObjectExactCardinality},
         * {@code ObjectHasSelf}, etc which actually has literals in their descriptions -
         * see <a href='https://github.com/owlcs/owlapi/issues/783'/>),
         * but it comes indirectly from the data-range...
         */
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(CLASS_EXPRESSION, INDIVIDUAL, OBJECT_PROPERTY_EXPRESSION, DATATYPE_PROPERTY, DATA_RANGE);
        }

        @Override
        Stream<OWLClassExpression> components(OWLObject o) {
            return o.nestedClassExpressions().filter(IsAnonymous::isAnonymous);
        }

        @SuppressWarnings("unchecked")
        @Override
        ExtendedIterator<OntCE> listObjects(OntGraphModel model) {
            return (ExtendedIterator<OntCE>) super.listObjects(model).filterKeep(RDFNode::isAnon);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntCE.class));
        }
    },
    CLASS_EXPRESSION(OWLClassExpression.class, OntCE.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(CLASS, ANONYMOUS_CLASS_EXPRESSION);
        }

        @Override
        Stream<OWLClassExpression> components(OWLObject o) {
            return o.nestedClassExpressions();
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntCE.class));
        }
    },
    INVERSE_OBJECT_PROPERTY(OWLObjectInverseOf.class, OntOPE.Inverse.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Collections.singletonList(NAMED_OBJECT_PROPERTY);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntOPE.Inverse.class));
        }
    },
    OBJECT_PROPERTY_EXPRESSION(OWLObjectPropertyExpression.class, OntOPE.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(NAMED_OBJECT_PROPERTY, INVERSE_OBJECT_PROPERTY);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntOPE.class));
        }
    },
    FACET_RESTRICTION(OWLFacetRestriction.class, OntFR.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Collections.singletonList(LITERAL);
        }

        @Override
        ONTObject<OWLFacetRestriction> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntFR.class));
        }

    },
    SWRL_VARIABLE(SWRLVariable.class, OntSWRL.Variable.class, true) {
        @Override
        ONTObject<SWRLVariable> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntSWRL.Variable.class));
        }
    },
    SWRL_ATOM(SWRLAtom.class, OntSWRL.Atom.class, false) {
        @Override
        List<OWLComponentType> includes() {
            return Arrays.asList(INDIVIDUAL, LITERAL, SWRL_VARIABLE,
                    CLASS_EXPRESSION, DATA_RANGE, DATATYPE_PROPERTY, OBJECT_PROPERTY_EXPRESSION);
        }

        @Override
        ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df) {
            return df.get(n.as(OntSWRL.Atom.class));
        }
    }
    ;

    private static final Set<OWLComponentType> CACHE_KEYS = EnumSet.of(CLASS
            , DATATYPE
            , ANNOTATION_PROPERTY
            , DATATYPE_PROPERTY
            , NAMED_OBJECT_PROPERTY
            , NAMED_INDIVIDUAL
            , ANONYMOUS_INDIVIDUAL);

    private static final Set<OWLComponentType> SHARED_COMPONENTS = EnumSet.of(ANONYMOUS_CLASS_EXPRESSION
            , ANONYMOUS_DATA_RANGE
            , FACET_RESTRICTION
            , INVERSE_OBJECT_PROPERTY
            , SWRL_ATOM
            , SWRL_VARIABLE);

    final Class<? extends OWLObject> owl;
    final Class<? extends RDFNode> jena;
    private final boolean primitive;

    OWLComponentType(Class<? extends OWLObject> owl, Class<? extends RDFNode> rdf, boolean primitive) {
        this.owl = owl;
        this.jena = rdf;
        this.primitive = primitive;
    }

    /**
     * Represents the given array of components as a full {@code Set},
     * that includes all given components and all its dependent (sub-)components.
     *
     * @param values Array of {@link OWLComponentType}s
     * @return a {@code Set} of {@code OWLComponent}s
     */
    public static Set<OWLComponentType> toSet(OWLComponentType... values) {
        Set<OWLComponentType> res = EnumSet.noneOf(OWLComponentType.class);
        Arrays.stream(values).forEach(v -> v.putInSet(res));
        return res;
    }

    /**
     * Determines and returns the most specific type for the given {@link OWLObject}.
     * The primitive types go first, then the composite.
     *
     * @param o {@link OWLObject}, not {@code null}
     * @return {@link OWLComponentType}
     */
    public static OWLComponentType get(OWLObject o) {
        Optional<OWLComponentType> res = Arrays.stream(values()).filter(OWLComponentType::isPrimitive)
                .filter(x -> x.owl.isInstance(o))
                .findFirst();
        return res.orElseGet(() -> Arrays.stream(values()).filter(t -> !t.isPrimitive())
                .filter(x -> x.owl.isInstance(o))
                .findFirst()
                .orElseThrow(() -> new OntApiException.Unsupported("Unsupported object type: " + o)));
    }

    /**
     * Lists components that can be shared,
     * but at the same time are not {@link OWLEntity OWL entities}.
     *
     * @return {@code Stream} of {@link OWLContentType}s
     * @see InternalModel#getUsedComponentTriples(OntGraphModel, OWLObject)
     */
    static Stream<OWLComponentType> sharedComponents() {
        return SHARED_COMPONENTS.stream();
    }

    /**
     * Lists {@link OWLComponentType} that are used as keys in the {@link InternalModel internal model} components cache.
     *
     * @return {@code Stream} of {@link OWLContentType}s
     * @see InternalModel#components
     */
    static Stream<OWLComponentType> keys() {
        return CACHE_KEYS.stream();
    }

    /**
     * Lists all types, that make up or define this type.
     *
     * @return a {@code List} of {@link OWLComponentType}s
     */
    List<OWLComponentType> includes() {
        return Collections.singletonList(IRI);
    }

    private void putInSet(Set<OWLComponentType> set) {
        if (!set.add(this)) {
            return;
        }
        includes().forEach(i -> i.putInSet(set));
    }

    Stream<? extends OWLObject> components(OWLObject container) {
        return OwlObjects.parseComponents(owl, container);
    }

    @SuppressWarnings("unchecked")
    ExtendedIterator<? extends RDFNode> listObjects(OntGraphModel model) {
        return OntModels.listLocalObjects(model, (Class<? extends OntObject>) jena);
    }

    /**
     * Wraps the given node as {@link ONTObject}.
     *
     * @param n  {@link RDFNode}, not {@code null}
     * @param df {@link InternalObjectFactory}, not {@code null}
     * @return {@link ONTObject}
     */
    abstract ONTObject<? extends OWLObject> wrap(RDFNode n, InternalObjectFactory df);

    /**
     * Wraps the given {@link OWLObject} as {@link ONTObject} using the specified factory and model.
     * Note: currently it does not work for anonymous expressions, although it must work for anonymous individuals.
     *
     * @param object {@link OWLObject}, not {@code null}
     * @param model  {@link OntGraphModel}, not {@code null}
     * @param df     {@link InternalObjectFactory}, not {@code null}
     * @return {@link ONTObject}
     */
    @SuppressWarnings("unchecked")
    ONTObject<OWLObject> wrap(OWLObject object, OntGraphModel model, InternalObjectFactory df) {
        // if it is anonymous object then fail
        return (ONTObject<OWLObject>) wrap(WriteHelper.toRDFNode(object).inModel(model), df);
    }

    /**
     * Returns all components of this type from the specified {@link OWLObject}-container
     * in the form of {@code Stream} of {@link OWLObject}s.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @return {@code Stream} of {@link OWLObject} of this type
     */
    @SuppressWarnings("unchecked")
    public Stream<OWLObject> select(OWLObject container) {
        return (Stream<OWLObject>) components(container);
    }

    /**
     * Returns all components of this type from the specified {@link OWLObject}-container
     * in the form of {@code Stream} of {@link ONTObject}s.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @param df        {@link InternalObjectFactory}, not {@code null}
     * @return {@code Stream} of {@link ONTObject} encapsulating {@link OWLObject}s of this type
     */
    Stream<ONTObject<OWLObject>> select(OWLObject container, OntGraphModel model, InternalObjectFactory df) {
        return select(container).map(x -> wrap(x, model, df));
    }

    /**
     * Returns all components of this type from the specified {@link OntGraphModel model}-container
     * in the form of {@code Stream} of {@link ONTObject}s.
     *
     * @param model {@link OntGraphModel}, not {@code null}
     * @param df {@link InternalObjectFactory}, not {@code null}
     * @return {@link Stream} of {@link ONTObject}s of this type
     */
    @SuppressWarnings("unchecked")
    public Stream<ONTObject<OWLObject>> select(OntGraphModel model, InternalObjectFactory df) {
        return Iter.asStream(listObjects(model).mapWith(x -> (ONTObject<OWLObject>) wrap(x, df)));
    }

    /**
     * Answers {@code true} if the type is primitive.
     *
     * @return boolean
     * @see OWLPrimitive
     */
    public boolean isPrimitive() {
        return primitive;
    }

}
