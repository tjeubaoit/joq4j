package org.joq4j.encoding;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unchecked")
public class JacksonMessageEncoder implements MessageEncoder {

    static final ObjectMapper om = new ObjectMapper();

    @Override
    public <T> void write(T input, OutputStream out) {
        try {
            om.writeValue(out, input);
        } catch (IOException e) {
            throw new EncodingException(e);
        }
    }

    @Override
    public <T> T read(InputStream in) {
        try {
            return (T) om.readValue(in, Object.class);
        } catch (IOException e) {
            throw new EncodingException(e);
        }
    }
}
