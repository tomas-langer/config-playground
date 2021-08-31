package jakarta.config.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import jakarta.config.spi.Converter;

class OrdinalConverter {
    static final int BUILT_IN_PRIORITY = 10000;

    private final int priority;
    private final Class<?> type;
    private final Converter<?> converter;

    OrdinalConverter(Converter<?> converter, Class<?> aClass, int priority) {
        this.priority = priority;
        this.type = aClass;
        this.converter = converter;
    }

    OrdinalConverter(Converter<?> converter) {
        this(converter, getConverterType(converter.getClass()), Priorities.find(converter, 100));
    }

    @Override
    public String toString() {
        return priority + ": " + getType().getName() + " - " + converter;
    }

    int getPriority() {
        return priority;
    }

    Class<?> getType() {
        return type;
    }

    Converter<?> getConverter() {
        return converter;
    }

    static Class<?> getConverterType(Class<?> converterClass) {
        Class<?> type = doGetType(converterClass);
        if (null == type) {
            throw new IllegalArgumentException("Converter " + converterClass + " must be a ParameterizedType.");
        }
        return type;
    }

    private static Class<?> doGetType(Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return null;
        }

        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericInterface;
                if (pt.getRawType().equals(Converter.class)) {
                    Type[] typeArguments = pt.getActualTypeArguments();
                    if (typeArguments.length != 1) {
                        throw new IllegalStateException("Converter " + clazz + " must be a ParameterizedType.");
                    }
                    Type typeArgument = typeArguments[0];
                    if (typeArgument instanceof Class) {
                        return (Class<?>) typeArgument;
                    }
                    throw new IllegalStateException("Converter " + clazz + " must convert to a class, not " + typeArgument);
                }
            }
        }

        return doGetType(clazz.getSuperclass());
    }
}
