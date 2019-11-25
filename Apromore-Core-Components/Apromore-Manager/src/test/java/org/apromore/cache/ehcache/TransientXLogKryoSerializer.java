package org.apromore.cache.ehcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apromore.cache.ehcache.model.Description;
import org.apromore.cache.ehcache.model.Employee;
import org.apromore.cache.ehcache.model.Person;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.*;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
// tag::thirdPartyTransientSerializer[]
public class TransientXLogKryoSerializer implements Serializer<XLog>, Closeable{

    protected static final Kryo kryo = new Kryo();

    protected Map<Class, Integer> objectHeaderMap = new HashMap<Class, Integer>();  // <1>

    public TransientXLogKryoSerializer() {
    }

    public TransientXLogKryoSerializer(ClassLoader loader) {
        populateObjectHeadersMap(kryo.register(XLog.class));  // <2>
        populateObjectHeadersMap(kryo.register(XLogImpl.class));  // <2>
        populateObjectHeadersMap(kryo.register(XTraceImpl.class));  // <3>
        populateObjectHeadersMap(kryo.register(XEventImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeBooleanImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeCollectionImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeContainerImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeContinuousImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeDiscreteImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeIDImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeListImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeLiteralImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeMapImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeTimestampImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XAttributeMapLazyImpl.class)); // <4>
        populateObjectHeadersMap(kryo.register(XsDateTimeFormat.class)); // <4>
    }

    protected void populateObjectHeadersMap(Registration reg) {
        objectHeaderMap.put(reg.getType(), reg.getId());  // <5>
    }

    @Override
    public ByteBuffer serialize(XLog object) throws SerializerException {
        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, object);
        output.close();

        return ByteBuffer.wrap(output.getBuffer());
    }

    @Override
    public XLog read(final ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        Input input =  new Input(new ByteBufferInputStream(binary)) ;
        return kryo.readObject(input, XLog.class);
    }

    @Override
    public boolean equals(final XLog object, final ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return object.equals(read(binary));
    }

    @Override
    public void close() throws IOException {
        objectHeaderMap.clear();
    }

}
// end::thirdPartyTransientSerializer[]