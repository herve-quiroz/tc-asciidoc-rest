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

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @author Herve Quiroz
 */
public final class Launcher
{
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final String ENV_HTTP_PORT = "PORT";

    private static final String PROPERTY_HTTP_PORT = "http.port";
    private static final String PROPERTY_LOGGING_LEVEL = "logging.level";

    public static void main(final String[] args) throws Exception
    {
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().addAppender(
                new ConsoleAppender(new PatternLayout("%d{dd-MM-yyyy HH:mm:ss.SSS}:%-5p:%c:%m%n")));
        final String levelName = System.getProperty(PROPERTY_LOGGING_LEVEL);
        final Level level;
        if (levelName == null)
        {
            level = Level.INFO;
        }
        else
        {
            level = Level.toLevel(levelName);
        }
        org.apache.log4j.Logger.getRootLogger().setLevel(level);

        final String portString = System.getProperty(PROPERTY_HTTP_PORT, System.getenv(ENV_HTTP_PORT));
        final int port = portString != null ? Integer.valueOf(portString) : DEFAULT_HTTP_PORT;
        final Server server = new Server(port);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new AsciidocEndPoint()), "/html/*");
        context.addServlet(new ServletHolder(new StaticResource()), "/favicon.ico");
        server.start();
        server.join();
    }
}
