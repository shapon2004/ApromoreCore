package org.apromore.service.impl;

import org.apromore.xes.extension.std.XConceptExtension;
import org.apromore.xes.extension.std.XLifecycleExtension;
import org.apromore.xes.extension.std.XOrganizationalExtension;
import org.apromore.xes.extension.std.XTimeExtension;
import org.apromore.xes.factory.XFactory;
import org.apromore.xes.model.XTrace;
import org.apromore.xes.model.impl.XAttributeLiteralImpl;
import org.junit.Before;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;
import java.util.*;

import static org.junit.Assert.assertNotNull;

public class EventLogServiceImplTestAbstract {



    protected static final int TEST_SIZE = 1000;

    private static final int RANDOM_POOL_SIZE = 65536;
    private static final int EVENT_POOL_SIZE = 32;

    private Random random;
    private String[] randomPool;
    private String[] eventPool;

    interface AttributeNamingStrategy {
        String createAttributeName();
    }

    @Before
    public void setUp() {
        random = new Random(42);
        randomPool = new String[RANDOM_POOL_SIZE];
        for (int i = 0; i < RANDOM_POOL_SIZE; i++) {
            randomPool[i] = randomString(random, 20);
        }
        eventPool = new String[EVENT_POOL_SIZE];
        for (int i = 0; i < EVENT_POOL_SIZE; i++) {
            eventPool[i] = "Event "+i;
        }
    }

    private String randomString(Random random, final int length) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            char c = (char)(random.nextInt((int)(Character.MAX_VALUE)));
            sb.append(c);
        }
        return sb.toString();
    }

    private String getRandomString() {
        return randomPool[random.nextInt(RANDOM_POOL_SIZE)];
    }

    private String getRandomEvent() {
        return eventPool[random.nextInt(EVENT_POOL_SIZE)];
    }

    protected void readRandom(org.apromore.xes.model.XLog log) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        long startTime = System.nanoTime();
        System.out.println("Reading all attributes via values() of random traces & events: ");
        long attributeCounter = 0;
        int traceCounter = 0;
        while (traceCounter < log.size()) {
            org.apromore.xes.model.XTrace trace = log.get(random.nextInt(log.size()));
            int eventCounter = 0;
            while (eventCounter < trace.size()) {
                org.apromore.xes.model.XEvent event = trace.get(random.nextInt(trace.size()));
                for (String key : event.getAttributes().keySet()) {
                    org.apromore.xes.model.XAttribute attr = event.getAttributes().get(key);
                    assertNotNull(attr);
                    if (attr instanceof org.apromore.xes.model.XAttributeBoolean) {
                        assertNotNull(((org.apromore.xes.model.XAttributeBoolean) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeLiteral) {
                        assertNotNull(((org.apromore.xes.model.XAttributeLiteral) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeContinuous) {
                        assertNotNull(((org.apromore.xes.model.XAttributeContinuous) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeDiscrete) {
                        assertNotNull(((org.apromore.xes.model.XAttributeDiscrete) attr).getValue());
                    }
                    attributeCounter++;
                }
                eventCounter++;
            }
            traceCounter++;
        }
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedNanos / 1000000 + " ms");
        double elapsedSecond = (System.nanoTime() - startTime) / 1000000000.0;
        double attributesPerSecond = attributeCounter / elapsedSecond;
        System.out.println(numberFormat.format(attributesPerSecond) + " APS");
        System.gc();
        System.out.println("Memory Used: " + getMemoryUsage().getUsed() / 1024 / 1024 + " MB ");
    }

    protected void readSequentially(org.apromore.xes.model.XLog log) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        long startTime = System.nanoTime();
        System.out.println("Reading all attributes via values(): ");
        long attributeCounter = 0;
        for (org.apromore.xes.model.XTrace trace : log) {
            for (org.apromore.xes.model.XEvent event : trace) {
                for (org.apromore.xes.model.XAttribute attr : event.getAttributes().values()) {
                    assertNotNull(attr.getKey());
                    if (attr instanceof org.apromore.xes.model.XAttributeBoolean) {
                        assertNotNull(((org.apromore.xes.model.XAttributeBoolean) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeLiteral) {
                        assertNotNull(((org.apromore.xes.model.XAttributeLiteral) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeContinuous) {
                        assertNotNull(((org.apromore.xes.model.XAttributeContinuous) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeDiscrete) {
                        assertNotNull(((org.apromore.xes.model.XAttributeDiscrete) attr).getValue());
                    } else if (attr instanceof org.apromore.xes.model.XAttributeTimestamp) {
                        assertNotNull(((org.apromore.xes.model.XAttributeTimestamp) attr).getValue());
                    }
                    attributeCounter++;
                }
            }
        }
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedNanos / 1000000 + " ms");
        double elapsedSecond = (System.nanoTime() - startTime) / 1000000000.0;
        double attributesPerSecond = attributeCounter / elapsedSecond;
        System.out.println(numberFormat.format(attributesPerSecond) + " APS");
        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        System.out.println("Memory Used: " + getMemoryUsage().getUsed() / 1024 / 1024 + " MB ");
    }

    protected void readSequentiallyCommon(org.apromore.xes.model.XLog log) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        long startTime = System.nanoTime();
        System.out.println("Reading concept:name, lifecycle:transition, org:resource: ");
        long attributeCounter = 0;
        for (org.apromore.xes.model.XTrace trace : log) {
            for (org.apromore.xes.model.XEvent event : trace) {
                org.apromore.xes.model.XAttributeMap attributes = event.getAttributes();
                assertNotNull(attributes.get(XConceptExtension.KEY_NAME));
                assertNotNull(attributes.get(XLifecycleExtension.KEY_TRANSITION));
                assertNotNull(attributes.get(XTimeExtension.KEY_TIMESTAMP));
                attributeCounter = attributeCounter + 3;
            }
        }
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedNanos / 1000000 + " ms");
        double elapsedSecond = (System.nanoTime() - startTime) / 1000000000.0;
        double attributesPerSecond = attributeCounter / elapsedSecond;
        System.out.println(numberFormat.format(attributesPerSecond) + " APS");
        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        System.out.println("Memory Used: " + getMemoryUsage().getUsed() / 1024 / 1024 + " MB ");
    }

    protected void changeAttributes(org.apromore.xes.model.XLog log) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        long startTime = System.nanoTime();
        System.out.println("Changing all attributes via entrySet(): ");
        long attributeCounter = 0;
        for (org.apromore.xes.model.XTrace trace : log) {
            for (org.apromore.xes.model.XEvent event : trace) {
                for (Map.Entry<String, org.apromore.xes.model.XAttribute> attrEntry : event.getAttributes().entrySet()) {
                    if (attrEntry.getValue() instanceof org.apromore.xes.model.XAttributeBoolean) {
                        attrEntry.setValue(new org.apromore.xes.model.impl.XAttributeBooleanImpl(attrEntry.getKey(),
                                !((org.apromore.xes.model.XAttributeBoolean) attrEntry.getValue()).getValue(),
                                attrEntry.getValue().getExtension()));
                    } else if (attrEntry.getValue() instanceof org.apromore.xes.model.XAttributeLiteral) {
                        if (attrEntry.getKey().equals(XConceptExtension.KEY_NAME)) {
                            attrEntry.setValue(new org.apromore.xes.model.impl.XAttributeLiteralImpl(attrEntry.getKey(),
                                    ((org.apromore.xes.model.XAttributeLiteral) attrEntry.getValue()).getValue().concat(" NEW"),
                                    attrEntry.getValue().getExtension()));
                        } else {
                            attrEntry.setValue(new XAttributeLiteralImpl(attrEntry.getKey(), getRandomString(),
                                    attrEntry.getValue().getExtension()));
                        }
                    } else if (attrEntry.getValue() instanceof org.apromore.xes.model.XAttributeContinuous) {
                        attrEntry.setValue(new org.apromore.xes.model.impl.XAttributeContinuousImpl(attrEntry.getKey(), random.nextDouble(),
                                attrEntry.getValue().getExtension()));
                    } else if (attrEntry.getValue() instanceof org.apromore.xes.model.XAttributeDiscrete) {
                        attrEntry.setValue(new org.apromore.xes.model.impl.XAttributeDiscreteImpl(attrEntry.getKey(), random.nextInt(),
                                attrEntry.getValue().getExtension()));
                    } else if (attrEntry.getValue() instanceof org.apromore.xes.model.XAttributeTimestamp) {
                        attrEntry.setValue(new org.apromore.xes.model.impl.XAttributeTimestampImpl(attrEntry.getKey(), Math.abs(random.nextLong()),
                                attrEntry.getValue().getExtension()));
                    }
                    attributeCounter++;
                }
            }
        }
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedNanos / 1000000 + " ms");
        double elapsedSecond = (System.nanoTime() - startTime) / 1000000000.0;
        double attributesPerSecond = attributeCounter / elapsedSecond;
        System.out.println(numberFormat.format(attributesPerSecond) + " APS");
        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        System.out.println("Memory Used: " + getMemoryUsage().getUsed() / 1024 / 1024 + " MB ");
    }

    protected org.apromore.xes.model.XLog createRandomLog(XFactory factory, int traces) {
        return createLog(factory, 40, traces, 2);
    }

    public org.apromore.xes.model.XLog createLog(XFactory factory, int events, int traces, int attributes) {
        return createLog(factory, events, traces, attributes, false);
    }

    public org.apromore.xes.model.XLog createLog(XFactory factory, int events, int traces, int attributes, boolean minimalAttributes) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        long startTime = System.nanoTime();
        System.out.println("Creating with " + factory.getClass().getSimpleName() + ": ");
        System.out.println(integerFormat.format(traces) + " traces");
        System.out.println(integerFormat.format(events * traces) + " events");
        int standardAttributes = minimalAttributes ? 1 : 4;
        long totalAttributes = events * traces * ((attributes * 4) + standardAttributes);
        System.out.println(integerFormat.format(totalAttributes) + " attributes");
        org.apromore.xes.model.XLog testLog = factory.createLog();
        int traceCounter = 0;
        while (traceCounter++ < traces) {
            XTrace t = factory.createTrace();
            if (!minimalAttributes) {
                addAttribute(t, factory.createAttributeLiteral(XConceptExtension.KEY_NAME, "Trace " + traceCounter,
                        XConceptExtension.instance()));
            }
            createAttributes(factory, t, attributes, traceCounter);

            int eventCounter = 0;
            List<org.apromore.xes.model.XEvent> eventList = new ArrayList<>();
            while (eventCounter++ < events) {
                org.apromore.xes.model.XEvent e = factory.createEvent();
                createAttributes(factory, e, attributes, eventCounter);
                if (!minimalAttributes) {
                    addAttribute(e, factory.createAttributeTimestamp(XTimeExtension.KEY_TIMESTAMP,
                            (new Date().getTime() + eventCounter + traceCounter), XTimeExtension.instance()));
                    addAttribute(e, factory.createAttributeLiteral(XLifecycleExtension.KEY_TRANSITION,
                            XLifecycleExtension.StandardModel.COMPLETE.getEncoding(), XLifecycleExtension.instance()));
                    addAttribute(e, factory.createAttributeLiteral(XOrganizationalExtension.KEY_RESOURCE,
                            "Resource " + eventCounter, XOrganizationalExtension.instance()));
                }
                addAttribute(e, factory.createAttributeLiteral(XConceptExtension.KEY_NAME, getRandomEvent(),
                        XConceptExtension.instance()));
                eventList.add(e);
            }
            t.addAll(eventList);
            testLog.add(t);
        }
        long elapsedNanos = System.nanoTime() - startTime;
        System.out.println("Elapsed time: " + elapsedNanos / 1000000 + " ms");
        double elapsedSecond = (System.nanoTime() - startTime) / 1000000000.0;
        double attributesPerSecond = (traceCounter * events * (attributes * 4 + 3)) / elapsedSecond;
        System.out.println(numberFormat.format(attributesPerSecond) + " APS");
        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        System.out.println("Memory Used: " + getMemoryUsage().getUsed() / 1024 / 1024 + " MB ");
        return testLog;
    }

    protected MemoryUsage getMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        return memoryMXBean.getHeapMemoryUsage();
    }

    private void createAttributes(XFactory factory, org.apromore.xes.model.XAttributable a, int attributes, int currentIndex) {
        int attributesCounter = 0;
        while (attributesCounter++ < attributes) {
            addAttribute(a, factory.createAttributeBoolean("boolean" + attributesCounter, random.nextBoolean(), null));
            addAttribute(a,
                    factory.createAttributeDiscrete("discrete" + attributesCounter, random.nextInt(100_000), null));
            addAttribute(a,
                    factory.createAttributeContinuous("continuous" + attributesCounter, random.nextDouble(), null));
            addAttribute(a, factory.createAttributeLiteral("literal" + attributesCounter, getRandomString(), null));
        }
    }

    private void addAttribute(org.apromore.xes.model.XAttributable a, org.apromore.xes.model.XAttribute attr) {
        a.getAttributes().put(attr.getKey(), attr);
    }

}
