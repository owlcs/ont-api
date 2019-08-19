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
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * A base for any OWLObject in ONT-API.
 *
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
@ParametersAreNonnullByDefault
@SuppressWarnings("WeakerAccess")
public abstract class OWLObjectImpl implements OWLObject, Serializable {

    public static final Comparator<OWLObject> DEFAULT_COMPARATOR = Comparator.comparingInt(OWLObject::typeIndex)
            .thenComparing(o -> o.components().iterator(), OWLObjectImpl::compareIterators);

    /**
     * a convenience reference for an empty annotation set, saves on typing.
     */
    protected static final Set<OWLAnnotation> NO_ANNOTATIONS = Collections.emptySet();

    protected int hashCode;

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
     * @param values Array of {@link X}s without {@code null}s
     * @param <X>    anything
     * @return a {@code Set} of {@link X}s
     */
    @SafeVarargs
    protected static <X> Set<X> createSet(X... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    /**
     * Creates an immutable singleton {@code Set}.
     *
     * @param value {@link X}, not {@code null}
     * @param <X>   anything
     * @return a {@code Set} with {@link X}-value
     */
    protected static <X> Set<X> createSet(X value) {
        return Collections.singleton(value);
    }

    /**
     * Creates an empty immutable {@code Set}.
     *
     * @param <X> anything
     * @return a {@code Set} of {@link X}
     */
    protected static <X> Set<X> createSet() {
        return Collections.emptySet();
    }

    /**
     * Creates an empty sorted {@code Set}.
     *
     * @param <X> subtype of {@link Comparable}
     * @return a {@code Set}
     */
    protected <X extends Comparable> Set<X> createSortedSet() {
        return new TreeSet<>();
    }

    /**
     * Creates an empty sorted {@code Set} with the given {@code comparator}.
     *
     * @param comparator {@link Comparator} for {@link X}
     * @param <X>        anything
     * @return a {@code Set}
     */
    protected <X> Set<X> createSortedSet(Comparator<X> comparator) {
        return new TreeSet<>(comparator);
    }

    @Override
    public boolean containsEntityInSignature(@Nullable OWLEntity entity) {
        return getSignatureSet().contains(entity);
    }

    @Override
    public final Stream<OWLEntity> signature() {
        return getSignatureSet().stream();
    }

    @Override
    public final Stream<OWLClass> classesInSignature() {
        return getNamedClassSet().stream();
    }

    @Override
    public final Stream<OWLClassExpression> nestedClassExpressions() {
        return getClassExpressionSet().stream();
    }

    @Override
    public final Stream<OWLNamedIndividual> individualsInSignature() {
        return getNamedIndividualSet().stream();
    }

    @Override
    public final Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return getAnonymousIndividualSet().stream();
    }

    @Override
    public final Stream<OWLDatatype> datatypesInSignature() {
        return getDatatypeSet().stream();
    }

    @Override
    public final Stream<OWLDataProperty> dataPropertiesInSignature() {
        return getDataPropertySet().stream();
    }

    @Override
    public final Stream<OWLObjectProperty> objectPropertiesInSignature() {
        return getObjectPropertySet().stream();
    }

    @Override
    public final Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        return getAnnotationPropertySet().stream();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLClass> getClassesInSignature() {
        return getNamedClassSet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLNamedIndividual> getIndividualsInSignature() {
        return getNamedIndividualSet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLAnonymousIndividual> getAnonymousIndividuals() {
        return getAnonymousIndividualSet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLDatatype> getDatatypesInSignature() {
        return getDatatypeSet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLObjectProperty> getObjectPropertiesInSignature() {
        return getObjectPropertySet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLDataProperty> getDataPropertiesInSignature() {
        return getDataPropertySet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLAnnotationProperty> getAnnotationPropertiesInSignature() {
        return getAnnotationPropertySet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLEntity> getSignature() {
        return getSignatureSet();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final Set<OWLClassExpression> getNestedClassExpressions() {
        return getClassExpressionSet();
    }

    protected Set<OWLEntity> getSignatureSet() {
        return (Set<OWLEntity>) accept(new OWLEntityCollector(createSortedSet()));
    }

    protected Set<OWLClass> getNamedClassSet() {
        Set<OWLClass> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLClass>(res) {
            @Override
            public Collection<OWLClass> visit(OWLClass clazz) {
                objects.add(clazz);
                return objects;
            }
        });
        return res;
    }

    protected Set<OWLClassExpression> getClassExpressionSet() {
        return (Set<OWLClassExpression>) accept(new OWLClassExpressionCollector());
    }

    protected Set<OWLNamedIndividual> getNamedIndividualSet() {
        Set<OWLNamedIndividual> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLNamedIndividual>(res) {
            @Override
            public Collection<OWLNamedIndividual> visit(OWLNamedIndividual individual) {
                objects.add(individual);
                return objects;
            }
        });
        return res;
    }

    protected Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        Set<OWLAnonymousIndividual> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLAnonymousIndividual>(res) {
            @Override
            public Collection<OWLAnonymousIndividual> visit(OWLAnonymousIndividual individual) {
                objects.add(individual);
                return objects;
            }
        });
        return res;
    }

    protected Set<OWLDatatype> getDatatypeSet() {
        Set<OWLDatatype> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLDatatype>(res) {
            @Override
            public Collection<OWLDatatype> visit(OWLDatatype datatype) {
                objects.add(datatype);
                return objects;
            }
        });
        return res;
    }

    protected Set<OWLObjectProperty> getObjectPropertySet() {
        Set<OWLObjectProperty> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLObjectProperty>(res) {
            @Override
            public Collection<OWLObjectProperty> visit(OWLObjectProperty property) {
                objects.add(property);
                return objects;
            }
        });
        return res;
    }

    protected Set<OWLDataProperty> getDataPropertySet() {
        Set<OWLDataProperty> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLDataProperty>(res) {
            @Override
            public Collection<OWLDataProperty> visit(OWLDataProperty property) {
                objects.add(property);
                return objects;
            }
        });
        return res;
    }

    protected Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        Set<OWLAnnotationProperty> res = createSortedSet();
        accept(new AbstractCollectorEx<OWLAnnotationProperty>(res) {
            @Override
            public Collection<OWLAnnotationProperty> visit(OWLAnnotationProperty property) {
                objects.add(property);
                return objects;
            }
        });
        return res;
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

    /**
     * Answers {@code true} if and only if this object and the specified one are definitely not equal.
     * Note that the method returns {@code false} in any other case, so the objects might still be equal.
     * This operation is cheap: the method {@link OWLObject#initHashCode()} is not called.
     *
     * @param other {@link OWLObjectImpl}, not {@code null}
     * @return boolean - {@code true} if objects have different pre-calculated hash-codes, {@code false} otherwise
     */
    protected boolean notSame(OWLObjectImpl other) {
        return hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode;
    }

    @Override
    public final int hashCode() {
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

}
