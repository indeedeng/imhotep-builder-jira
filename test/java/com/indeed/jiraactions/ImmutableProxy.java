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
 * Immutables are awesome, but they have one primary flaw: they can be hard to use in tests. We want the fields on the actual Action class
 * to be nonnull. This helps detect errors early in testing (because it can be hard to validate the actual data is correct). However, the
 * Action class changes frequently when new fields are created, and we don't want this to force someone to have to update tests for no value.
 *
 * This class attempts to solve this problem by taking advantage of the fact that you can build one Immutable by starting with another. This
 * class basically helps you make mocks of Immutables. It returns a reasonable default value for all the types it knows about. So you can
 * create a new Immutable, using this as the starting point. Therefore you end up with an Immutable that's like a nice mock; you can set it
 * with whatever fields you want without having to do any more work.
 *
 * Sample usage:
 * final Action action = ImmutableAction.builder().from(ImmutableProxy.createProxy(Action.class)).action("create").build();
 *
 * Whether I'm proud or ashamed to have written this depends on the day.
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
            return (int)0;
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