package org.apromore.cache.ehcache;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
//import de.javakaffee.kryoserializers.UUIDSerializer;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.*;
import org.deckfour.xes.model.impl.*;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class EhcacheXLogSerializer implements Serializer<XLog> {

//    private final Serializer<XLog> serializer;

    private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(
                    new StdInstantiatorStrategy()));
            return kryo;
        };
    };

    private static Kryo createKryo(boolean isObjectSerializer) {
        Kryo kryo = new Kryo();
        // handle classes with missing default constructors
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        // supports addition of fields if the @since annotation is used
        kryo.setDefaultSerializer(VersionFieldSerializer.class);

        if (!isObjectSerializer) {
            // For performance reasons, and to avoid memory use, assume documents do not
            // require object graph serialization with duplicate or recursive references
            kryo.setReferences(false);
        } else {
            // To avoid monotonic increase of memory use, due to reference tracking, we must
            // reset after each use.
            kryo.setAutoReset(true);
        }

        kryo.setWarnUnregisteredClasses(true);

        kryo.register(org.eclipse.collections.impl.set.mutable.UnifiedSet.class);
        kryo.register(UUID.class, new UUIDSerializer());
        kryo.register(XAttributeMapImpl.class);
        kryo.register(XAttributeMapLazyImpl.class);
        kryo.register(java.net.URI.class);
        kryo.register(java.util.Date.class);
        kryo.register(XLogImpl.class);
        kryo.register(XTraceImpl.class, new EhcacheXTraceSerializer());
        kryo.register(XEventImpl.class);
        kryo.register(XElement.class);
        kryo.register(XAttributeContinuousImpl.class);
        kryo.register(XAttributeLiteralImpl.class);
        kryo.register(org.deckfour.xes.extension.std.XOrganizationalExtension.class);
        kryo.register(org.eclipse.collections.impl.set.mutable.UnifiedSet.class);
        kryo.register(XLifecycleExtension.class);
        kryo.register(org.deckfour.xes.id.XID.class);
        kryo.register(XLogImpl.class);
        kryo.register(XTraceImpl.class);
        kryo.register(XEventImpl.class);
        kryo.register(XAttributeBooleanImpl.class);
        kryo.register(XAttributeCollectionImpl.class);
        kryo.register(XAttributeContainerImpl.class);
        kryo.register(XAttributeDiscreteImpl.class);
        kryo.register(XAttributeIDImpl.class);
        kryo.register(XAttributeListImpl.class);
        kryo.register(XAttributeLiteralImpl.class);
        kryo.register(XAttributeTimestampImpl.class);
        kryo.register(XAttributeImpl.class);
        kryo.register(XsDateTimeFormat.class);
        kryo.register(XExtension.class);
        kryo.register(XLifecycleExtension.class);
        kryo.register(org.deckfour.xes.extension.std.XConceptExtension.class);
        kryo.register(org.deckfour.xes.extension.std.XTimeExtension.class);
        kryo.register(org.deckfour.xes.extension.std.XIdentityExtension.class);
        kryo.register(XAttributable.class);
        kryo.register(java.util.HashSet.class);

        return kryo;
    }



    public EhcacheXLogSerializer(ClassLoader classLoader) {

    }

    @Override
    public ByteBuffer serialize(XLog object) throws SerializerException {

        Kryo kryo = createKryo(true);

        ByteArrayOutputStream byteArrayOutputStream =
                new ByteArrayOutputStream();
//        DeflaterOutputStream deflaterOutputStream =
//                new DeflaterOutputStream(byteArrayOutputStream);
        Output output = new Output(byteArrayOutputStream);

        kryo.writeObject(output, object.getAttributes());
        output.writeInt(object.size(), true);
        for(XTrace xTrace : object){
            kryo.writeObject(output, xTrace);
        }
//        kryo.writeObject(output, object);
        System.out.println("**************** Kryo serialisation size: " + output.toBytes().length);
//        output.flush();
        output.close();

        //compression off
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return ByteBuffer.wrap(bytes);

        // compression on
//        byte[] bytes = byteArrayOutputStream.toByteArray();
//        try {
//            byte[] compressBytes = Snappy.compress(bytes);
//            return ByteBuffer.wrap(compressBytes);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
    }

    @Override
    public XLog read(ByteBuffer byteBuffer) throws SerializerException, ClassNotFoundException {

        Kryo kryo = createKryo(true);

        //compression off
        Input input =  new Input(new ByteBufferInputStream(byteBuffer)) ;

        XAttributeMap attributeMap = kryo.readObject(input, XAttributeMapImpl.class);
        final int size = input.readInt(true);
        final XLog xLog = new XLogImpl(attributeMap);
        for (int i = 0; i < size; ++i) {
            xLog.add(i, kryo.readObject(input, XTraceImpl.class));
        }

        return xLog;

        // compression on
//        byte[] bytes = new byte[byteBuffer.remaining()];
//        byteBuffer.get(bytes);
//        try {
//            byte[] srcBytes = Snappy.uncompress(bytes);
//
//            Input input = new Input(new ByteBufferInputStream(ByteBuffer.wrap(srcBytes)));
//            return kryo.readObject(input, XLogImpl.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
    }

    @Override
    public boolean equals(final org.deckfour.xes.model.XLog object, final ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return object.equals(read(binary));
    }
//    public boolean equals(T object, ByteBuffer binary) throws SerializerException, ClassNotFoundException {
//        return serializer.equals(object, binary);
//    }

}
