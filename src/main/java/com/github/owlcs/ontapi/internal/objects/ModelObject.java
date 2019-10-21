/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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

import org.semanticweb.owlapi.model.OWLObject;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;

/**
 * Represents an {@link ONTObject} attached to the {@link OntGraphModel model}.
 * Created by @ssz on 23.09.2019.
 *
 * @param <X> subtype of {@link OWLObject}
 */
public interface ModelObject<X extends OWLObject> extends ONTObject<X> {

    /**
     * Returns the model to which this object is attached.
     *
     * @return {@link OntGraphModel}
     */
    OntGraphModel getModel();

    /**
     * Answers with an object that equals to the {@link #getOWLObject()},
     * but without any model information inside.
     * <p>
     * Such unattached object can be used in whatever way,
     * while the object {@link #getOWLObject()} requires more attention:
     * it will not allow GC to dispose of the model,
     * as there is a strong reference (or a facility to get such reference) to the model inside that object.
     *
     * @return {@link X}
     * @see ONTObject#getOWLObject()
     */
    @FactoryAccessor
    X eraseModel();

}
