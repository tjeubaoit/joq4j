package org.joq4j.config;

import com.google.common.base.Preconditions;
import org.joq4j.common.utils.DateTimes;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;

/**
 * @author <a href="https://github.com/tjeubaoit">tjeubaoit</a>
 */
public interface Configurable {

    default void configure(Config conf) {
        Class<?> cls = this.getClass();
        for (Field field : cls.getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigDescriptor.class)) continue;
            try {
                field.setAccessible(true);
                ConfigDescriptor descriptor = field.getAnnotation(ConfigDescriptor.class);
                String name = descriptor.name();
                String defVal = descriptor.defaultValue();
                if (!conf.containsKey(name) && !descriptor.allowMissing()) {
                    throw new ConfigException("Missing config value for key: " + name);
                }

                Class<?> type = field.getType();
                if (type.isAssignableFrom(Integer.class) || type.equals(int.class)) {
                    int val = conf.getInt(name, defVal.equals("") ? 0 : Integer.parseInt(defVal));
                    validateNumberField(descriptor, val);
                    field.set(this, val);

                } else if (type.isAssignableFrom(Long.class) || type.equals(long.class)) {
                    long val = conf.getLong(name, defVal.equals("")?0:Long.parseLong(defVal));
                    validateNumberField(descriptor, val);
                    field.set(this, val);

                } else if (type.isAssignableFrom(Boolean.class) || type.equals(boolean.class)) {
                    field.set(this, conf.getBool(name, !defVal.equals("")
                            && Boolean.parseBoolean(defVal)));

                } else if (type.isAssignableFrom(Double.class) || type.equals(double.class)) {
                    double val = conf.getDouble(name, defVal.equals("") ? 0 : Double.parseDouble(defVal));
                    validateNumberField(descriptor, val);
                    field.set(this, val);

                } else if (type.isAssignableFrom(Float.class) || type.equals(float.class)) {
                    float val = conf.getFloat(name, defVal.equals("") ? 0 : Float.parseFloat(defVal));
                    validateNumberField(descriptor, val);
                    field.set(this, val);

                } else if (type.isAssignableFrom(Date.class)) {
                    Date defDate = DateTimes.parse(defVal, descriptor.datetimeFormat());
                    field.set(this, conf.getDateTime(name, descriptor.datetimeFormat(), defDate));

                } else if (type.isAssignableFrom(Collection.class)) {
                    field.set(this, conf.getList(name));

                } else {
                    field.set(this, conf.getString(name, defVal));
                }
            } catch (Exception e) {
                throw new ConfigException(e);
            } finally {
                field.setAccessible(false);
            }
        }
    }

    static void validateNumberField(ConfigDescriptor descriptor, long val) {
        long max = (long) descriptor.maxValue();
        long min = (long) descriptor.minValue();
        Preconditions.checkArgument(val <= max && val >= min,
                "Field value %s is not in range (%s, %s)", val, min, max);
    }

    static void validateNumberField(ConfigDescriptor descriptor, double val) {
        double max = descriptor.maxValue();
        double min = descriptor.minValue();
        Preconditions.checkArgument(val <= max && val >= min,
                "Field value %s is not in range (%s, %s)", val, min, max);
    }
}
