/*
 * Sonar LDAP Plugin
 * Copyright (C) 2009 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.ldap;

import java.util.Arrays;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * Fluent API for building LDAP queries.
 *
 * @author Evgeny Mandrikov
 */
public class LdapSearch {

  private static final Logger LOG = LoggerFactory.getLogger(LdapSearch.class);

  private final LdapContextFactory contextFactory;

  private String baseDn;
  private SearchScope scope = SearchScope.SUB;
  private String request;
  private String[] parameters;
  private String[] returningAttributes;

  public LdapSearch(LdapContextFactory contextFactory) {
    this.contextFactory = contextFactory;
  }

  /**
   * Sets BaseDN.
   */
  public LdapSearch setBaseDn(String baseDn) {
    this.baseDn = baseDn;
    return this;
  }

  public String getBaseDn() {
    return baseDn;
  }

  /**
   * Sets the search scope.
   *
   * @see SearchControls#ONELEVEL_SCOPE
   * @see SearchControls#SUBTREE_SCOPE
   * @see SearchControls#OBJECT_SCOPE
   */
  public LdapSearch setScope(SearchScope scope) {
    this.scope = scope;
    return this;
  }

  public SearchScope getScope() {
    return scope;
  }

  /**
   * Sets request.
   */
  public LdapSearch setRequest(String request) {
    this.request = request;
    return this;
  }

  public String getRequest() {
    return request;
  }

  /**
   * Sets search parameters.
   */
  public LdapSearch setParameters(String... parameters) {
    this.parameters = parameters;
    return this;
  }

  public String[] getParameters() {
    return parameters;
  }

  /**
   * Sets attributes, which should be returned by search.
   */
  public LdapSearch returns(String... attributes) {
    this.returningAttributes = attributes;
    return this;
  }

  public String[] getReturningAttributes() {
    return returningAttributes;
  }

  /**
   * @throws NamingException if unable to perform search
   */
  public SearchResult find() throws LDAPException {
    LOG.debug("Search: {}", this);
    SearchResult result;
    LDAPConnection context = null;
    boolean threw = false;
    try {
      context = contextFactory.createConnection();
      
      String req = request;
      for (int i = 0; i < parameters.length; i++) {
          req = StringUtils.replace(req, "{" + i + "}", parameters[i]);
      }
      
      result = context.search(baseDn, this.scope, req, returningAttributes);
      threw = true;
    } finally {
      ConnectionHelper.close(context, threw);
    }
    return result;
  }

  /**
   * @return result, or null if not found
   * @throws NamingException if unable to perform search, or non unique result
   */
  public SearchResultEntry findUnique() throws LDAPException {
    SearchResult result = find();
    int entryCount = result.getEntryCount();
    switch (entryCount) {
    case 0:
    	return null;
    case 1:
    	return result.getSearchEntries().get(0);
    default:
    	throw new LDAPException(ResultCode.INAPPROPRIATE_MATCHING, "Non unique result for " + toString());
    }
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("baseDn", baseDn)
        .add("scope", scopeToString())
        .add("request", request)
        .add("parameters", Arrays.toString(parameters))
        .add("attributes", Arrays.toString(returningAttributes))
        .toString();
  }

  private String scopeToString() {
	  return scope.toString();
  }

}
