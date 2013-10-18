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

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.plugins.ldap.server.LdapServer;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

public class LdapSearchTest {

  @ClassRule
  public static LdapServer server = new LdapServer("/users.example.org.ldif");

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static Map<String, LdapContextFactory> contextFactories;

  @BeforeClass
  public static void init() {
    contextFactories = new LdapSettingsManager(LdapSettingsFactory.generateSimpleAnonymousAccessSettings(server, null)).getContextFactories();
  }

  @Test
  public void subtreeSearch() throws Exception {
    LdapSearch search = new LdapSearch(contextFactories.values().iterator().next())
        .setBaseDn("dc=example,dc=org")
        .setRequest("(objectClass={0})")
        .setParameters("inetOrgPerson")
        .returns("objectClass");

    assertThat(search.getBaseDn()).isEqualTo("dc=example,dc=org");
    assertThat(search.getScope()).isEqualTo(SearchScope.SUB);
    assertThat(search.getRequest()).isEqualTo("(objectClass={0})");
    assertThat(search.getParameters()).isEqualTo(new String[] {"inetOrgPerson"});
    assertThat(search.getReturningAttributes()).isEqualTo(new String[] {"objectClass"});
    assertThat(search.toString()).isEqualTo("LdapSearch{baseDn=dc=example,dc=org, scope=SUB, request=(objectClass={0}), parameters=[inetOrgPerson], attributes=[objectClass]}");
    assertThat(search.find().getEntryCount()).isEqualTo(3);
    thrown.expect(LDAPException.class);
    thrown.expectMessage("Non unique result for " + search.toString());
    search.findUnique();
  }

  @Test
  public void oneLevelSearch() throws Exception {
    LdapSearch search = new LdapSearch(contextFactories.values().iterator().next())
        .setBaseDn("dc=example,dc=org")
        .setScope(SearchScope.ONE)
        .setRequest("(objectClass={0})")
        .setParameters("inetOrgPerson")
        .returns("cn");

    assertThat(search.getBaseDn()).isEqualTo("dc=example,dc=org");
    assertThat(search.getScope()).isEqualTo(SearchScope.ONE);
    assertThat(search.getRequest()).isEqualTo("(objectClass={0})");
    assertThat(search.getParameters()).isEqualTo(new String[] {"inetOrgPerson"});
    assertThat(search.getReturningAttributes()).isEqualTo(new String[] {"cn"});
    assertThat(search.toString()).isEqualTo("LdapSearch{baseDn=dc=example,dc=org, scope=ONE, request=(objectClass={0}), parameters=[inetOrgPerson], attributes=[cn]}");
    assertThat(search.find().getEntryCount()).isEqualTo(0);
    assertThat(search.findUnique()).isNull();
  }

  @Test
  public void objectSearch() throws Exception {
    LdapSearch search = new LdapSearch(contextFactories.values().iterator().next())
        .setBaseDn("cn=bind,ou=users,dc=example,dc=org")
        .setScope(SearchScope.BASE)
        .setRequest("(objectClass={0})")
        .setParameters("uidObject")
        .returns("uid");

    assertThat(search.getBaseDn()).isEqualTo("cn=bind,ou=users,dc=example,dc=org");
    assertThat(search.getScope()).isEqualTo(SearchScope.BASE);
    assertThat(search.getRequest()).isEqualTo("(objectClass={0})");
    assertThat(search.getParameters()).isEqualTo(new String[] {"uidObject"});
    assertThat(search.getReturningAttributes()).isEqualTo(new String[] {"uid"});
    assertThat(search.toString()).isEqualTo(
        "LdapSearch{baseDn=cn=bind,ou=users,dc=example,dc=org, scope=BASE, request=(objectClass={0}), parameters=[uidObject], attributes=[uid]}");
    assertThat(search.find().getEntryCount()).isEqualTo(1);
    assertThat(search.findUnique()).isNotNull();
  }

}
