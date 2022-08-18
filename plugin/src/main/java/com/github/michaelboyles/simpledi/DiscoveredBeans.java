package com.github.michaelboyles.simpledi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

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

    public List<SdiBean> forFqn(String fqn) {
        return unmodifiableList(fqnToBeans.getOrDefault(fqn, emptyList()));
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
