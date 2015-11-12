package com.github.avarabyeu.jashing.core;

import com.github.avarabyeu.jashing.core.subscribers.ServerSentEventHandler;
import com.github.avarabyeu.jashing.utils.StringUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;
import freemarker.cache.ClassTemplateLoader;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.servlet.SparkApplication;
import spark.template.freemarker.FreeMarkerEngine;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

/**
 * Specifies all needed mappings and request handlers
 *
 * @author Andrei Varabyeu
 */
class JashingController implements SparkApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(JashingController.class);

    private final Provider<ServerSentEventHandler> serverSentEventHandler;
    private final FreeMarkerEngine freemarkerEngine;

    @Inject
    public JashingController(@Nonnull Provider<ServerSentEventHandler> serverSentEventHandler) {
        this.serverSentEventHandler = serverSentEventHandler;

        freemarkerEngine = new FreeMarkerEngine();
        freemarker.template.Configuration conf = new freemarker.template.Configuration();
        conf.setTemplateLoader(new ClassTemplateLoader(Jashing.class, "/"));
        freemarkerEngine.setConfiguration(conf);
    }


    @Override
    public void init() {

        /**
         * Doesn't works correctly in {@link com.github.avarabyeu.jashing.core.Jashing.Mode#CONTAINER} mode
         * for binary data due to <a href="https://github.com/perwendel/spark/pull/235">spark issue</>
         */
        staticFileLocation("/statics");

        /**
         * Redirect to the default dashboard
         */
        get("/", (request, response) -> {
                    response.redirect("/sample");
                    return response;
                }
        );

        /**
         * Creates new SSE event handler which pushes events into response output stream
         */
        get("/events", (request, response) ->
                {
                    try {
                        serverSentEventHandler.get().handle(request.raw().startAsync());
                    } catch (IOException e) {
                        LOGGER.error("Unable to start async request processing", e);
                        return null;
                    }
                    //return null anyway. handler takes care about response content
                    return null;
                }
        );

        /**
         * Opens widget
         */
        get("views/:widget", (request, response) -> {
                    /* path to widget is /widgets/{widget folder == widget name}/{widget name}.html */
                    response.redirect("/widgets/" + StringUtils.substringBefore(request.params(":widget"), ".html") + "/" + request.params(":widget"));
                    return response;
                }
        );

        /**
         * Opens dashboard
         */
        get("/:dashboard", (request, response) ->
                        new ModelAndView(Collections.emptyMap(),
                                "/views/dashboards/" + request.params(":dashboard") + ".ftl.html"), this.freemarkerEngine
        );

        exception(FileNotFoundException.class, (e, request, response) -> {
            response.status(HttpStatus.NOT_FOUND_404);
            response.body("Resource not found");
        });

        /* Maps template engine error */
        exception(IllegalArgumentException.class, (e, request, response) -> {
            response.status(HttpStatus.NOT_FOUND_404);
            response.body("Resource not found");
        });


    }


}
