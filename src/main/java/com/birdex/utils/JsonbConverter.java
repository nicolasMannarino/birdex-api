package com.birdex.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.util.HashMap;
import java.util.Map;

@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<Map<String, Object>, Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, Object>> TYPE = new TypeReference<>() {};

    /**
     * Convertimos el Map de la entidad a un PGobject que PostgreSQL pueda entender
     * para insert/update en columnas jsonb.
     */
    @Override
    public Object convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) return null;

        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(MAPPER.writeValueAsString(attribute));
            return jsonObject;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error serializing JSONB", e);
        }
    }

    /**
     * Convertimos lo que viene de la base de datos a un Map para la entidad.
     * Maneja PGobject o String según lo que devuelva el driver.
     */
    @Override
    public Map<String, Object> convertToEntityAttribute(Object dbData) {
        if (dbData == null) return new HashMap<>();

        try {
            String json;

            if (dbData instanceof PGobject pg) {
                json = pg.getValue();
            } else if (dbData instanceof String s) {
                json = s;
            } else {
                // fallback para cualquier otro tipo raro
                json = dbData.toString();
            }

            if (json == null || json.isBlank()) return new HashMap<>();
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error deserializing JSONB", e);
        }
    }
}
