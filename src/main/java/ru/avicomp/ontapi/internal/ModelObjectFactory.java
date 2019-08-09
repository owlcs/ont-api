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
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.*;

/**
 * An Extended Internal Object Factory impl which maps {@link ru.avicomp.ontapi.jena.model.OntObject}
 * to {@link org.semanticweb.owlapi.model.OWLObject} directly having no cache.
 * Unlike the base class, each returned instance is associated with a concrete model.
 * <p>
 * Created by @ssz on 07.08.2019.
 *
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ModelObjectFactory extends NoCacheObjectFactory {

    public ModelObjectFactory(DataFactory factory) {
        super(factory);
    }

    @Override
    public ONTObject<OWLClass> get(OntClass ce) {
        return new ONTClassImpl(ce.getURI(), ce.getModel());
    }

    @Override
    public ONTObject<OWLAnonymousIndividual> get(OntIndividual.Anonymous i) {
        return new ONTAnonymousIndividualImpl(i.asNode().getBlankNodeId(), i.getModel());
    }

    @Override
    public ONTObject<OWLNamedIndividual> get(OntIndividual.Named i) {
        return new ONTNamedIndividualImpl(i.getURI(), i.getModel());
    }

    @Override
    public ONTObject<OWLDatatype> get(OntDT dt) {
        return new ONTDatatypeImpl(dt.getURI(), dt.getModel());
    }

    @Override
    public ONTObject<OWLAnnotationProperty> get(OntNAP p) {
        return new ONTAnnotationPropertyImpl(p.getURI(), p.getModel());
    }

    @Override
    public ONTObject<OWLObjectProperty> get(OntNOP p) {
        return new ONTObjectPropertyImpl(p.getURI(), p.getModel());
    }

    @Override
    public ONTObject<OWLDataProperty> get(OntNDP p) {
        return new ONTDataPropertyImpl(p.getURI(), p.getModel());
    }

    @Override
    public ONTObject<OWLLiteral> get(Literal literal) {
        return new ONTLiteralImpl(literal.asNode().getLiteral(), (OntGraphModel) literal.getModel());
    }
}
