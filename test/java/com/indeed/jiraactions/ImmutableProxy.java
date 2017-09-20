package com.indeed.jiraactions;

import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author kbinswanger
 */
public class ImmutableProxy<T> implements InvocationHandler {
    @Nonnull
    private final Class<T> interfaceClass;

    private ImmutableProxy(
            @Nonnull final Class<T> interfaceClass
    ) {
        this.interfaceClass = interfaceClass;
    }

    @Nonnull
    private T createProxy() {
        final Class[] interfaces = { interfaceClass };
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), interfaces, this);
    }

    public static <T> T createProxy(
            @Nonnull final Class<T> interfaceClass
    ) {
        final ImmutableProxy<T> proxy = new ImmutableProxy<>(interfaceClass);
        return proxy.createProxy();
    }

    @Override
    public Object invoke(final Object proxy,
                         final Method method,
                         final Object[] args) throws Throwable {
        final Class<?> returnType = method.getReturnType();
        if(returnType.isAssignableFrom(Number.class)) {
            return returnType.cast(0);
        } else if(returnType.equals(long.class)) {
            return (long)0;
        } else if (returnType.equals(float.class)) {
            return (float)0;
        } else if(returnType.equals(int.class)) {
            return (int) 0;
        } else if(returnType.equals(Boolean.class) || returnType.equals(boolean.class)) {
            return false;
        } else if(returnType.equals(String.class)) {
            return "";
        } else if(returnType.equals(DateTime.class)) {
            return DateTime.now();
        } else if(returnType.equals(List.class)) {
            return Collections.emptyList();
        } else if(returnType.equals(Map.class)) {
            return Collections.emptyMap();
        }
        return null;
    }
}