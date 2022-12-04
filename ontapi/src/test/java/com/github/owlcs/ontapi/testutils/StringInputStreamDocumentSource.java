/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.testutils;

import com.github.owlcs.ontapi.OntFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSourceBase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The {@link org.semanticweb.owlapi.io.OWLOntologyDocumentSource DocumentSource} providing InputStream.
 * <p>
 * Created by @szuev on 02.02.2018.
 *
 * @see org.semanticweb.owlapi.io.StringDocumentSource
 */
public class StringInputStreamDocumentSource extends OWLOntologyDocumentSourceBase {
    private final String txt;
    private final Charset charset;

    public StringInputStreamDocumentSource(String txt, OntFormat format) {
        this(txt, format.createOwlFormat());
    }

    public StringInputStreamDocumentSource(String txt, OWLDocumentFormat format) {
        this(txt, IRI.create("string:" + format.getClass().getSimpleName()), format, null, StandardCharsets.UTF_8);
    }

    public StringInputStreamDocumentSource(String txt, IRI doc, OWLDocumentFormat format, String mime, Charset charset) {
        super(doc, format, mime);
        this.txt = txt;
        this.charset = charset;
    }

    @Override
    public Optional<InputStream> getInputStream() {
        return Optional.of(new ByteArrayInputStream(txt.getBytes(charset)));
    }
}
