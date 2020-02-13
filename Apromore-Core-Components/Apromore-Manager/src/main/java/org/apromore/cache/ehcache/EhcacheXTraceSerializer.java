package org.apromore.cache.ehcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.model.impl.XTraceImpl;

public class EhcacheXTraceSerializer extends Serializer<XTrace> {

    @Override
    public void write(Kryo kryo, Output output, XTrace xTrace) {
//        output.writeInt(xTrace.getAttributes().size());
        kryo.writeObject(output, xTrace.getAttributes());
        output.writeInt(xTrace.size(), true);
        for (Object elm : xTrace) {
            kryo.writeObject(output, elm);
        }
    }

    @Override
    public XTrace read(Kryo kryo, Input input, Class<XTrace> aClass) {
        XAttributeMap attributeMap = kryo.readObject(input, XAttributeMapImpl.class);
        final int size = input.readInt(true);
        final XTrace xTrace = new XTraceImpl(attributeMap);
        for (int i = 0; i < size; ++i) {
            xTrace.add(i, kryo.readObject(input, XEventImpl.class));
        }
        return xTrace;
    }
}
