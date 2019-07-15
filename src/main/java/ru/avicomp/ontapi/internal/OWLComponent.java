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

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OwlObjects;

import java.util.*;
import java.util.stream.Stream;

/**
 * Enum of mostly used OWL-API object types, to be used in filter operations.
 * Created by @ssz on 13.07.2019.
 */
public enum OWLComponent {
    IRI(org.semanticweb.owlapi.model.IRI.class, true) {
        @Override
        List<OWLComponent> includes() {
            return Collections.emptyList();
        }

        @Override
        Stream<IRI> components(OWLObject o) {
            // this differs from the OWL-API behaviour (see https://github.com/owlcs/owlapi/issues/865)
            return OwlObjects.iris(o);
        }
    },
    ANNOTATION_PROPERTY(OWLAnnotationProperty.class, true) {
        @Override
        Stream<OWLAnnotationProperty> components(OWLObject o) {
            return o.annotationPropertiesInSignature();
        }
    },
    DATATYPE_PROPERTY(OWLDataProperty.class, true) {
        @Override
        Stream<OWLDataProperty> components(OWLObject o) {
            return o.dataPropertiesInSignature();
        }
    },
    NAMED_OBJECT_PROPERTY(OWLObjectProperty.class, true) {
        @Override
        Stream<OWLObjectProperty> components(OWLObject o) {
            return o.objectPropertiesInSignature();
        }
    },
    NAMED_INDIVIDUAL(OWLNamedIndividual.class, true) {
        @Override
        Stream<OWLNamedIndividual> components(OWLObject o) {
            return o.individualsInSignature();
        }
    },
    CLASS(OWLClass.class, true) {
        @Override
        Stream<OWLClass> components(OWLObject o) {
            return o.classesInSignature();
        }
    },
    DATATYPE(OWLDatatype.class, true) {
        @Override
        Stream<OWLDatatype> components(OWLObject o) {
            return o.datatypesInSignature();
        }
    },
    ENTITY(OWLEntity.class, false) {
        @Override
        List<OWLComponent> includes() {
            return Arrays.asList(ANNOTATION_PROPERTY, DATATYPE_PROPERTY, NAMED_OBJECT_PROPERTY,
                    NAMED_INDIVIDUAL, CLASS, DATATYPE);
        }

        @Override
        Stream<OWLEntity> components(OWLObject o) {
            return o.signature();
        }
    },
    LITERAL(OWLLiteral.class, true) {
        @Override
        List<OWLComponent> includes() {
            return Collections.singletonList(DATATYPE);
        }
    },
    INDIVIDUAL(OWLIndividual.class, false) {
        @Override
        List<OWLComponent> includes() {
            return Arrays.asList(NAMED_INDIVIDUAL, ANONYMOUS_INDIVIDUAL);
        }

        @Override
        Stream<OWLIndividual> components(OWLObject o) {
            return Stream.concat(o.anonymousIndividuals(), o.individualsInSignature());
        }
    },
    ANONYMOUS_INDIVIDUAL(OWLAnonymousIndividual.class, true),
    DATA_RANGE(OWLDataRange.class, false) {
        /**
         * {@inheritDoc}
         * + literals ({@code DataOneOf}, {@code DatatypeRestriction})
         */
        @Override
        List<OWLComponent> includes() {
            return Arrays.asList(DATATYPE, LITERAL);
        }
    },
    CLASS_EXPRESSION(OWLClassExpression.class, false) {
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
        List<OWLComponent> includes() {
            return Arrays.asList(CLASS, INDIVIDUAL, OBJECT_PROPERTY_EXPRESSION, DATATYPE_PROPERTY, DATA_RANGE);
        }

        @Override
        Stream<OWLClassExpression> components(OWLObject o) {
            return o.nestedClassExpressions();
        }
    },
    OBJECT_PROPERTY_EXPRESSION(OWLObjectPropertyExpression.class, false) {
        @Override
        List<OWLComponent> includes() {
            return Collections.singletonList(NAMED_OBJECT_PROPERTY);
        }
    },
    ;

    private final Class<? extends OWLObject> type;
    private final boolean primitive;

    OWLComponent(Class<? extends OWLObject> type, boolean primitive) {
        this.type = type;
        this.primitive = primitive;
    }

    /**
     * Represents the given array of components as a {@code Set}.
     *
     * @param values Array of {@link OWLComponent}s
     * @return a {@code Set} of {@code OWLComponent}s
     */
    public static Set<OWLComponent> asSet(OWLComponent... values) {
        Set<OWLComponent> res = EnumSet.noneOf(OWLComponent.class);
        Arrays.stream(values).forEach(v -> v.putInSet(res));
        return res;
    }

    /**
     * Determines and returns the most specific type for the given {@link OWLObject}.
     * The primitive types go first, then the composite.
     *
     * @param o {@link OWLObject}, not {@code null}
     * @return {@link OWLComponent}
     */
    public static OWLComponent getType(OWLObject o) {
        Optional<OWLComponent> res = Arrays.stream(values()).filter(OWLComponent::isPrimitive)
                .filter(x -> x.type.isInstance(o))
                .findFirst();
        return res.orElseGet(() -> Arrays.stream(values()).filter(t -> !t.isPrimitive())
                .filter(x -> x.type.isInstance(o))
                .findFirst()
                .orElseThrow(() -> new OntApiException.Unsupported("Unsupported object type: " + o)));
    }

    /**
     * Lists all types, that make up or define this type.
     *
     * @return a {@code List} of {@link OWLComponent}s
     */
    List<OWLComponent> includes() {
        return Collections.singletonList(IRI);
    }

    private void putInSet(Set<OWLComponent> set) {
        if (!set.add(this)) {
            return;
        }
        includes().forEach(i -> i.putInSet(set));
    }

    Stream<? extends OWLObject> components(OWLObject container) {
        return OwlObjects.objects(type, container);
    }

    /**
     * Returns all components of this type from the specified {@link OWLObject}-container.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @return {@code Stream} of {@link OWLObject} of this type
     */
    @SuppressWarnings("unchecked")
    public Stream<OWLObject> select(OWLObject container) {
        return (Stream<OWLObject>) components(container);
    }

    /**
     * Answers {@code true} if the given object contains any component of this type.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @return boolean
     */
    public boolean isContainedIn(OWLObject container) {
        return components(container).findFirst().isPresent();
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
