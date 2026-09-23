package com.newcron.dispatch.common.solace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;

/** Convierte objetos Java <-> TextMessage JSON para publicar/leer en Solace. */
public final class JsonMessageCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonMessageCodec() {
    }

    public static <T> TextMessage toTextMessage(T payload) throws Exception {
        TextMessage message = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        message.setText(MAPPER.writeValueAsString(payload));
        // PERSISTENT es obligatorio para que el mensaje publicado a un tópico
        // quede también copiado en las colas durables suscritas a ese tópico.
        message.setDeliveryMode(DeliveryMode.PERSISTENT);
        return message;
    }

    public static <T> T fromJson(String json, Class<T> type) throws Exception {
        return MAPPER.readValue(json, type);
    }
}
