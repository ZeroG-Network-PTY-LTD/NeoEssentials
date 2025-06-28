package com.zerog.neoessentials.util;

import com.zerog.neoessentials.NeoEssentials;

import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * YAML utility class that provides a simplified interface to SnakeYAML.
 * Uses the bundled SnakeYAML directly without complex relocation.
 */
public class YamlUtil {
    private static final String YAML_CLASS = "org.yaml.snakeyaml.Yaml";
    private static final String DUMPER_OPTIONS_CLASS = "org.yaml.snakeyaml.DumperOptions";
    
    private final Object yamlInstance;
    private final Class<?> yamlClass;
    private final Class<?> dumperOptionsClass;
    
    /**
     * Create a new YAML utility instance
     */
    public YamlUtil() {
        this(null);
    }
    
    /**
     * Create a new YAML utility instance with dumper options
     * 
     * @param dumperOptions Dumper options (can be null)
     */
    public YamlUtil(Object dumperOptions) {
        try {
            yamlClass = Class.forName(YAML_CLASS);
            dumperOptionsClass = Class.forName(DUMPER_OPTIONS_CLASS);
            NeoEssentials.LOGGER.info("Using bundled SnakeYAML");
            
            if (dumperOptions != null) {
                Constructor<?> constructor = yamlClass.getConstructor(dumperOptionsClass);
                yamlInstance = constructor.newInstance(dumperOptions);
            } else {
                yamlInstance = yamlClass.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize YAML utility", e);
        }
    }
    
    /**
     * Load YAML from an InputStream
     * 
     * @param inputStream The input stream to load from
     * @return The loaded object
     */
    @SuppressWarnings("unchecked")
    public <T> T load(InputStream inputStream) {
        try {
            Method loadMethod = yamlClass.getMethod("load", InputStream.class);
            return (T) loadMethod.invoke(yamlInstance, inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from InputStream", e);
        }
    }
    
    /**
     * Load YAML from a String
     * 
     * @param yamlString The YAML string to load
     * @return The loaded object
     */
    @SuppressWarnings("unchecked")
    public <T> T load(String yamlString) {
        try {
            Method loadMethod = yamlClass.getMethod("load", String.class);
            return (T) loadMethod.invoke(yamlInstance, yamlString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from String", e);
        }
    }
    
    /**
     * Dump an object to YAML string
     * 
     * @param data The object to dump
     * @return The YAML string representation
     */
    public String dump(Object data) {
        try {
            Method dumpMethod = yamlClass.getMethod("dump", Object.class);
            return (String) dumpMethod.invoke(yamlInstance, data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to dump object to YAML", e);
        }
    }
    
    /**
     * Dump an object to a Writer
     * 
     * @param data The object to dump
     * @param output The writer to write to
     */
    public void dump(Object data, Writer output) {
        try {
            Method dumpMethod = yamlClass.getMethod("dump", Object.class, Writer.class);
            dumpMethod.invoke(yamlInstance, data, output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to dump object to Writer", e);
        }
    }
    
    /**
     * Create dumper options
     * 
     * @return A new dumper options instance
     */
    public Object createDumperOptions() {
        try {
            return dumperOptionsClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dumper options", e);
        }
    }
    
    /**
     * Set the default flow style for dumper options
     * 
     * @param dumperOptions The dumper options instance
     * @param flowStyle The flow style enum value
     */
    public void setDefaultFlowStyle(Object dumperOptions, String flowStyle) {
        try {
            // Get the FlowStyle enum class
            String enumClassName = dumperOptionsClass.getPackage().getName() + ".DumperOptions$FlowStyle";
            Class<?> flowStyleEnum = Class.forName(enumClassName);
            
            // Get the enum value
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object flowStyleValue = Enum.valueOf((Class<Enum>) flowStyleEnum, flowStyle);
            
            // Set the default flow style
            Method setMethod = dumperOptionsClass.getMethod("setDefaultFlowStyle", flowStyleEnum);
            setMethod.invoke(dumperOptions, flowStyleValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set default flow style", e);
        }
    }
    
    /**
     * Set pretty flow for dumper options
     * 
     * @param dumperOptions The dumper options instance
     * @param prettyFlow Whether to use pretty flow
     */
    public void setPrettyFlow(Object dumperOptions, boolean prettyFlow) {
        try {
            Method setMethod = dumperOptionsClass.getMethod("setPrettyFlow", boolean.class);
            setMethod.invoke(dumperOptions, prettyFlow);
        } catch (Exception e) {
            // Not all versions of SnakeYAML have this method, ignore if not available
            NeoEssentials.LOGGER.debug("setPrettyFlow method not available in this SnakeYAML version");
        }
    }
}
