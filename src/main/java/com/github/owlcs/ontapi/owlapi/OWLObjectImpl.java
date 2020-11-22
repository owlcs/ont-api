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
package com.github.owlcs.ontapi.owlapi;

import com.github.owlcs.ontapi.jena.utils.Iter;
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
    protected static final List<OWLAnnotation> NO_ANNOTATIONS = Collections.emptyList();

    protected int hashCode;

    /**
     * Creates a {@code Set}.
     *
     * @param values Array of {@link X}s without {@code null}s
     * @param <X>    anything
     * @return a modifiable {@code Set} of {@link X}s
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
     * @return an unmodifiable {@code Set} with {@link X}-value
     */
    protected static <X> Set<X> createSet(X value) {
        return Collections.singleton(value);
    }

    /**
     * Creates an empty immutable {@code Set}.
     *
     * @param <X> anything
     * @return an unmodifiable {@code Set} of {@link X}
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
    public static <X extends Comparable<?>> Set<X> createSortedSet() {
        return new TreeSet<>();
    }

    /**
     * Creates an empty sorted {@code Set} with the given {@code comparator}.
     *
     * @param comparator {@link Comparator} for {@link X}
     * @param <X>        anything
     * @return a {@code Set}
     */
    public static <X> Set<X> createSortedSet(Comparator<X> comparator) {
        return new TreeSet<>(comparator);
    }

    /**
     * Prepares the collection to be used as internal store inside an {@link OWLObject}.
     *
     * @param input a nonnull {@code Collection} of {@link X}, without {@code null}s
     * @param msg   the error message if the input is {@code null}
     * @param <X>   anything
     * @return an unmodifiable sorted, distinct, nonnull {@code List} of {@link X}s
     * @throws NullPointerException if {@code input} or any its element is {@code null}
     */
    protected static <X> List<X> toContentList(Collection<? extends X> input, String msg) {
        return forOutput(Objects.requireNonNull(input, msg).stream()).collect(Iter.toUnmodifiableList());
    }

    /**
     * Ensures that the given {@code Stream} is distinct and sorted, and does not contain {@code null}s.
     * For internal usage only.
     *
     * @param stream a {@code Stream} of {@link X}, not {@code null}
     * @param <X>    anything
     * @return a distinct, sorted and nonnull {@code Stream}
     */
    protected static <X> Stream<X> forOutput(Stream<X> stream) {
        return stream.map(Objects::requireNonNull).sorted().distinct();
    }

    /**
     * A convenience method for implementation that returns a set containing the annotations on this axiom
     * plus the annotations in the specified set.
     * For internal usage only.
     *
     * @param withAnnotations {@link HasAnnotations}, not {@code null}
     * @param other           a {@code Stream} of annotations to append to the annotations
     *                        of the object {@code withAnnotations}, not {@code null}
     * @return an unmodifiable sorted and distinct {@code List} of {@link OWLAnnotation annotation}s
     */
    protected static List<OWLAnnotation> mergeAnnotations(HasAnnotations withAnnotations,
                                                          Stream<OWLAnnotation> other) {
        return forOutput(Stream.concat(other, withAnnotations.annotations())).collect(Iter.toUnmodifiableList());
    }

    /**
     * Transforms the given collection of annotations to the form that is required by OWL-API:
     * an internal annotations collection must be distinct, nonnull and sorted.
     *
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s
     * @return an unmodifiable sorted and distinct {@code List} of {@link OWLAnnotation annotation}s
     */
    protected static List<OWLAnnotation> prepareAnnotations(Collection<OWLAnnotation> annotations) {
        if (annotations == NO_ANNOTATIONS) {
            return NO_ANNOTATIONS;
        }
        if (Objects.requireNonNull(annotations, "Annotations cannot be null").isEmpty()) {
            return NO_ANNOTATIONS;
        }
        return forOutput(annotations.stream()).collect(Iter.toUnmodifiableList());
    }

    /**
     * Checks the iterator contents for equality (sensitive to order).
     * This method has been moved here from the {@link org.semanticweb.owlapi.util.OWLAPIStreamUtils}
     * to better control behaviour.
     *
     * @param left  {@code Iterator} to compare, not {@code null}
     * @param right {@code Iterator} to compare, not {@code null}
     * @return {@code true} if the iterators have the same content, {@code false} otherwise
     * @see org.semanticweb.owlapi.util.OWLAPIStreamUtils#equalIterators(Iterator, Iterator)
     */
    protected static boolean equalIterators(Iterator<?> left, Iterator<?> right) {
        while (left.hasNext() && right.hasNext()) {
            Object a = left.next();
            Object b = right.next();
            if (a instanceof Stream && b instanceof Stream) {
                if (!equalStreams(((Stream<?>) a), ((Stream<?>) b))) {
                    return false;
                }
            } else {
                if (!a.equals(b)) {
                    return false;
                }
            }
        }
        return left.hasNext() == right.hasNext();
    }

    /**
     * Checks streams for equality (sensitive to order)
     *
     * @param left  {@code Stream} to compare, not {@code null}
     * @param right {@code Stream} to compare, not {@code null}
     * @return {@code true} if the streams have the same content, {@code false} otherwise
     * @see org.semanticweb.owlapi.util.OWLAPIStreamUtils#equalStreams(Stream, Stream)
     */
    protected static boolean equalStreams(Stream<?> left, Stream<?> right) {
        return equalIterators(left.iterator(), right.iterator());
    }

    /**
     * Compares iterators element by element (sensitive to order).
     * It was moved from the {@link org.semanticweb.owlapi.util.OWLAPIStreamUtils} to control behaviour.
     *
     * @param left {@code Iterator} to compare, not {@code null}
     * @param right {@code Iterator} to compare, not {@code null}
     * @return {@code int}, a negative value if {@code left} comes before {@code right},
     * a positive value if {@code left} comes before {@code right},
     * or {@code 0} if the two sets are equal or incomparable
     * @see org.semanticweb.owlapi.util.OWLAPIStreamUtils#compareIterators(Iterator, Iterator)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static int compareIterators(Iterator<?> left, Iterator<?> right) {
        while (left.hasNext() && right.hasNext()) {
            Object o1 = left.next();
            Object o2 = right.next();
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
        return Boolean.compare(left.hasNext(), right.hasNext());
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

    /**
     * Gets the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of entities that represents the signature of this object
     */
    protected Set<OWLEntity> getSignatureSet() {
        return (Set<OWLEntity>) accept(new OWLEntityCollector(createSortedSet()));
    }

    /**
     * Gets the classes in the signature of this object.
     * The returned set is a subset of the signature, and is not backed by the signature;
     * it is a modifiable collection and changes are not reflected by the signature.
     *
     * @return a modifiable sorted {@code Set} containing the classes
     * that are in the signature of this object
     */
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

    /**
     * Gets all of the nested (includes top level) class expressions that are used in this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable unordered {@code Set} of {@link OWLClassExpression}s
     * that represent the nested class expressions used in this object
     */
    protected Set<OWLClassExpression> getClassExpressionSet() {
        return (Set<OWLClassExpression>) accept(new OWLClassExpressionCollector());
    }

    /**
     * Gets all of the individuals that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} containing the individuals that are in the signature of this object
     */
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

    /**
     * Gets the anonymous individuals occurring in this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the anonymous individuals
     */
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

    /**
     * Gets the datatypes that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the datatypes that are in the signature of this object
     */
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

    /**
     * Obtains the (named) object properties that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the object properties that are in the signature of this object
     */
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

    /**
     * Obtains the data properties that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the data properties that are in the signature of this object
     */
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

    /**
     * Obtains the annotation properties that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the annotation properties that are in the signature of this object
     */
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

    @Override
    public String toString() {
        return new SimpleRenderer().render(this);
    }

}
