package org.joq4j.config;

import org.joq4j.common.utils.DateTimes;
import org.joq4j.common.utils.Strings;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="https://github.com/tjeubaoit">tjeubaoit</a>
 */
public class Config {

    private static final String LIST_DELIMITER = ",";
    private static final String CONF_ENV = "joq4j.conf";
    private static final String DEFAULT_NAME = "joq4j.properties";

    /**
     * @param path Path to file use as default resource to load properties
     *             when init new Config object with empty constructor
     */
    public static void setDefaultResourcePath(String path) {
        defaultResPath = path;
    }

    private static final Properties defaultProps = new Properties();
    private static String defaultResPath = System.getProperty(CONF_ENV);

    static {
        try {
            // default properties from resource file
            defaultProps.load(Config.class.getClassLoader().getResourceAsStream(DEFAULT_NAME));
        } catch (IOException e) {
            Throwable t = new IOException(Strings.format("Cannot find %s in classpath", DEFAULT_NAME));
            t.printStackTrace(new PrintStream(System.out));
        }
    }

    private final Properties inst = new Properties(defaultProps);

    public Config() {
        this(defaultResPath);
    }

    public Config(String path) {
        try {
            // extra properties from file file will have higher priority
            addResource(path);
        } catch (IOException e) {
            throw new ConfigException(e);
        }
    }

    public Config(InputStream is) {
        try {
            addResource(is);
        } catch (IOException e) {
            throw new ConfigException(e);
        }
    }

    public void addResource(InputStream is, boolean closeAfterLoad) throws IOException {
        try {
            inst.load(is);
        } finally {
            if (closeAfterLoad) is.close();
        }
    }

    public void addResource(InputStream is) throws IOException {
        this.addResource(is, false);
    }

    public void addResource(String path) throws IOException {
        if (path != null) {
            this.addResource(new FileInputStream(path), true);
        }
    }

    public boolean containsKey(String key) {
        return this.inst.containsKey(key);
    }

    public <T> void set(String key, T val) {
        inst.setProperty(key, val.toString());
    }

    public void setDateTime(String key, Date date) {
        setDateTime(key, date, DateTimes.ISO_FORMAT);
    }

    public void setDateTime(String key, Date date, String format) {
        inst.setProperty(key, DateTimes.format(date, format));
    }

    public void setCollection(String key, String... elements) {
        setCollection(key, Arrays.asList(elements));
    }

    public void setCollection(String key, Collection<String> coll) {
        inst.setProperty(key, Strings.join(coll, LIST_DELIMITER));
    }

    public void setClass(String key, Class<?> cls) {
        inst.setProperty(key, cls.getName());
    }

    public void setClasses(String key, Class<?>... classes) {
        setClasses(key, Arrays.asList(classes));
    }

    public void setClasses(String key, Collection<Class<?>> classes) {
        List<String> clsNames = new ArrayList<>(classes.size());
        for (Class<?> cls : classes) {
            clsNames.add(cls.getName());
        }
        setCollection(key, clsNames);
    }

    public String getString(String key) {
        String val = getProperty(key);
        if (Strings.isNullOrEmpty(val)) {
            throw new ConfigException(new NoSuchElementException());
        }
        return val;
    }

    public String getString(String key, String defVal) {
        return getProperty(key, defVal);
    }

    public String[] getStringArray(String key) {
        return getString(key).split(",");
    }

    public int getInt(String key) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public int getInt(String key, int defVal) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public long getLong(String key) {
        try {
            return Long.parseLong(getProperty(key));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public long getLong(String key, long defVal) {
        try {
            return Long.parseLong(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public BigInteger getBigInt(String key) {
        try {
            return new BigInteger(getProperty(key));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public BigInteger getBigInt(String key, BigInteger defVal) {
        try {
            return new BigInteger(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(getProperty(key));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public double getDouble(String key, double defVal) {
        try {
            return Double.parseDouble(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public float getFloat(String key) {
        try {
            return Float.parseFloat(getProperty(key));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public float getFloat(String key, float defVal) {
        try {
            return Float.parseFloat(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public boolean getBool(String key) {
        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public boolean getBool(String key, boolean defVal) {
        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public List<String> getList(String key) {
        try {
            return Arrays.asList(getProperty(key).split(","));
        } catch (Exception e) {
            throw new ConfigException(e);
        }
    }

    public Date getDateTime(String key, Date defVal) {
        return getDateTime(key, DateTimes.ISO_FORMAT, defVal);
    }

    public Date getDateTime(String key, String format, Date defVal) {
        try {
            return DateTimes.parse(getProperty(key), format);
        } catch (Exception e) {
            return defVal;
        }
    }

    public Class<?> getClass(String key, Class<?> defVal) {
        try {
            return Class.forName(getProperty(key));
        } catch (Exception ignored) {
            return defVal;
        }
    }

    public Class<?> getClass(String key) {
        return getClass(key, null);
    }

    public Collection<Class<?>> getClasses(String key) throws ClassNotFoundException {
        Collection<String> clsNames = getList(key);
        List<Class<?>> classes = new ArrayList<>(clsNames.size());
        for (String className : getList(key)) {
            classes.add(Class.forName(className));
        }
        return classes;
    }

    public Properties toProperties() {
        return new Properties(inst);
    }

    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>();
        for (Object key : this.inst.keySet()) {
            keys.add(key.toString());
        }
        for (Object key : defaultProps.keySet()) {
            keys.add(key.toString());
        }
        return keys;
    }

    @Override
    public String toString() {
        Set<Object> keySet = new TreeSet<>(inst.keySet());
        List<Object> keys = new ArrayList<>(keySet);
        keys.sort(Comparator.comparing(Object::toString));

        StringBuilder sb = new StringBuilder("Configuration properties:\n");
        keys.forEach(key -> sb.append(Strings.format("\t%s = %s\n",
                key.toString(), getString(key.toString()))));
        return sb.toString();
    }

    protected String getProperty(String key) {
        if (inst.containsKey(key)) {
            return inst.getProperty(key);
        } else
            return defaultProps.getProperty(key);
    }

    protected String getProperty(String key, String defVal) {
        if (inst.containsKey(key)) {
            return inst.getProperty(key, defVal);
        } else
            return defaultProps.getProperty(key, defVal);
    }
}
