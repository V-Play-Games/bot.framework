/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vpg.bot.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class InstanceLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceLoader.class);
    private static final ScanResult SCAN_RESULT = new ClassGraph().enableClassInfo().scan();
    private ClassFilter filter;

    public InstanceLoader(ClassFilter filter) {
        this.filter = filter;
    }

    public ClassFilter getFilter() {
        return filter;
    }

    public void setFilter(ClassFilter filter) {
        this.filter = filter;
    }

    @SuppressWarnings("unchecked")
    public <T> List<Class<? extends T>> getAllClasses(Class<T> clazz) {
        return SCAN_RESULT.getAllClasses()
            .stream()
            .filter(x -> !x.isAbstract() && x.isStandardClass() && (x.implementsInterface(clazz) || x.extendsSuperclass(clazz)))
            .map(ClassInfo::loadClass)
            .map(c -> (Class<? extends T>) c)
            .filter(filter.asPredicate())
            .collect(Collectors.toList());
    }

    public <T> void loadAllInstances(Class<T> clazz, Consumer<T> newInstanceConsumer, BiConsumer<Class<? extends T>, ? super Throwable> errorConsumer, Object... parameters) {
        Class<?>[] paramTypes = Arrays.stream(parameters).map(Object::getClass).toArray(Class[]::new);
        getAllClasses(clazz).forEach(c -> {
            try {
                newInstanceConsumer.accept(c.getConstructor(paramTypes).newInstance(parameters));
            } catch (Throwable t) {
                errorConsumer.accept(c, t);
            }
        });
    }
}