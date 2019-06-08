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

package ru.avicomp.ontapi.jena.model;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * A technical interface to access {@link P} properties from a []-list on
 * on predicate {@link ru.avicomp.ontapi.jena.vocabulary.OWL#onProperties owl:onProperties}.
 * <p>
 * Created by @ssz on 09.05.2019.
 *
 * @param <P> - any subtype of {@link OntDOP} in general case, but in the current model it can only be {@link OntNDP}
 * @see SetProperties
 * @since 1.4.0
 */
interface HasProperties<P extends OntDOP> extends HasRDFNodeList<P>, HasProperty<P> {

    /**
     * Gets the first property from {@code owl:onProperties} []-list.
     * Currently in OWL2, a []-list from n-ary Restrictions may contain one and only one (data) property.
     *
     * @return {@link P}
     * @see OntDR#arity()
     */
    @Override
    default P getProperty() {
        return getList().first().orElseThrow(OntJenaException.IllegalState::new);
    }
}
