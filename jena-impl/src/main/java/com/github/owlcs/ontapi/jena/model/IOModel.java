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

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.Model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * A technical interface that describes model I/O operations.
 * Contains overridden read/write methods inherited from {@link Model}.
 * Created by @ssz on 15.03.2020.
 *
 * @param <R> - subtype of {@link Model}, the model to return
 * @see <a href="http://jena.apache.org/documentation/io/index.html">"Reading and Writing RDF in Apache Jena"</a>
 */
interface IOModel<R extends Model> extends Model {

    @Override
    R read(String url);

    @Override
    R read(InputStream in, String base);

    @Override
    R read(InputStream in, String base, String lang);

    @Override
    R read(Reader reader, String base);

    @Override
    R read(String url, String lang);

    @Override
    R read(Reader reader, String base, String lang);

    @Override
    R read(String url, String base, String lang);

    @Override
    R write(Writer writer);

    @Override
    R write(Writer writer, String lang);

    @Override
    R write(Writer writer, String lang, String base);

    @Override
    R write(OutputStream out);

    @Override
    R write(OutputStream out, String lang);

    @Override
    R write(OutputStream out, String lang, String base);

}
