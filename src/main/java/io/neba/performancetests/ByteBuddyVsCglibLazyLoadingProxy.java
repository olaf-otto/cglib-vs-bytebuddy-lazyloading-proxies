/*
  Copyright 2013 the original author or authors.
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package io.neba.performancetests;

import net.bytebuddy.ByteBuddy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.LazyLoader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.implementation.FieldAccessor.ofField;
import static net.bytebuddy.implementation.InvocationHandlerAdapter.toField;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Creates two different lazy-loading proxies using cglib and ByteBuddy
 * and provides them via a {@link ProxyFactory uniform interface}.
 * Tests both the creation and usage performance.
 */
@State(Scope.Benchmark)
public class ByteBuddyVsCglibLazyLoadingProxy {
    private final ProxyFactory cglibProxyFactory;
    private final ProxyFactory byteBuddyProxyFactory;
    private final List<String> target = asList("one", "two", "three");

    @SuppressWarnings("unchecked")
    public ByteBuddyVsCglibLazyLoadingProxy() {
        this.cglibProxyFactory = (list) -> (Collection<String>) ((Factory) Enhancer.create(Collection.class, (LazyLoader) () -> null)).newInstance((LazyLoader) () -> list);

        Class<? extends NebaLazyLoadingHandlerSetter> lazyType = (Class<? extends NebaLazyLoadingHandlerSetter>) new ByteBuddy()
                .subclass(Collection.class)
                .defineField("_proxyTarget", InvocationHandler.class, PUBLIC)
                .implement(NebaLazyLoadingHandlerSetter.class)
                .intercept(ofField("_proxyTarget"))
                .method(not(isDeclaredBy(NebaLazyLoadingHandlerSetter.class)))
                .intercept(toField("_proxyTarget"))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();

        this.byteBuddyProxyFactory = (list) -> {
            NebaLazyLoadingHandlerSetter handlerSetter = lazyType.newInstance();
            handlerSetter.setHandler((instance, method, args) -> method.invoke(list, args));
            return (Collection<String>) handlerSetter;
        };
    }

    @Benchmark
    public void testCglibProxyCreation() throws InstantiationException, IllegalAccessException {
        assertThat(this.cglibProxyFactory.proxyTo(this.target)).containsExactly("one", "two", "three");
    }

    @Benchmark
    public void testByteBuddyProxyCreation() throws InstantiationException, IllegalAccessException {
        assertThat(this.byteBuddyProxyFactory.proxyTo(this.target)).containsExactly("one", "two", "three");
    }

    @FunctionalInterface
    public interface ProxyFactory {
        Collection<String> proxyTo(Collection<String> target) throws IllegalAccessException, InstantiationException;
    }

    public interface NebaLazyLoadingHandlerSetter {
        void setHandler(InvocationHandler handler);
    }
}
