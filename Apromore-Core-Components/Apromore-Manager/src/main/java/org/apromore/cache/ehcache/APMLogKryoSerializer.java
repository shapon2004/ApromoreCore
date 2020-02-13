package org.apromore.cache.ehcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apromore.apmlog.APMLog;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.*;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.xerial.snappy.Snappy;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class APMLogKryoSerializer  implements Serializer<APMLog>, Closeable {

    //    protected static final Kryo kryo = new Kryo( new ListReferenceResolver());
    protected static final Kryo kryo = new Kryo();



    protected Map<Class, Integer> objectHeaderMap = new HashMap<Class, Integer>();  // <1>

    public APMLogKryoSerializer() {
    }

    public APMLogKryoSerializer(ClassLoader loader) {
    }


    @Override
    public ByteBuffer serialize(APMLog object) throws SerializerException {
        kryo.setWarnUnregisteredClasses(true);
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        kryo.register(org.apromore.apmlog.APMLog.class);
        kryo.register(org.eclipse.collections.impl.bimap.mutable.HashBiMap.class);
        kryo.register(org.eclipse.collections.impl.map.mutable.UnifiedMap.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(org.apromore.apmlog.ATrace.class);
        kryo.register(org.apromore.apmlog.AActivity.class);
        kryo.register(org.apromore.apmlog.AEvent.class);
        kryo.register(org.eclipse.collections.impl.set.mutable.UnifiedSet.class);

        ByteArrayOutputStream byteArrayOutputStream =
                new ByteArrayOutputStream();

        Output output = new Output(byteArrayOutputStream);

        kryo.writeObject(output, object);
        System.out.println("**************** Kryo serialisation size: " + output.toBytes().length);
//        output.flush();
        output.close();

        //compression off
//        byte[] bytes = byteArrayOutputStream.toByteArray();
//        return ByteBuffer.wrap(bytes);

        // compression on
        byte[] bytes = byteArrayOutputStream.toByteArray();
        try {
            byte[] compressBytes = Snappy.compress(bytes);
            return ByteBuffer.wrap(compressBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public APMLog read(final ByteBuffer byteBuffer) throws ClassNotFoundException, SerializerException {

        kryo.setWarnUnregisteredClasses(true);
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        kryo.register(org.apromore.apmlog.APMLog.class);
        kryo.register(org.eclipse.collections.impl.bimap.mutable.HashBiMap.class);
        kryo.register(org.eclipse.collections.impl.map.mutable.UnifiedMap.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(org.apromore.apmlog.ATrace.class);
        kryo.register(org.apromore.apmlog.AActivity.class);
        kryo.register(org.apromore.apmlog.AEvent.class);
        kryo.register(org.eclipse.collections.impl.set.mutable.UnifiedSet.class);

        //compression off
//        Input input =  new Input(new ByteBufferInputStream(byteBuffer)) ;
//        return kryo.readObject(input, XLogImpl.class);

        // compression on
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        try {
            byte[] srcBytes = Snappy.uncompress(bytes);

            Input input =  new Input(new ByteBufferInputStream(ByteBuffer.wrap(srcBytes))) ;
            return kryo.readObject(input, APMLog.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(final APMLog object, final ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return object.equals(read(binary));
    }

    @Override
    public void close() throws IOException {
        objectHeaderMap.clear();
    }

}
