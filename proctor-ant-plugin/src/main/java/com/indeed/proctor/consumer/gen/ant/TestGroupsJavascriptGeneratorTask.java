package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsJavascriptGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ant task for generating Javascript Proctor test groups files.
 *
 * @author andrewk
 */
public class TestGroupsJavascriptGeneratorTask extends TestGroupsGeneratorTask {
    private static final Logger LOGGER = LogManager.getLogger(TestGroupsJavascriptGeneratorTask.class);
    private final TestGroupsJavascriptGenerator gen = new TestGroupsJavascriptGenerator();

    private boolean useClosure;

    public boolean isUseClosure() {
        return useClosure;
    }

    public void setUseClosure(final boolean useClosure) {
        this.useClosure = useClosure;
    }

    @Override
    protected void generateSourceFiles(final ProctorSpecification specification) throws CodeGenException {
        gen.generate(
                specification,
                target,
                packageName,
                groupsClass,
                useClosure
        );
    }
}
