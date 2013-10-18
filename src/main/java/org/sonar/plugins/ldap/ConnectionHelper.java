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

import javax.annotation.Nullable;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;

/**
 * @author Evgeny Mandrikov
 */
public final class ConnectionHelper {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionHelper.class);

  private ConnectionHelper() {
  }

  /**
   * <pre>
   * public void useContextNicely() throws NamingException {
   *   InitialDirContext context = null;
   *   boolean threw = true;
   *   try {
   *     context = new InitialDirContext();
   *     // Some code which does something with the Context and may throw a NamingException
   *     threw = false; // No throwable thrown
   *   } finally {
   *     // Close context
   *     // If an exception occurs, only rethrow it if (threw==false)
   *     close(context, threw);
   *   }
   * }
   * </pre>
   *
   * @param context the {@code Context} object to be closed, or null, in which case this method does nothing
   * @param swallowException if true, don't propagate {@code NamingException} thrown by the {@code close} method
   * @throws NamingException if {@code swallowIOException} is false and {@code close} throws a {@code NamingException}.
   */
  public static void close(@Nullable LDAPConnection context, boolean swallowException) throws LDAPException {
    if (context == null) {
      return;
    }
    try {
      context.close();
    } catch (Exception e) {
      if (swallowException) {
        LOG.warn("Exception thrown while closing context.", e);
      } else {
    	  if (e instanceof LDAPException)
    		  throw (LDAPException) e;
        throw new LDAPException(ResultCode.OTHER, e);
      }
    }
  }

  public static void closeQuetly(@Nullable LDAPConnection context) {
    try {
      close(context, true);
    } catch (Exception e) {
      LOG.error("Unexpected NamingException", e);
    }
  }

}
