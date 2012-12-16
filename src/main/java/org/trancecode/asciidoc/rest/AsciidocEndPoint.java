/*
 * Copyright 2012 TranceCode
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.trancecode.asciidoc.rest;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.trancecode.asciidoc.AsciiDoc;
import org.trancecode.logging.Logger;

/**
 * @author Herve Quiroz
 */
public final class AsciidocEndPoint extends HttpServlet
{
    private static final long serialVersionUID = -3890844511885007418L;
    private static Logger LOG = Logger.getLogger(AsciidocEndPoint.class);

    private static boolean isRedirect(final int code)
    {
        return code >= 300 && code < 400;
    }

    private static URI getRedirect(final HttpURLConnection connection) throws IOException
    {
        return URI.create(connection.getHeaderField("Location"));
    }

    private static InputStream get(final URI uri, final boolean followRedirect) throws IOException
    {
        LOG.trace("{@method} uri = {} ; followRedirect = {}", uri, followRedirect);
        final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        try
        {
            if (followRedirect && isRedirect(connection.getResponseCode()))
            {
                final URI redirectUri = getRedirect(connection);
                LOG.trace("  redirect = {}", redirectUri);
                return get(redirectUri, followRedirect);
            }
            return uri.toURL().openStream();
        }
        finally
        {
            connection.disconnect();
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException
    {
        LOG.trace("{@method} request = {}", request);
        final String targetType = request.getServletPath();
        Preconditions.checkArgument(targetType.equals("/html"), "%s", targetType);
        final String path = request.getPathInfo();
        LOG.trace("  path = {}", path);
        final URI targetUri = URI.create("http:/" + path);
        LOG.trace("  targetUri = {}", targetUri);
        LOG.debug("{} :: GET :: {} :: {}", request.getRemoteHost(), request.getRequestURI(), targetUri);
        InputStream targetIn = null;
        OutputStream htmlOut = null;
        try
        {
            targetIn = get(targetUri, true);
            Preconditions.checkState(targetIn != null);
            htmlOut = response.getOutputStream();
            AsciiDoc.toXhtml(targetIn, htmlOut);
        }
        finally
        {
            Closeables.closeQuietly(targetIn);
            Closeables.closeQuietly(htmlOut);
        }
        response.setContentType("application/xhtml+xml");
    }
}
