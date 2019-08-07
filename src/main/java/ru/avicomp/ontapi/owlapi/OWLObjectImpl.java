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
package ru.avicomp.ontapi.owlapi;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AbstractCollectorEx;
import org.semanticweb.owlapi.util.OWLClassExpressionCollector;
import org.semanticweb.owlapi.util.OWLEntityCollector;
import org.semanticweb.owlapi.util.SimpleRenderer;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * A base for any OWLObject in ONT-API.
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class OWLObjectImpl implements OWLObject, Serializable {

    public static final Comparator<OWLObject> DEFAULT_COMPARATOR = Comparator.comparingInt(OWLObject::typeIndex)
            .thenComparing(o -> o.components().iterator(), OWLObjectImpl::compareIterators);

    /**
     * a convenience reference for an empty annotation set, saves on typing.
     */
    protected static final Set<OWLAnnotation> NO_ANNOTATIONS = Collections.emptySet();

    protected int hashCode = 0;

    /**
     * Check iterator contents for equality (sensitive to order).
     * Note: moved from {@link org.semanticweb.owlapi.util.OWLAPIStreamUtils} to control behaviour.
     *
     * @param set1 iterator to compare
     * @param set2 iterator to compare
     * @return true if the iterators have the same content, false otherwise.
     * @see org.semanticweb.owlapi.util.OWLAPIStreamUtils#equalIterators(Iterator, Iterator)
     */
    protected static boolean equalIterators(Iterator set1, Iterator set2) {
        while (set1.hasNext() && set2.hasNext()) {
            Object o1 = set1.next();
            Object o2 = set2.next();
            if (o1 instanceof Stream && o2 instanceof Stream) {
                if (!equalIterators(((Stream) o1).iterator(), ((Stream) o2).iterator())) {
                    return false;
                }
            } else {
                if (!o1.equals(o2)) {
                    return false;
                }
            }
        }
        return set1.hasNext() == set2.hasNext();
    }

    /**
     * Compare iterators element by element (sensitive to order).
     * Note: moved from {@link org.semanticweb.owlapi.util.OWLAPIStreamUtils} to control behaviour.
     *
     * @param set1 iterator to compare
     * @param set2 iterator to compare
     * @return int, negative value if {@code set1} comes before {@code set2},
     * positive value if {@code set2} comes before {@code set1},
     * {@code 0} if the two sets are equal or incomparable
     * @see org.semanticweb.owlapi.util.OWLAPIStreamUtils#compareIterators(Iterator, Iterator)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static int compareIterators(Iterator<?> set1, Iterator<?> set2) {
        while (set1.hasNext() && set2.hasNext()) {
            Object o1 = set1.next();
            Object o2 = set2.next();
            int res;
            if (o1 instanceof Stream && o2 instanceof Stream) {
                res = compareIterators(((Stream<?>) o1).iterator(), ((Stream<?>) o2).iterator());
            } else if (o1 instanceof Collection && o2 instanceof Collection) {
                res = compareIterators(((Collection<?>) o1).iterator(), ((Collection<?>) o2).iterator());
            } else if (o1 instanceof Comparable && o2 instanceof Comparable) {
                res = ((Comparable) o1).compareTo(o2);
            } else {
                throw new IllegalArgumentException(String.format("Incomparable types: " +
                                "'%s' with class %s, '%s' with class %s found while comparing iterators",
                        o1, o1.getClass(), o2, o2.getClass()));
            }
            if (res != 0) {
                return res;
            }
        }
        return Boolean.compare(set1.hasNext(), set2.hasNext());
    }

    /**
     * Creates a {@code Set}.
     *
     * @param values Array of {@link X}
     * @param <X>    anything
     * @return a {@code Set} of {@link X}
     */
    @SafeVarargs
    protected static <X> Set<X> createSet(X... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return addAnonymousIndividualsToSet(new TreeSet<>()).stream();
    }

    @Override
    public Stream<OWLEntity> signature() {
        return addSignatureEntitiesToSet(new TreeSet<>()).stream();
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity owlEntity) {
        return signature().anyMatch(o -> Objects.equals(o, owlEntity));
    }

    @Override
    public Stream<OWLClass> classesInSignature() {
        return signature().filter(OWLEntity::isOWLClass).map(OWLEntity::asOWLClass);
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        return signature().filter(OWLEntity::isOWLDataProperty).map(OWLEntity::asOWLDataProperty);
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        return signature().filter(OWLEntity::isOWLObjectProperty).map(OWLEntity::asOWLObjectProperty);
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        return signature().filter(OWLEntity::isOWLNamedIndividual).map(OWLEntity::asOWLNamedIndividual);
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        return signature().filter(OWLEntity::isOWLDatatype).map(OWLEntity::asOWLDatatype);
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        return signature().filter(OWLEntity::isOWLAnnotationProperty).map(OWLEntity::asOWLAnnotationProperty);
    }

    @Override
    public Stream<OWLClassExpression> nestedClassExpressions() {
        return accept(new OWLClassExpressionCollector()).stream();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLObject)) {
            return false;
        }
        OWLObject other = (OWLObject) obj;
        return typeIndex() == other.typeIndex()
                && hashCode() == other.hashCode()
                && equalIterators(components().iterator(), other.components().iterator());
    }

    @Override
    public int hashCode() {
        return hashCode == 0 ? hashCode = initHashCode() : hashCode;
    }

    @Override
    public int compareTo(@Nullable OWLObject o) {
        return DEFAULT_COMPARATOR.compare(this, Objects.requireNonNull(o));
    }

    /**
     * Returns string representation of this object.
     * This method does not use {@link org.semanticweb.owlapi.io.ToStringRenderer} for two reasons:
     * - to remove explicit usage of {@code javax.inject},
     * - {@code toString} is not the kind of method that should change its behavior in runtime.
     *
     * @return String, not {@code null}
     */
    @Override
    public String toString() {
        return new SimpleRenderer().render(this);
    }

    /**
     * Moved from uk.ac.manchester.cs.owl.owlapi.HasIncrementalSignatureGenerationSupport.
     *
     * @param entities entity set where entities will be added
     * @return the modified input entities
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/HasIncrementalSignatureGenerationSupport.java'>uk.ac.manchester.cs.owl.owlapi.HasIncrementalSignatureGenerationSupport</a>
     */
    protected Set<OWLEntity> addSignatureEntitiesToSet(Set<OWLEntity> entities) {
        accept(new OWLEntityCollector(entities));
        return entities;
    }

    /**
     * Moved from uk.ac.manchester.cs.owl.owlapi.HasIncrementalSignatureGenerationSupport.
     *
     * @param anons anonymous individuals set where individuals will be added
     * @return the modified input individuals
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/HasIncrementalSignatureGenerationSupport.java'>uk.ac.manchester.cs.owl.owlapi.HasIncrementalSignatureGenerationSupport</a>
     */
    protected Set<OWLAnonymousIndividual> addAnonymousIndividualsToSet(Set<OWLAnonymousIndividual> anons) {
        accept(new AnonymousIndividualCollector(anons));
        return anons;
    }

    /**
     * A utility class that visits axioms, class expressions etc. and accumulates
     * the anonymous individuals referred.
     *
     * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/AnonymousIndividualCollector.java'>uk.ac.manchester.cs.owl.owlapi.AnonymousIndividualCollector</a>
     * @since 1.2.0
     */
    protected static class AnonymousIndividualCollector extends AbstractCollectorEx<OWLAnonymousIndividual> {

        /**
         * @param anonsToReturn the set that will contain the anon individuals
         */
        public AnonymousIndividualCollector(Collection<OWLAnonymousIndividual> anonsToReturn) {
            super(anonsToReturn);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Collection<OWLAnonymousIndividual> visit(OWLAnonymousIndividual individual) {
            objects.add(individual);
            return objects;
        }
    }
}
