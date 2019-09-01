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

package ru.avicomp.ontapi.internal.objects;

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * A base for {@link ONTResourceImpl ONT Resource} (a {@link org.apache.jena.graph.Node node} based object)
 * and for {@link ONTBaseTripleImpl ONT Triple} (a {@link org.apache.jena.graph.Triple triple} based object).
 * Created by @ssz on 31.08.2019.
 *
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
abstract class ONTObjectImpl extends OWLObjectImpl implements ONTComposite {

    @Override
    public final boolean containsEntityInSignature(@Nullable OWLEntity entity) {
        return entity != null && containsEntity(entity);
    }

    /**
     * Answers {@code true} iff the given entity is present in the object's signature.
     *
     * @param entity {@link OWLEntity}, not {@code null}
     * @return boolean
     */
    public boolean containsEntity(OWLEntity entity) {
        if (entity.isOWLClass()) {
            return containsNamedClass(entity.asOWLClass());
        }
        if (entity.isOWLNamedIndividual()) {
            return containsNamedIndividual(entity.asOWLNamedIndividual());
        }
        if (entity.isOWLDatatype()) {
            return containsDatatype(entity.asOWLDatatype());
        }
        if (entity.isOWLObjectProperty()) {
            return containsObjectProperty(entity.asOWLObjectProperty());
        }
        if (entity.isOWLDataProperty()) {
            return containsDataProperty(entity.asOWLDataProperty());
        }
        if (entity.isOWLAnnotationProperty()) {
            return containsAnnotationProperty(entity.asOWLAnnotationProperty());
        }
        return false;
    }

    /**
     * Answers {@code true} iff the given OWL class is present in the object's signature.
     *
     * @param clazz {@link OWLClass}, not {@code null}
     * @return boolean
     */
    public boolean containsNamedClass(OWLClass clazz) {
        return canContainClassExpressions() ? accept(ONTCollectors.forClass(clazz)) : false;
    }

    /**
     * Answers {@code true} iff the given OWL named individual is present in the object's signature.
     *
     * @param individual {@link OWLNamedIndividual}, not {@code null}
     * @return boolean
     */
    public boolean containsNamedIndividual(OWLNamedIndividual individual) {
        return canContainNamedIndividuals() ? accept(ONTCollectors.forNamedIndividual(individual)) : false;
    }

    /**
     * Answers {@code true} iff the given OWL named data range (datatype) is present in the object's signature.
     *
     * @param datatype {@link OWLDatatype}, not {@code null}
     * @return boolean
     */
    public boolean containsDatatype(OWLDatatype datatype) {
        return canContainDatatypes() ? accept(ONTCollectors.forDatatype(datatype)) : false;
    }

    /**
     * Answers {@code true} iff the given OWL named object property expression ({@code OWLObjectProperty})
     * is present in the object's signature.
     *
     * @param property {@link OWLObjectProperty}, not {@code null}
     * @return boolean
     */
    public boolean containsObjectProperty(OWLObjectProperty property) {
        return canContainObjectProperties() ? accept(ONTCollectors.forObjectProperty(property)) : false;
    }

    /**
     * Answers {@code true} iff the given OWL data property is present in the object's signature.
     *
     * @param property {@link OWLDataProperty}, not {@code null}
     * @return boolean
     */
    public boolean containsDataProperty(OWLDataProperty property) {
        return canContainDataProperties() ? accept(ONTCollectors.forDataProperty(property)) : false;
    }

    /**
     * Answers {@code true} iff the given OWL annotation property is present in the object's signature.
     *
     * @param property {@link OWLAnnotationProperty}, not {@code null}
     * @return boolean
     */
    public boolean containsAnnotationProperty(OWLAnnotationProperty property) {
        return canContainAnnotationProperties() ? accept(ONTCollectors.forAnnotationProperty(property)) : false;
    }

    @Override
    public Set<OWLEntity> getSignatureSet() {
        return accept(ONTCollectors.forEntities(createSortedSet()));
    }

    @Override
    public Set<OWLClassExpression> getClassExpressionSet() {
        return canContainClassExpressions() ? accept(ONTCollectors.forClassExpressions(new HashSet<>())) : createSet();
    }

    @Override
    public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        return canContainAnonymousIndividuals() ?
                accept(ONTCollectors.forAnonymousIndividuals(createSortedSet())) : createSet();
    }

    @Override
    public Set<OWLClass> getNamedClassSet() {
        return canContainNamedClasses() ? accept(ONTCollectors.forClasses(createSortedSet())) : createSet();
    }

    @Override
    public Set<OWLNamedIndividual> getNamedIndividualSet() {
        return canContainNamedIndividuals() ?
                accept(ONTCollectors.forNamedIndividuals(createSortedSet())) : createSet();
    }

    @Override
    public Set<OWLDatatype> getDatatypeSet() {
        return canContainDatatypes() ? accept(ONTCollectors.forDatatypes(createSortedSet())) : createSet();
    }

    @Override
    public Set<OWLObjectProperty> getObjectPropertySet() {
        return canContainObjectProperties() ?
                accept(ONTCollectors.forObjectProperties(createSortedSet())) : createSet();
    }

    @Override
    public Set<OWLDataProperty> getDataPropertySet() {
        return canContainDataProperties() ? accept(ONTCollectors.forDataProperties(createSortedSet())) : createSet();
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return canContainAnnotationProperties() ?
                accept(ONTCollectors.forAnnotationProperties(createSortedSet())) : createSet();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("Suspicious method call. " +
                "Serialization is unsupported for " + getClass().getSimpleName() + ".");
    }

    private void readObject(ObjectInputStream in) throws Exception {
        throw new NotSerializableException("Suspicious method call. " +
                "Deserialization is unsupported for " + getClass().getSimpleName() + ".");
    }
}
