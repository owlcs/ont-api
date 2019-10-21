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

package com.github.owlcs.ontapi;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A helper to work with reflection.
 * <p>
 * Created by @ssz on 11.09.2018.
 */
class ReflectionUtils {

    /**
     * Finds a static method by the class and its name.
     *
     * @param clazz {@link Class}
     * @param name  String name
     * @return {@link Method}
     * @throws OntApiException if no method is found
     */
    static Method findStaticMethod(Class<?> clazz, String name) throws OntApiException {
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new OntApiException("Can't find method '" + name + "' in " + clazz, e);
        }
    }

    /**
     * Returns a proxy instance that implements {@code interfaceType} by dispatching method
     * invocations to {@code handler}.
     * The class loader of {@code interfaceType} will be used to define the proxy class.
     * To implement multiple interfaces or specify a class loader, use {@link Proxy#newProxyInstance}.
     *
     * @param interfaceType {@link Class}
     * @param handler       {@link InvocationHandler}
     * @param <R>           any class-type
     * @return instance of the type {@link R}
     */
    static <R> R newProxy(Class<R> interfaceType, InvocationHandler handler) {
        Objects.requireNonNull(handler);
        if (!Objects.requireNonNull(interfaceType).isInterface())
            throw new OntApiException(interfaceType + " is not an interface");
        Object object = Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class<?>[]{interfaceType}, handler);
        return interfaceType.cast(object);
    }

    /**
     * Creates an instance.
     *
     * @param interfaceType     {@link Class}, a type of class, not {@code null}
     * @param className         String, full class-path, not {@code null}
     * @param constructorParams {@link ListMultimap} collection of parameters,
     *                          class-type as key, object as value, not {@code null}
     * @param <R>               generic class-type
     * @return new instance of {@link R}, not {@code null}
     * @throws OntApiException if no class found or it does not meet expectations
     */
    @SuppressWarnings("unchecked")
    static <R> R newInstance(Class<? extends R> interfaceType,
                             String className,
                             LinkedListMultimap<Class<?>, Object> constructorParams) {
        Class<? extends R> clazz = getClass(interfaceType, className);
        String name = MessageFormat.format("{0}({1})", clazz.getName(),
                constructorParams.keys().stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
        Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor(constructorParams.keys().toArray(new Class[constructorParams.size()]));
        } catch (NoSuchMethodException e) {
            throw new OntApiException("Unable to find constructor " + name, e);
        }
        try {
            return (R) constructor.newInstance(constructorParams.values().toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            throw new OntApiException("Can't init " + name);
        }
    }

    /**
     * Creates an instance of the specified class.
     *
     * @param type {@link Class}, not {@code null}
     * @param <R>  generic class-type
     * @return instance of {@code T}, not {@code null}
     * @throws OntApiException in case no possible to create instance
     */
    static <R> R newInstance(Class<R> type) throws OntApiException {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new OntApiException("Can't create instance of " + type.getName(), e);
        }
    }

    /**
     * Finds class by path and base type.
     *
     * @param interfaceType {@link Class} the base type, not {@code null}
     * @param className     full class-name path, not {@code null}
     * @param <R>           class-type
     * @return a {@link Class} or {@code null} in case nothing was found
     * @throws OntApiException if wrong arguments
     */
    @SuppressWarnings("unchecked")
    static <R> Class<R> findClass(Class<R> interfaceType, String className) throws OntApiException {
        Class<?> res;
        try {
            res = getClass(className);
        } catch (OntApiException e) {
            return null;
        }
        if (interfaceType.isAssignableFrom(res)) {
            return (Class<R>) res;
        } else {
            throw new OntApiException(className + " is not subtype of " + interfaceType.getName());
        }
    }

    /**
     * Gets class by the path and base (interface) type.
     *
     * @param interfaceType {@link Class} the base type, not {@code null}
     * @param className     full class-name path, not {@code null}
     * @param <R>           class-type
     * @return a {@link Class}, not {@code null}
     * @throws OntApiException if wrong arguments
     */
    @SuppressWarnings("unchecked")
    static <R> Class<R> getClass(Class<R> interfaceType, String className) throws OntApiException {
        Class<?> res = getClass(className);
        if (interfaceType.isAssignableFrom(res)) {
            return (Class<R>) res;
        } else {
            throw new OntApiException(className + " is not subtype of " + interfaceType.getName());
        }
    }

    /**
     * Finds class by its full path name.
     *
     * @param name String, full class path, not {@code null}
     * @return {@link Class}, not {@code null}
     * @throws OntApiException if no class found
     * @see Class#forName(String)
     */
    static Class<?> getClass(String name) throws OntApiException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new OntApiException("No " + name + " in class-path found. " +
                    "Please include corresponding library to the maven dependencies.", e);
        }
    }
}
