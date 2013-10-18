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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.GetEntryLDAPConnectionPoolHealthCheck;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SingleServerSet;

/**
 * @author Evgeny Mandrikov and others
 */
public class LdapContextFactory {

  private static final Logger LOG = LoggerFactory.getLogger(LdapContextFactory.class);

  private static final String DEFAULT_AUTHENTICATION = "simple";

  @VisibleForTesting
  static final String GSSAPI_METHOD = "GSSAPI";

  @VisibleForTesting
  static final String DIGEST_MD5_METHOD = "DIGEST-MD5";

  @VisibleForTesting
  static final String CRAM_MD5_METHOD = "CRAM-MD5";

  /**
   * The Sun LDAP property used to enable connection pooling. This is used in the default implementation to enable
   * LDAP connection pooling.
   */
  private final String providerUrl;
  private final String authentication;
  private final String username;
  private final String password;
  private final String realm;
  private ServerSet serverSet;

  public LdapContextFactory(Settings settings, String settingsPrefix) {
    this.authentication = StringUtils.defaultString(settings.getString(settingsPrefix + ".authentication"), DEFAULT_AUTHENTICATION);
    this.realm = settings.getString(settingsPrefix + ".realm");
    String urlKey = settingsPrefix + ".url";
    String ldapUrl = settings.getString(urlKey);
    if (ldapUrl == null && realm != null) {
      ldapUrl = LdapAutodiscovery.getLdapServer(realm);
    }
    if (StringUtils.isBlank(ldapUrl)) {
      throw new SonarException("The property '" + urlKey + "' property is empty and SonarQube is not able to auto-discover any LDAP server.");
    }
    this.providerUrl = ldapUrl;
    
    String[] urls = ldapUrl.split(",");
    List<ServerSet> sss = new ArrayList<ServerSet>();
    for (String url : urls) {
    	LDAPURL u;
		try {
			u = new LDAPURL(url);
		} catch (LDAPException e) {
			throw new SonarException(e);
		}
    	LDAPConnectionOptions opts = new LDAPConnectionOptions();
    	opts.setAutoReconnect(true);
    	ServerSet ss = new SingleServerSet(u.getHost(), u.getPort(), opts);
    	sss.add(ss);
    }
    	
    this.serverSet = new FailoverServerSet(sss);

    
    this.username = settings.getString(settingsPrefix + ".bindDn");
    this.password = settings.getString(settingsPrefix + ".bindPassword");
	
  }

  /**
   * Returns {@code InitialDirContext} for Bind user.
   */
  public LDAPConnection createConnection() throws LDAPException {
    return createUserConnection(username, password);
  }

  /**
   * Returns {@code InitialDirContext} for specified user.
   * Note that pooling intentionally disabled by this method.
   */
  public LDAPConnection createUserConnection(String principal, String credentials) throws LDAPException {
	  LDAPConnection conn = this.serverSet.getConnection(new GetEntryLDAPConnectionPoolHealthCheck(null, 0L, true, true, true, true, true));
	  conn.bind(principal, credentials);
	  return conn;
  }

  

  public boolean isSasl() {
    return DIGEST_MD5_METHOD.equals(authentication) ||
      CRAM_MD5_METHOD.equals(authentication) ||
      GSSAPI_METHOD.equals(authentication);
  }

  public boolean isGssapi() {
    return GSSAPI_METHOD.equals(authentication);
  }

  /**
   * Tests connection.
   *
   * @throws SonarException if unable to open connection
   */
  public void testConnection() {
    if (StringUtils.isBlank(username) && isSasl()) {
      throw new SonarException("When using SASL - property ldap.bindDn is required");
    } else {
      try {
        createConnection();
        LOG.info("Test LDAP connection on {}: OK", providerUrl);
      } catch (LDAPException e) {
        LOG.info("Test LDAP connection: FAIL");
        throw new SonarException("Unable to open LDAP connection", e);
      }
    }
  }

  public String getProviderUrl() {
    return providerUrl;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("url", providerUrl)
        .add("authentication", authentication)
        .add("bindDn", username)
        .add("realm", realm)
        .toString();
  }

}
