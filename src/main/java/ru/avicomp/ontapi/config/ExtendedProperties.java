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

package ru.avicomp.ontapi.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * This is a simple extended {@link Properties} with supporting several primitive typed objects.
 * Currently there are only 6 additional types:
 * <ul>
 * <li>{@link Class}</li>
 * <li>{@link Enum}</li>
 * <li>{@link Boolean}</li>
 * <li>{@link Integer}</li>
 * <li>{@link Long}</li>
 * <li>{@link Double}</li>
 * </ul>
 * It also supports a {@link List} consisting of the types listed above.
 * Example:
 * The following snippet
 * <pre>{@code
 * ExtendedProperties prop = new ExtendedProperties();
 * prop.setTypedProperty("key1", Stream.of(AccessMode.EXECUTE, AccessMode.READ).collect(Collectors.toCollection(ArrayList::new)));
 * prop.setTypedProperty("key2", false);
 * prop.setProperty("key3", "Some comment");
 * }</pre>
 * will produce the following properties file:
 * <pre>{@code
 * key1.list.enum.1=java.nio.file.AccessMode\#READ
 * key1.list.enum.0=java.nio.file.AccessMode\#EXECUTE
 * key2.boolean=false
 * key3=Some comment
 * }</pre>
 * <p>
 * Created by @szuev on 14.04.2017.
 */
@SuppressWarnings("WeakerAccess")
public class ExtendedProperties extends Properties {

    public ExtendedProperties() {
        super();
    }

    public ExtendedProperties(Properties defaults) {
        super(defaults);
    }

    /**
     * Reads a {@code List} from the {@link Properties} Map.
     * The keys in the Properties must be in the format "{@code key}.list.{@code type}.index",
     * where "list" is a fixed delimiter, and "index" is an integer position of the element in the resulting {@code List}.
     *
     * @param key  String, key-prefix
     * @param type Class-type, one of the following:
     *             {@link Class}, {@link Enum}, {@link Boolean}, {@link Integer}, {@link Long}, {@link Double}
     * @param <T>  a generic type of the returned list
     * @return {@link List}, not {@code null}
     * @see #setListProperty(String, List)
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getListProperty(String key, Class<T> type) {
        List<T> res = new ArrayList<>();
        MapType map = getMapType(type);
        for (int i = 0; i < size(); i++) {
            String v = getProperty(map.toListKey(key, i));
            if (v == null) continue;
            T obj;
            try {
                obj = (T) map.toObject(v);
            } catch (Exception e) {
                throw new RuntimeException("Unable to get list property [" + key + ":" + i + "]", e);
            }
            res.add(obj);
        }
        return res;
    }

    protected MapType getMapType(Class<?> type) {
        return Stream.of(MapType.values()).filter(m -> m.isSupported(type))
                .findFirst().orElseThrow(() -> new RuntimeException(type + " is not supported."));
    }

    /**
     * Gets a typed Object value from the {@link Properties} Map.
     * The keys in the Properties must be in the format {@code key}.{@code type}.
     *
     * @param key  String, key-prefix
     * @param type Class-type, one of the following:
     *             {@link Class}, {@link Enum}, {@link Boolean}, {@link Integer}, {@link Long}, {@link Double}
     * @param <T>  a type of the returned value
     * @return Object value
     * @see #setTypedProperty(String, Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T getTypedProperty(String key, Class<T> type) {
        MapType map = getMapType(type);
        String res = getProperty(map.toKey(key));
        if (res == null) return null;
        try {
            return (T) map.toObject(res);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get property '" + key + "'", e);
        }
    }

    /**
     * Puts the object of the type {@link T} to the {@link Properties} Map.
     *
     * @param key   String, key-prefix
     * @param value Object, allowed to be of one of the following types:
     *              {@link Class}, {@link Enum}, {@link Boolean}, {@link Integer}, {@link Long}, {@link Double}
     * @param <T>   a type of the input value
     * @see #getTypedProperty(String, Class)
     */
    public <T> void setTypedProperty(String key, T value) {
        if (value instanceof List) {
            setListProperty(key, (List<?>) value);
            return;
        }
        MapType map = getMapType(value.getClass());
        try {
            setProperty(map.toKey(key), map.toString(value));
        } catch (Exception e) {
            throw new RuntimeException("Unable to set property '" + key + "'", e);
        }
    }

    /**
     * Writes the given list to the {@link Properties} Map
     *
     * @param key    String, key-prefix
     * @param values List of values that are allowed to be one of the following types:
     *               {@link Class}, {@link Enum}, {@link Boolean}, {@link Integer}, {@link Long}, {@link Double}
     * @param <T>    a generic type of the input list
     * @see #getListProperty(String, Class)
     */
    public <T> void setListProperty(String key, List<T> values) {
        if (values.isEmpty()) return;
        MapType map = getMapType(values.get(0).getClass());
        for (int i = 0; i < values.size(); i++) {
            String val;
            try {
                val = map.toString(values.get(i));
            } catch (Exception e) {
                throw new RuntimeException("Unable to set list property [" + key + ":" + i + "]", e);
            }
            setProperty(map.toListKey(key, i), val);
        }
    }

    public List<?> getListProperty(String key) {
        for (MapType map : MapType.values()) {
            if (!containsKey(map.toListKey(key, 0))) continue;
            return getListProperty(key, map.type);
        }
        return null;
    }

    public List<String> getStringListProperty(String key) {
        return getListProperty(key, String.class);
    }

    public Class getClassProperty(String key) {
        return getTypedProperty(key, Class.class);
    }

    public Enum getEnumProperty(String key) {
        return getTypedProperty(key, Enum.class);
    }

    public Boolean getBooleanProperty(String key) {
        return getTypedProperty(key, Boolean.class);
    }

    public Integer getIntegerProperty(String key) {
        return getTypedProperty(key, Integer.class);
    }

    public Long getLongProperty(String key) {
        return getTypedProperty(key, Long.class);
    }

    public Double getDoubleProperty(String key) {
        return getTypedProperty(key, Double.class);
    }

    protected enum MapType {
        CLASS(Class.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return Class.forName(value);
            }

            @Override
            public String toString(Object value) {
                return ((Class) value).getName();
            }
        },
        ENUM(Enum.class) {
            @Override
            public Object toObject(String value) throws Exception {
                String path = value.replaceAll("^(.+)#.+$", "$1");
                String name = value.replaceAll("^.+#(.+)$", "$1");

                for (Object o : Class.forName(path).getEnumConstants()) {
                    if (String.valueOf(o).equals(name)) return o;
                }
                throw new RuntimeException(value + " is not enum.");
            }

            @Override
            public String toString(Object value) {
                Enum val = (Enum) value;
                return val.getDeclaringClass().getName() + "#" + val.name();
            }

            @Override
            public boolean isSupported(Class<?> clazz) {
                return type.isAssignableFrom(clazz);
            }
        },
        BOOLEAN(Boolean.class) {
            @Override
            public Object toObject(String value) {
                return Boolean.parseBoolean(value);
            }
        },
        INTEGER(Integer.class) {
            @Override
            public Object toObject(String value) {
                return Integer.valueOf(value);
            }
        },
        LONG(Long.class) {
            @Override
            public Object toObject(String value) {
                return Long.valueOf(value);
            }
        },
        DOUBLE(Double.class) {
            @Override
            public Object toObject(String value) {
                return Double.valueOf(value);
            }
        },
        STRING(String.class) {
            @Override
            public Object toObject(String value) {
                return value;
            }

            @Override
            public String toKey(String key) {
                return key;
            }
        },
        ;
        protected final Class<?> type;

        MapType(Class<?> t) {
            this.type = t;
        }

        public abstract Object toObject(String value) throws Exception;

        public String toString(Object value) {
            return String.valueOf(value);
        }

        public String typeName() {
            return type.getSimpleName().toLowerCase();
        }

        public String toKey(String key) {
            return key + "." + typeName();
        }

        public String toListKey(String key, int index) {
            return toKey(key + ".list") + "." + index;
        }

        public boolean isSupported(Class<?> clazz) {
            return Objects.equals(type, clazz);
        }
    }
}
