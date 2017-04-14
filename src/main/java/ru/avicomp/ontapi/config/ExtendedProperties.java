package ru.avicomp.ontapi.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * This is a simple extended {@link Properties} with supporting several primitive typed objects.
 * Currently there are only 6 additional types:
 * - {@link Class}
 * - {@link Enum}
 * - {@link Boolean}
 * - {@link Integer}
 * - {@link Long}
 * - {@link Double}
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

    protected enum MapType {
        CLASS(Class.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return Class.forName(value);
            }

            @Override
            public String toString(Object value) throws Exception {
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
            public String toString(Object value) throws Exception {
                Enum val = (Enum) value;
                Class<?> enumType = val.getDeclaringClass();
                return enumType.getName() + "#" + val.name();
            }

            @Override
            public boolean isSupported(Class<?> clazz) {
                return type.isAssignableFrom(clazz);
            }
        },
        BOOLEAN(Boolean.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return Boolean.parseBoolean(value);
            }
        },
        INTEGER(Integer.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return Integer.valueOf(value);
            }
        },
        LONG(Long.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return Long.valueOf(value);
            }
        },
        DOUBLE(Double.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return Double.valueOf(value);
            }
        },
        STRING(String.class) {
            @Override
            public Object toObject(String value) throws Exception {
                return value;
            }

            @Override
            public String toKey(String key) {
                return key;
            }
        },;
        protected final Class<?> type;

        MapType(Class<?> t) {
            this.type = t;
        }

        public abstract Object toObject(String value) throws Exception;

        public String toString(Object value) throws Exception {
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

    protected MapType getMapType(Class<?> type) {
        return Stream.of(MapType.values()).filter(m -> m.isSupported(type))
                .findFirst().orElseThrow(() -> new RuntimeException(type + " is not supported."));
    }

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

    @SuppressWarnings("unchecked")
    public <T> List<T> getListProperty(String key, Class<T> type) {
        List<T> res = new ArrayList<>();
        MapType map = getMapType(type);
        int i = 0;
        String v;
        while ((v = getProperty(map.toListKey(key, i++))) != null) {
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
}
