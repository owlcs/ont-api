/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.internal.PrefixMappingRenderer;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.owlapi.objects.AnonymousIndividualImpl;
import com.github.owlcs.ontapi.owlapi.objects.LiteralImpl;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.util.EscapeUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class ONTPrefixMappingRenderer extends PrefixMappingRenderer {

    public ONTPrefixMappingRenderer(PrefixMapping pm) {
        super(pm);
    }

    @Override
    protected String shortForm(OWLEntity entity) {
        return shortForm(((ONTEntityImpl<?>) entity).getURI());
    }

    @Override
    public void visit(OWLAnonymousIndividual individual) {
        sb.append(((AnonymousIndividualImpl) individual).getBlankNodeId().getLabelString());
    }

    @Override
    public void visit(OWLLiteral literal) {
        visit((ONTLiteralImpl) literal);
    }

    public void visit(LiteralImpl node) {
        String txt = EscapeUtils.escapeString(node.getLiteral());
        sb.append('"').append(txt).append('"');
        String dt = node.getDatatypeURI();
        if (RDF.PlainLiteral.getURI().equals(dt) || RDF.langString.getURI().equals(dt)) {
            if (node.hasLang()) {
                sb.append('@').append(node.getLang());
            }
        } else if (!XSD.xstring.getURI().equals(dt)) {
            sb.append("^^");
            node.getDatatype().accept(this);
        }
    }
}
