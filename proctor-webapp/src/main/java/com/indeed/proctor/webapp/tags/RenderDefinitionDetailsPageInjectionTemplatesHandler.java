package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.webapp.extensions.renderer.DefinitionDetailsPageRenderer;
import com.indeed.proctor.webapp.extensions.renderer.DefinitionDetailsPageRenderer.DefinitionDetailsPagePosition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

/** */
public class RenderDefinitionDetailsPageInjectionTemplatesHandler extends TagSupport {
    private static final Logger LOGGER =
            LogManager.getLogger(RenderDefinitionDetailsPageInjectionTemplatesHandler.class);

    private DefinitionDetailsPagePosition position;
    private String testName;
    private TestDefinition testDefinition;

    public void setPosition(final DefinitionDetailsPagePosition position) {
        this.position = position;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public void setTestDefinition(final TestDefinition testDefinition) {
        this.testDefinition = testDefinition;
    }

    public int doStartTag() {
        try {
            pageContext.getOut().print(renderTemplates());
        } catch (IOException e) {
            LOGGER.error("Failed to write rendered html to page context", e);
        }

        return SKIP_BODY;
    }

    private String renderTemplates() {
        final StringBuilder renderedHTML = new StringBuilder();
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        try {
            final Map<String, DefinitionDetailsPageRenderer> rendererBeans =
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(
                            context, DefinitionDetailsPageRenderer.class);
            for (final DefinitionDetailsPageRenderer renderer : rendererBeans.values()) {
                if (position == renderer.getDefinitionDetailsPagePosition()) {
                    renderedHTML.append(renderer.getRenderedHtml(testName, testDefinition));
                    renderedHTML.append(
                            renderer.getRenderedHtml(pageContext, testName, testDefinition));
                }
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred when attempting to inject template.", e);
        }
        return renderedHTML.toString();
    }
}
