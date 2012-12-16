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

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.trancecode.logging.Logger;

/**
 * @author Herve Quiroz
 */
public final class StaticResource extends HttpServlet
{
    private static final long serialVersionUID = 8959569664906377375L;
    private static Logger LOG = Logger.getLogger(StaticResource.class);

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException
    {
        LOG.trace("{@method} request = {}", request);
        final String resourcePath = request.getRequestURI();
        LOG.trace("  resourcePath = {}", resourcePath);
        final URL resourceUrl = getClass().getResource(resourcePath);
        ByteStreams.copy(resourceUrl.openStream(), response.getOutputStream());
        return;
    }
}
