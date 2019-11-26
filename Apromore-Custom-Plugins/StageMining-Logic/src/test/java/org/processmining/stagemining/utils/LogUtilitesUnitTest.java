package org.processmining.stagemining.utils;

import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apromore.xes.factory.XFactory;
import org.apromore.xes.model.XLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xeslite.external.XFactoryExternalStore;
import org.xeslite.parser.XesLiteXmlParser;

public class LogUtilitesUnitTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Ignore("Test fails because Guava's Ints.fromBytes method is missing.")
    @Test
    public void addStartEndEvents() throws Exception {

        XFactory factory = new XFactoryExternalStore.InMemoryStoreImpl();
        XesLiteXmlParser parser = new XesLiteXmlParser(factory, false);
        List<XLog> parsedLog = parser.parse(new GZIPInputStream(LogUtilitesUnitTest.class.getClassLoader().getResourceAsStream("BPI13.xes.gz")));

        XLog log = parsedLog.iterator().next();

        LogUtilites.addStartEndEvents(log);

    }
}
