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

import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.jena.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link HashMap} based implementation for debug.
 * Created by @ssz on 09.09.2018.
 */
public class MapDataFactory extends NoCacheDataFactory {
    private Map<OntClass, ONTObject<OWLClass>> classes = new HashMap<>();
    private Map<OntDT, ONTObject<OWLDatatype>> datatypes = new HashMap<>();
    private Map<OntNAP, ONTObject<OWLAnnotationProperty>> annotationProperties = new HashMap<>();
    private Map<OntNDP, ONTObject<OWLDataProperty>> datatypeProperties = new HashMap<>();
    private Map<OntNOP, ONTObject<OWLObjectProperty>> objectProperties = new HashMap<>();
    private Map<OntIndividual.Named, ONTObject<OWLNamedIndividual>> individuals = new HashMap<>();
    private Map<Literal, ONTObject<OWLLiteral>> literals = new HashMap<>();

    public MapDataFactory(DataFactory factory) {
        super(factory);
    }

    @Override
    public void clear() {
        classes.clear();
        datatypes.clear();
        annotationProperties.clear();
        objectProperties.clear();
        datatypeProperties.clear();
        individuals.clear();
        literals.clear();
    }

    @Override
    public ONTObject<OWLClass> get(OntClass ce) {
        return classes.computeIfAbsent(ce, super::get);
    }

    @Override
    public ONTObject<OWLDatatype> get(OntDT dt) {
        return datatypes.computeIfAbsent(dt, super::get);
    }

    @Override
    public ONTObject<OWLAnnotationProperty> get(OntNAP nap) {
        return annotationProperties.computeIfAbsent(nap, super::get);
    }

    @Override
    public ONTObject<OWLDataProperty> get(OntNDP ndp) {
        return datatypeProperties.computeIfAbsent(ndp, super::get);
    }

    @Override
    public ONTObject<OWLObjectProperty> get(OntNOP nop) {
        return objectProperties.computeIfAbsent(nop, super::get);
    }

    @Override
    public ONTObject<OWLLiteral> get(Literal l) {
        return literals.computeIfAbsent(l, super::get);
    }

    @Override
    public ONTObject<OWLNamedIndividual> get(OntIndividual.Named i) {
        return individuals.computeIfAbsent(i, super::get);
    }
}
