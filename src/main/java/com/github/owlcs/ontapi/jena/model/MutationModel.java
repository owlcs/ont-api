/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.*;

import java.util.List;

/**
 * A technical interface that describes model modify operations.
 * Contains overridden methods inherited from {@link Model} and {@link ModelCon}:
 * Created by @ssz on 15.03.2020.
 *
 * @param <R> - a subtype of {@link Model}, the type to return
 */
interface MutationModel<R extends Model> extends Model {

    @Override
    R add(Statement s);

    @Override
    R add(Resource s, Property p, RDFNode o);

    @Override
    R add(Model m);

    @Override
    R add(StmtIterator it);

    @Override
    R add(Statement[] statements);

    @Override
    R add(List<Statement> statements);

    @Override
    R remove(Statement s);

    @Override
    R remove(Resource s, Property p, RDFNode o);

    @Override
    R remove(Model m);

    @Override
    R remove(StmtIterator it);

    @Override
    R removeAll(Resource s, Property p, RDFNode o);

    @Override
    R remove(Statement[] statements);

    @Override
    R remove(List<Statement> statements);

    @Override
    R removeAll();

    @Override
    R addLiteral(Resource s, Property p, boolean v);

    @Override
    R addLiteral(Resource s, Property p, long v);

    @Override
    R addLiteral(Resource s, Property p, int v);

    @Override
    R addLiteral(Resource s, Property p, char v);

    @Override
    R addLiteral(Resource s, Property p, float v);

    @Override
    R addLiteral(Resource s, Property p, double v);

    @Override
    R addLiteral(Resource s, Property p, Literal o);

    @Override
    R add(Resource s, Property p, String lex);

    @Override
    R add(Resource s, Property p, String lex, RDFDatatype datatype);

    @Override
    R add(Resource s, Property p, String o, boolean wellFormed);

    @Override
    R add(Resource s, Property p, String lex, String lang);
}
