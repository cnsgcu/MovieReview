package edu.umkc;

import edu.umkc.config.ApplicationConfig;
import edu.umkc.config.WebMvcConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

public class WebXML implements WebApplicationInitializer
{
    final static private Logger LOGGER = LoggerFactory.getLogger(WebXML.class);

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException
    {
        LOGGER.info("Start Web App.");

        final WebApplicationContext rootContext = createRootContext(servletContext);

        createWebMvcContext(servletContext, rootContext);
    }

    private WebApplicationContext createRootContext(ServletContext servletContext)
    {
        LOGGER.info("Config Web App Context.");

        final AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(ApplicationConfig.class);

        servletContext.addListener(new ContextLoaderListener(rootContext));

        return rootContext;
    }

    private void createWebMvcContext(ServletContext servletContext, WebApplicationContext rootContext)
    {
        LOGGER.info("Config Web Mvc Context.");

        final AnnotationConfigWebApplicationContext webMvcContext = new AnnotationConfigWebApplicationContext();
        webMvcContext.register(WebMvcConfig.class);
        webMvcContext.setParent(rootContext);

        ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet(
                "dispatcher",
                new DispatcherServlet(webMvcContext)
        );

        dispatcherServlet.addMapping("/");
        dispatcherServlet.setLoadOnStartup(1);
    }
}