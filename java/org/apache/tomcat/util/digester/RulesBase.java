/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.util.digester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * Default implementation of the <code>Rules</code> interface that supports the standard rule matching behavior. This
 * class can also be used as a base class for specialized <code>Rules</code> implementations.
 * </p>
 * <p>
 * The matching policies implemented by this class support two different types of pattern matching rules:
 * </p>
 * <ul>
 * <li><em>Exact Match</em> - A pattern "a/b/c" exactly matches a <code>&lt;c&gt;</code> element, nested inside a
 * <code>&lt;b&gt;</code> element, which is nested inside an <code>&lt;a&gt;</code> element.</li>
 * <li><em>Tail Match</em> - A pattern "&#42;/a/b" matches a <code>&lt;b&gt;</code> element, nested inside an
 * <code>&lt;a&gt;</code> element, no matter how deeply the pair is nested.</li>
 * </ul>
 */
public class RulesBase implements Rules {

    // ----------------------------------------------------- Instance Variables

    /**
     * The set of registered Rule instances, keyed by the matching pattern. Each value is a List containing the Rules
     * for that pattern, in the order that they were originally registered.
     */
    protected HashMap<String,List<Rule>> cache = new HashMap<>();


    /**
     * The Digester instance with which this Rules instance is associated.
     */
    protected Digester digester = null;


    /**
     * The set of registered Rule instances, in the order that they were originally registered.
     */
    protected ArrayList<Rule> rules = new ArrayList<>();


    // ------------------------------------------------------------- Properties

    /**
     * Return the Digester instance with which this Rules instance is associated.
     */
    @Override
    public Digester getDigester() {
        return this.digester;
    }


    /**
     * Set the Digester instance with which this Rules instance is associated.
     *
     * @param digester The newly associated Digester instance
     */
    @Override
    public void setDigester(Digester digester) {
        this.digester = digester;
        for (Rule item : rules) {
            item.setDigester(digester);
        }
    }


    // --------------------------------------------------------- Public Methods

    /**
     * Register a new Rule instance matching the specified pattern.
     *
     * @param pattern Nesting pattern to be matched for this Rule
     * @param rule    Rule instance to be registered
     */
    @Override
    public void add(String pattern, Rule rule) {
        // to help users who accidentally add '/' to the end of their patterns
        int patternLength = pattern.length();
        if (patternLength > 1 && pattern.endsWith("/")) {
            pattern = pattern.substring(0, patternLength - 1);
        }

        cache.computeIfAbsent(pattern, k -> new ArrayList<>()).add(rule);
        rules.add(rule);
        if (this.digester != null) {
            rule.setDigester(this.digester);
        }
    }


    /**
     * Clear all existing Rule instance registrations.
     */
    @Override
    public void clear() {
        cache.clear();
        rules.clear();
    }


    /**
     * Return a List of all registered Rule instances that match the specified nesting pattern, or a zero-length List if
     * there are no matches. If more than one Rule instance matches, they <strong>must</strong> be returned in the order
     * originally registered through the <code>add()</code> method.
     *
     * @param namespaceURI Namespace URI for which to select matching rules, or <code>null</code> to match regardless of
     *                         namespace URI
     * @param pattern      Nesting pattern to be matched
     */
    @Override
    public List<Rule> match(String namespaceURI, String pattern) {

        // List rulesList = (List) this.cache.get(pattern);
        List<Rule> rulesList = lookup(namespaceURI, pattern);
        if ((rulesList == null) || (rulesList.isEmpty())) {
            // Find the longest key, ie more discriminant
            String longKey = "";
            for (String key : this.cache.keySet()) {
                if (key.startsWith("*/")) {
                    if (pattern.equals(key.substring(2)) || pattern.endsWith(key.substring(1))) {
                        if (key.length() > longKey.length()) {
                            // rulesList = (List) this.cache.get(key);
                            rulesList = lookup(namespaceURI, key);
                            longKey = key;
                        }
                    }
                }
            }
        }
        if (rulesList == null) {
            rulesList = new ArrayList<>();
        }
        return rulesList;
    }


    /**
     * Return a List of all registered Rule instances, or a zero-length List if there are no registered Rule instances.
     * If more than one Rule instance has been registered, they <strong>must</strong> be returned in the order
     * originally registered through the <code>add()</code> method.
     */
    @Override
    public List<Rule> rules() {
        return this.rules;
    }


    // ------------------------------------------------------ Protected Methods

    /**
     * Return a List of Rule instances for the specified pattern that also match the specified namespace URI (if any).
     * If there are no such rules, return <code>null</code>.
     *
     * @param namespaceURI Namespace URI to match, or <code>null</code> to select matching rules regardless of namespace
     *                         URI
     * @param pattern      Pattern to be matched
     *
     * @return a rules list
     */
    protected List<Rule> lookup(String namespaceURI, String pattern) {
        // Optimize when no namespace URI is specified
        List<Rule> list = this.cache.get(pattern);
        if (list == null) {
            return null;
        }
        if (namespaceURI == null || namespaceURI.isEmpty()) {
            return list;
        }

        // Select only Rules that match on the specified namespace URI
        List<Rule> results = new ArrayList<>();
        for (Rule item : list) {
            if ((namespaceURI.equals(item.getNamespaceURI())) || (item.getNamespaceURI() == null)) {
                results.add(item);
            }
        }
        return results;
    }
}
