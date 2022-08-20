package com.github.michaelboyles.simpledi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * A collection of {@link SdiBean}s which provides a few helpful ways of accessing them.
 */
class DiscoveredBeans {
    private final List<SdiBean> beans;
    private final Map<String, List<SdiBean>> fqnToBeans;

    DiscoveredBeans(List<SdiBean> beans) {
        this.beans = List.copyOf(beans);
        this.fqnToBeans = fqnToBeans(this.beans);
    }

    public List<SdiBean> all() {
        return beans;
    }

    public List<SdiBean> beansExtending(String fqn) {
        return unmodifiableList(fqnToBeans.getOrDefault(fqn, emptyList()));
    }

    public List<SdiBean> beansWithExactFqn(String fqn) {
        return beansExtending(fqn).stream()
            .filter(bean -> bean.getFqn().equals(fqn))
            .toList();
    }

    public List<SdiBean> byNumDependencies() {
        Map<String, Long> fqnToNumDependents = new HashMap<>();
        for (SdiBean bean : beans) {
            getNumDependencies(fqnToNumDependents, bean);
        }
        return beans.stream()
            .sorted(Comparator.comparing(bean -> fqnToNumDependents.get(bean.getFqn())))
            .toList();
    }

    private long getNumDependencies(Map<String, Long> fqnToNumDependents, SdiBean bean) {
        final Long SENTINEL = -123L;

        Long prevNumDeps = fqnToNumDependents.get(bean.getFqn());
        if (SENTINEL.equals(prevNumDeps)) throw new RuntimeException("Circular dependency!");
        if (prevNumDeps != null) return prevNumDeps;

        fqnToNumDependents.put(bean.getFqn(), SENTINEL);
        long numDependencies = 0;
        for (SdiDependency dependency : bean.dependencies()) {
            for (SdiBean dependentBean : dependency.directBeans()) {
                numDependencies += (1 + getNumDependencies(fqnToNumDependents, dependentBean));
            }
        }
        fqnToNumDependents.put(bean.getFqn(), numDependencies);
        return numDependencies;
    }

    private static Map<String, List<SdiBean>> fqnToBeans(List<SdiBean> beans) {
        Map<String, List<SdiBean>> fqnToBeans = new HashMap<>();
        for (SdiBean bean : beans) {
            for (String fqn : bean.getAllFqns()) {
                fqnToBeans.computeIfAbsent(fqn, k -> new ArrayList<>()).add(bean);
            }
        }
        return fqnToBeans;
    }
}
