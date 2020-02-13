package org.apromore.cache.ehcache;

import com.esotericsoftware.kryo.Kryo;
import org.deckfour.xes.model.XLog;
import org.ehcache.impl.serialization.TransientStateRepository;
import org.ehcache.spi.serialization.SerializerException;
import org.ehcache.impl.serialization.CompactJavaSerializer;
import org.ehcache.spi.serialization.Serializer;

import java.nio.ByteBuffer;

public class EhcacheSerializer implements Serializer<XLog>  {

    private final Serializer<XLog> serializer;

    protected static final Kryo kryo = new Kryo();

    public EhcacheSerializer(ClassLoader classLoader) {
        CompactJavaSerializer<XLog> compactJavaSerializer = new CompactJavaSerializer<>(classLoader);
        compactJavaSerializer.init(new TransientStateRepository());
        serializer = compactJavaSerializer;
    }

    @Override
    public ByteBuffer serialize(XLog object) throws SerializerException {

        kryo.setWarnUnregisteredClasses(true);

        return serializer.serialize(object);
    }

    @Override
    public XLog read(ByteBuffer binary) throws SerializerException, ClassNotFoundException {
        return serializer.read(binary);
    }

    @Override
    public boolean equals(XLog object, ByteBuffer binary) throws SerializerException, ClassNotFoundException {
        return serializer.equals(object, binary);
    }

}
