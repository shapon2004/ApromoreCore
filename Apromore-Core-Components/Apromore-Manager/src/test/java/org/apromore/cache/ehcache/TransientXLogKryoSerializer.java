package org.apromore.cache.ehcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.*;
import org.apromore.xes.extension.std.XLifecycleExtension;
import org.apromore.xes.model.impl.*;
import org.apromore.xes.model.impl.XLogImpl;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
// tag::thirdPartyTransientSerializer[]
public class TransientXLogKryoSerializer implements Serializer<XLogImpl>, Closeable{

    protected static final Kryo kryo = new Kryo();

    protected Map<Class, Integer> objectHeaderMap = new HashMap<Class, Integer>();  // <1>

    public TransientXLogKryoSerializer() {
    }

    public TransientXLogKryoSerializer(ClassLoader loader) {

//        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
//
//        kryo.register( Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );

//        populateObjectHeadersMap(kryo.register(XLog.class));  // <2>

        kryo.register(XLogImpl.class);

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
    public ByteBuffer serialize(XLogImpl object) throws SerializerException {
//        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        kryo.register(XLogImpl.class);
        kryo.register(XTraceImpl.class);
        kryo.register(XEventImpl.class);
        kryo.register(XAttributeBooleanImpl.class);
        kryo.register(XAttributeCollectionImpl.class);
        kryo.register(XAttributeContainerImpl.class);
        kryo.register(XAttributeContinuousImpl.class);
        kryo.register(XAttributeDiscreteImpl.class);
        kryo.register(XAttributeIDImpl.class);
        kryo.register(XAttributeListImpl.class);
        kryo.register(XAttributeLiteralImpl.class);
        kryo.register(XAttributeMapImpl.class);
        kryo.register(XAttributeTimestampImpl.class);
        kryo.register(XAttributeImpl.class);
        kryo.register(XAttributeMapLazyImpl.class);
        kryo.register(XsDateTimeFormat.class);

        kryo.register(XLifecycleExtension.class);
        kryo.register(org.eclipse.collections.impl.set.mutable.UnifiedSet.class);

//        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );
        kryo.register( Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );
        kryo.register( Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() );
        kryo.register( Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() );
        kryo.register( Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer() );
        kryo.register( Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer() );
        kryo.register( Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer() );
        UnmodifiableCollectionsSerializer.registerSerializers( kryo );
        SynchronizedCollectionsSerializer.registerSerializers( kryo );

        Output output = new Output(new ByteArrayOutputStream());
        kryo.writeObject(output, object);
        output.close();

        return ByteBuffer.wrap(output.getBuffer());
    }

    @Override
    public XLogImpl read(final ByteBuffer binary) throws ClassNotFoundException, SerializerException {
//        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

//        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

//        ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());

//        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

//        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );
        kryo.register( Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );
        kryo.register( Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() );
        kryo.register( Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() );
        kryo.register( Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer() );
        kryo.register( Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer() );
        kryo.register( Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer() );
        UnmodifiableCollectionsSerializer.registerSerializers( kryo );
        SynchronizedCollectionsSerializer.registerSerializers( kryo );


        Input input =  new Input(new ByteBufferInputStream(binary)) ;
        return kryo.readObject(input, XLogImpl.class);
    }

    @Override
    public boolean equals(final XLogImpl object, final ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return object.equals(read(binary));
    }

    @Override
    public void close() throws IOException {
        objectHeaderMap.clear();
    }

}
// end::thirdPartyTransientSerializer[]