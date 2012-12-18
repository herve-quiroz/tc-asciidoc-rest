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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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

    private static URLConnection connectFollowRedirect(final URI uri) throws IOException
    {
        LOG.trace("{@method} uri = {}", uri);
        final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try
        {
            if (isRedirect(connection.getResponseCode()))
            {
                final URI redirectUri = getRedirect(connection);
                LOG.trace("  redirect = {}", redirectUri);
                return connectFollowRedirect(redirectUri);
            }
            return uri.toURL().openConnection();
        }
        finally
        {
            connection.disconnect();
        }
    }

    private static Map.Entry<String, byte[]> getResource(final URI uri) throws IOException
    {
        LOG.trace("{@method} uri = {}", uri);
        final URLConnection connection = connectFollowRedirect(uri);
        final String etag = connection.getHeaderField("Etag");
        LOG.trace("  etag = {}", etag);
        final byte[] content = readBytesAndClose(connection.getInputStream());
        LOG.trace("  content = {}", content.length);
        return Maps.immutableEntry(etag, content);
    }

    private static final Cache<String, byte[]> CACHE = CacheBuilder.newBuilder().softValues().build();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException
    {
        LOG.trace("{@method} request = {}", request);
        final String targetType = request.getServletPath();
        Preconditions.checkArgument(targetType.equals("/html"), "%s", targetType);
        final String path = request.getPathInfo();
        LOG.trace("  path = {}", path);
        final URI sourceDocumentUri = URI.create("http:/" + path);
        LOG.trace("  sourceDocumentUri = {}", sourceDocumentUri);
        LOG.debug("{} :: GET :: {} :: {}", request.getRemoteHost(), request.getRequestURI(), sourceDocumentUri);
        final Map.Entry<String, byte[]> sourceDocument = getResource(sourceDocumentUri);
        final byte[] htmlOutput;
        try
        {
            htmlOutput = CACHE.get(sourceDocument.getKey(), new Callable<byte[]>()
            {
                @Override
                public byte[] call() throws Exception
                {
                    InputStream targetIn = null;
                    ByteArrayOutputStream htmlOut = null;
                    try
                    {
                        targetIn = new ByteArrayInputStream(sourceDocument.getValue());
                        Preconditions.checkState(targetIn != null);
                        htmlOut = new ByteArrayOutputStream();
                        AsciiDoc.toXhtml(targetIn, htmlOut);
                    }
                    finally
                    {
                        Closeables.closeQuietly(targetIn);
                        Closeables.closeQuietly(htmlOut);
                    }
                    return htmlOut.toByteArray();
                }
            });
        }
        catch (final ExecutionException e)
        {
            throw new ServletException(sourceDocumentUri.toString(), e);
        }
        response.setContentType("application/xhtml+xml");
        writeBytesAndClose(htmlOutput, response.getOutputStream());
    }

    private static byte[] readBytesAndClose(final InputStream in) throws IOException
    {
        try
        {
            return ByteStreams.toByteArray(in);
        }
        finally
        {
            Closeables.closeQuietly(in);
        }
    }

    private static void writeBytesAndClose(final byte[] bytes, final OutputStream out) throws IOException
    {
        InputStream in = null;
        try
        {
            in = new ByteArrayInputStream(bytes);
            ByteStreams.copy(in, out);
        }
        finally
        {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(out);
        }
    }
}
