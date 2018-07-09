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

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * domain for annotation property.
 * see {@link AbstractPropertyDomainTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class AnnotationPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLAnnotationPropertyDomainAxiom, OntNAP> {
    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLAnnotationPropertyDomainAxiom} axiom.
     *
     * @param model {@link OntGraphModel}
     * @return {@link OntStatement}
     */
    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        OntLoaderConfiguration conf = getConfig(model).loaderConfig();
        if (!conf.isLoadAnnotationAxioms()) return Stream.empty();
        return super.statements(model)
                .filter(s -> s.getObject().isURIResource())
                .filter(s -> ReadHelper.testAnnotationAxiomOverlaps(s, conf, AxiomType.OBJECT_PROPERTY_DOMAIN, AxiomType.DATA_PROPERTY_DOMAIN));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return super.testStatement(statement) && statement.getObject().isURIResource();
    }

    @Override
    public ONTObject<OWLAnnotationPropertyDomainAxiom> toAxiom(OntStatement statement) {
        InternalDataFactory reader = getDataFactory(statement.getModel());
        ONTObject<OWLAnnotationProperty> p = reader.get(statement.getSubject().as(getView()));
        ONTObject<IRI> d = reader.asIRI(statement.getObject().as(OntObject.class));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.get(statement);
        OWLAnnotationPropertyDomainAxiom res = reader.getOWLDataFactory()
                .getOWLAnnotationPropertyDomainAxiom(p.getObject(), d.getObject(), ONTObject.extract(annotations));
        return ONTObject.create(res, statement).append(annotations).append(p).append(d);
    }
}
