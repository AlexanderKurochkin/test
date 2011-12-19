package com.where.ads.domain.converter;

import com.where.ads.domain.Publisher;
import com.where.ads.domain.SpotlightRequest;
import com.where.commons.domain.geo.Point;
import com.where.commons.feed.citysearch.ads.CitysearchKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Converts plane {@link Map} to {@link SpotlightRequest}. Map is needed for
 * {@link org.springframework.amqp.support.converter.JsonMessageConverter} to passing through RabbitMQ queue.
 * This class depends from {@link SpotlightRequest} class at compile.
 *
 * @author Alexander_Kurochkin
 */
public class MapToSpotlightRequest {

    private static Class clazz = SpotlightRequest.class;

    private static final Logger LOGGER = LoggerFactory.getLogger(MapToSpotlightRequest.class);

    private Class[] typesForSimpleReflect =
            {String.class,
                    Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Character.class, Boolean.class,
                    List.class, Map.class,
            };
    private Class[] typesToIgnore = {HttpServletRequest.class};

    public SpotlightRequest convertMapToSpotlightRequest(Map<String, Object> map) {
        SpotlightRequest spotlightRequest = new SpotlightRequest();
        fillSpotlightRequest(map, spotlightRequest);
        return spotlightRequest;
    }

    private void fillSpotlightRequest(Map<String, Object> map, SpotlightRequest spotlightRequest) {
        fillNonTrivialObjectInSpotlightRequest(map, spotlightRequest);
        fillTrivialObjectInSpotlightRequest(map, spotlightRequest);
    }

    private void fillTrivialObjectInSpotlightRequest(Map<String, Object> map, SpotlightRequest spotlightRequest) {

        String[] keyArray = new String[map.size()];
        keyArray = map.keySet().toArray(keyArray);
        for (String mapKey : keyArray) {
            try {
                Field field = clazz.getDeclaredField(mapKey);
                field.setAccessible(true);
                if (isAppropriate(field)) {
                    if (field.getType().equals(float.class)) {
                        field.set(spotlightRequest, ((Double) map.remove(field.getName())).floatValue());
                    } else {
                        field.set(spotlightRequest, map.remove(field.getName()));
                    }
                }
            } catch (NoSuchFieldException e) {
                LOGGER.error("MapToSpotlightRequest - property not found: ", e);
            } catch (IllegalAccessException e) {
                LOGGER.error("MapToSpotlightRequest field.set property error", e);
            }
        }
    }

    private boolean isAppropriate(Field field) {
        return !Modifier.isFinal(field.getModifiers()) &&
                !Modifier.isStatic(field.getModifiers()) &&
                !isTypesToIgnore(field.getType()) &&
                !isTypesNonForSimpleReflect(field.getType());
    }

    @SuppressWarnings("unchecked")
    private void fillNonTrivialObjectInSpotlightRequest(Map<String, Object> map, SpotlightRequest spotlightRequest) {


        fillPoint(map, spotlightRequest);
        fillPublisher(map, spotlightRequest);
        fillCitysearchKeyword(map, spotlightRequest);

    }

    private void fillCitysearchKeyword(Map<String, Object> map, SpotlightRequest spotlightRequest) {
        try {
            Field field = clazz.getDeclaredField("keywords");
            field.setAccessible(true);
            if (field.getType().equals(CitysearchKeyword.class)) {
                List<String> keywords = (List<String>) map.remove("keywords");
                CitysearchKeyword citysearchKeyword = new CitysearchKeyword();
                if (map.containsKey("count")) {
                    int count = (Integer) map.remove("count");
                    citysearchKeyword.setCount(count);
                }
                citysearchKeyword.setKeywords(keywords);
                field.set(spotlightRequest, citysearchKeyword);
            }
        } catch (NoSuchFieldException e) {
            LOGGER.error("MapToSpotlightRequest - property not found: ", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("MapToSpotlightRequest field.set property error", e);
        }
    }

    private void fillPublisher(Map<String, Object> map, SpotlightRequest spotlightRequest) {
        try {
            Field field = clazz.getDeclaredField("publisher");
            field.setAccessible(true);
            if (field.getType().equals(Publisher.class)) {
                String name = (String) map.remove("name");
                String categoryForPublisher = (String) map.remove("categoryForPublisher");
                String categoryForPlacement = (String) map.remove("categoryForPlacement");
                Publisher publisher = new Publisher(name);
                publisher.setCategoryForPlacement(categoryForPlacement);
                publisher.setCategoryForPublisher(categoryForPublisher);
                field.set(spotlightRequest, publisher);
            }
        } catch (NoSuchFieldException e) {
            LOGGER.error("MapToSpotlightRequest - property not found: ", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("MapToSpotlightRequest field.set property error", e);
        }
    }

    private void fillPoint(Map<String, Object> map, SpotlightRequest spotlightRequest) {
        try {
            Field field = clazz.getDeclaredField("point");
            field.setAccessible(true);
            if (field.getType().equals(Point.class)) {
                Double lng = (Double) map.remove("lng");
                Double lat = (Double) map.remove("lat");
                if (lng != null && lat != null)
                    field.set(spotlightRequest, new Point(lng, lat));
            }
        } catch (NoSuchFieldException e) {
            LOGGER.error("MapToSpotlightRequest - property not found: ", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("MapToSpotlightRequest field.set property error", e);
        }
    }


    private boolean isTypesNonForSimpleReflect(Class type) {
        if (type.isEnum()) return false;
        if (type.isPrimitive()) return false;
        for (Class c : getTypesForSimpleReflect()) {
            if (c.equals(type)) return false;
        }
        return true;
    }

    private boolean isTypesToIgnore(Class type) {
        for (Class c : getTypesToIgnore()) {
            if (c.equals(type)) return true;
        }
        return false;
    }

    public Class[] getTypesForSimpleReflect() {
        return typesForSimpleReflect;
    }

    public void setTypesForSimpleReflect(Class[] typesForSimpleReflect) {
        this.typesForSimpleReflect = typesForSimpleReflect;
    }

    public Class[] getTypesToIgnore() {
        return typesToIgnore;
    }

    public void setTypesToIgnore(Class[] typesToIgnore) {
        this.typesToIgnore = typesToIgnore;
    }


}
