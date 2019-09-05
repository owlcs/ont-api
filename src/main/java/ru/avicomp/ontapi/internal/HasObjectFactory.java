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

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * An abstract {@link InternalObjectFactory} holder.
 * Created by @ssz on 07.08.2019.
 *
 * @since 1.4.3
 */
public interface HasObjectFactory {

    /**
     * Returns the {@code InternalDataFactory}, that is a helper (possibly, with cache) to read OWL-API objects.
     *
     * @return {@link InternalObjectFactory}
     */
    InternalObjectFactory getObjectFactory();

    /**
     * Gets the ONT-API Object Factory from the model's internals if possible, otherwise throws an exception.
     *
     * @param model {@link OntGraphModel}, not {@code null}
     * @return {@link InternalObjectFactory}
     * @throws OntApiException.IllegalArgument in case the model does not provide the object factory
     */
    static InternalObjectFactory getObjectFactory(OntGraphModel model) {
        if (model instanceof HasObjectFactory) {
            return ((HasObjectFactory) model).getObjectFactory();
        }
        throw new OntApiException.IllegalArgument("The given model has no object factory");
    }
}
