package sample.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.skjolber.jackson.jsh.AnsiSyntaxHighlight;
import com.github.skjolber.jackson.jsh.DefaultSyntaxHighlighter;
import com.github.skjolber.jackson.jsh.SyntaxHighlighter;
import com.github.skjolber.jackson.jsh.SyntaxHighlightingJsonGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Slf4j
@Configuration
public class PropertiesDumper {
    private final static int debugValueLength = 40;

    private final ConfigurableEnvironment environment;
    private Set<String> propertyKeys;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${logging.masked-properties:password}")
    private String maskedProperties;

    @Autowired
    public PropertiesDumper(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void dumpEnvironment() {
        try {
            if (log.isDebugEnabled()) {
                propertyKeys = new HashSet<>();
                Map<String, Object> environmentMap = environmentMap(propertyKeys);
                log.trace("PROPERTY DUMP environment:\n{}\n", toJson(environmentMap, log.isDebugEnabled()));
            } else {
                log.info(AnsiSyntaxHighlight.ESC_START+AnsiSyntaxHighlight.GREEN+AnsiSyntaxHighlight.ESC_END+
                    getClass().getSimpleName().replaceAll("[$].*", "")+" found. "+
                    "Enable dump by setting: logging.level."+getClass().getName().replaceAll("[$].*", "")+
                    ": debug or .trace for an additional environment dump."+AnsiSyntaxHighlight.RESET);
            }
        } catch (RuntimeException e) {
            log.error("Dump failed.", e);
        }
    }

    @EventListener({ ApplicationReadyEvent.class })
    public void dumpResolvedProperties() {
        try {
            if (log.isDebugEnabled()) {
                if (propertyKeys != null) {
                    SortedMap<String, String> resolvedProperties = new TreeMap<>();
                    for (String key : propertyKeys) {
                        String value;
                        try {
                            value = environment.getProperty(key);
                        } catch (Exception e) {
                            try {
                                value = toJson(environment.getProperty(key, Object.class), true);
                            } catch (Exception e2) {
                                value = "[Cannot convert value]";
                            }
                        }
                        if (key.toLowerCase().endsWith("path") && value!=null && value.length() > debugValueLength && !log.isTraceEnabled()) {
                            value = value.substring(0, debugValueLength) + "...";
                        }
                        resolvedProperties.put(key, ""+sanitize(key, value));
                    }
                    log.debug("PROPERTY DUMP resolved properties:\n{}", toJson(resolvedProperties, true));
                }
            }
        } catch (RuntimeException e) {
            log.error("Dump failed", e);
        }
    }

    private Map<String, Object> environmentMap(Set<String> collectedPropertyKeys) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profiles", environment.getActiveProfiles());
        for (Map.Entry<String, PropertySource<?>> entry : getPropertySources().entrySet()) {
            PropertySource<?> source = entry.getValue();
            String sourceName = entry.getKey();
            if (source instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
                Map<String, Object> properties = new TreeMap<>();
                for (String name : enumerable.getPropertyNames()) {
                    try {
                        properties.put(name, sanitize(name, enumerable.getProperty(name)));
                    } catch (RuntimeException e) {
                        log.error("problem with property {}", name);
                    }
                }
                result.put(sourceName, properties);
                propertyKeys.addAll(properties.keySet());
            }
        }
        return result;
    }

    private Map<String, PropertySource<?>> getPropertySources() {
        Map<String, PropertySource<?>> map = new LinkedHashMap<>();
        MutablePropertySources sources;
        if (environment != null) {
            sources = environment.getPropertySources();
        } else {
            sources = new StandardEnvironment().getPropertySources();
        }
        for (PropertySource<?> source : sources) {
            extract("", map, source);
        }
        return map;
    }

    private void extract(String root, Map<String, PropertySource<?>> map, PropertySource<?> source) {
        if (source instanceof CompositePropertySource) {
            for (PropertySource<?> nest : ((CompositePropertySource) source).getPropertySources()) {
                extract(source.getName() + ":", map, nest);
            }
        } else {
            map.put(root + source.getName(), source);
        }
    }

    private Object sanitize(String name, Object object) {
        if(name.toLowerCase().matches(maskedProperties)) {
            object="*****";
        }
        return object;
    }

    private String toJson(Object o, boolean pretty) {
        try {
            if(!pretty) {
                return om.writeValueAsString(o);
            }
            JsonFactory jsonFactory = new JsonFactory();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonGenerator delegate = jsonFactory.createGenerator(baos, JsonEncoding.UTF8);
            SyntaxHighlighter highlighter = DefaultSyntaxHighlighter
                    .newBuilder()
                    .withField(AnsiSyntaxHighlightIntense.BLUE)
                    .withString(AnsiSyntaxHighlightIntense.YELLOW)
                    .withNumber(AnsiSyntaxHighlight.MAGENTA)
                    .withCurlyBrackets(AnsiSyntaxHighlight.CYAN)
                    .withComma(AnsiSyntaxHighlight.WHITE)
                    .withColon(AnsiSyntaxHighlight.WHITE)
                    .build();
            try (JsonGenerator jsonGenerator = new SyntaxHighlightingJsonGenerator(delegate, highlighter)) {
                jsonGenerator.setCodec(om);
                jsonGenerator.writeObject(o);
                baos.write(AnsiSyntaxHighlight.RESET.getBytes());
                return baos.toString();
            } catch(Exception e) {
                return om.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            }
        } catch (Exception e) {
            log.error("Failed to serialize object", e);
            return "";
        }
    }

    private static final class AnsiSyntaxHighlightIntense {
        public static final String BLACK   = "90";
        public static final String RED     = "91";
        public static final String GREEN   = "92";
        public static final String YELLOW  = "93";
        public static final String BLUE        = "94";
        public static final String PURPLE  = "95";
        public static final String CYAN        = "96";
        public static final String WHITE   = "97";
    }
}
