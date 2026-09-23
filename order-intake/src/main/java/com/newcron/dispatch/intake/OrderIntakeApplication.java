package com.newcron.dispatch.intake;

import com.newcron.dispatch.common.model.OrderResult;
import com.newcron.dispatch.common.model.ShipperOrderRequest;
import com.newcron.dispatch.common.solace.JsonMessageCodec;
import com.newcron.dispatch.common.solace.SolaceSessionFactory;
import com.newcron.dispatch.common.validation.DateValidator;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Order Intake: recibe el payload de una solicitud de despacho (como argumento
 * de línea de comandos, ruta a un archivo .json), aplica las reglas de negocio
 * de NewCron y publica el resultado en Solace:
 *
 *  - Si es válido:  publica el pedido completo en el tópico de pedidos nuevos
 *                    (para que los transportistas lo vean) y publica un
 *                    resultado "Accepted" en el tópico de resultados.
 *  - Si es inválido: publica directamente un resultado "Cancelled" con el motivo
 *                    en el tópico de resultados.
 */
@SpringBootApplication
public class OrderIntakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderIntakeApplication.class, args);
    }

    @Component
    static class Runner implements CommandLineRunner {

        @Value("${solace.host}")
        private String host;
        @Value("${solace.vpn}")
        private String vpn;
        @Value("${solace.username}")
        private String username;
        @Value("${solace.password}")
        private String password;
        @Value("${solace.topic.orders-new}")
        private String ordersNewTopic;
        @Value("${solace.topic.orders-result}")
        private String ordersResultTopic;

        @Override
        public void run(String... args) throws Exception {
            if (args.length == 0) {
                System.out.println("Uso: java -jar order-intake.jar <ruta-al-payload.json> [<otro-payload.json> ...]");
                System.out.println("Ejemplo: java -jar order-intake.jar ../sample-payloads/valid.json");
                return;
            }

            JCSMPSession session = SolaceSessionFactory.createSession(host, vpn, username, password);

            XMLMessageProducer producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
                @Override
                public void responseReceivedEx(Object key) {
                    System.out.println("Confirmado por el broker: " + key);
                }

                @Override
                public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
                    System.err.println("Error al publicar " + key + ": " + cause.getMessage());
                }
            });

            Topic ordersTopic = JCSMPFactory.onlyInstance().createTopic(ordersNewTopic);
            Topic resultTopic = JCSMPFactory.onlyInstance().createTopic(ordersResultTopic);

            for (String path : args) {
                String json = Files.readString(Path.of(path));
                ShipperOrderRequest request = JsonMessageCodec.fromJson(json, ShipperOrderRequest.class);

                Optional<OrderResult> cancellation = DateValidator.validate(request);

                if (cancellation.isPresent()) {
                    OrderResult result = cancellation.get();
                    producer.send(JsonMessageCodec.toTextMessage(result), resultTopic);
                    System.out.println("Pedido " + request.getShipperOrderId() + " -> CANCELLED: " + result.getNotes());
                } else {
                    producer.send(JsonMessageCodec.toTextMessage(request), ordersTopic);

                    OrderResult accepted = new OrderResult(
                            request.getShipperOrderId(),
                            "Accepted",
                            "You will receive an email when a carrier accepts this dispatch request"
                    );
                    producer.send(JsonMessageCodec.toTextMessage(accepted), resultTopic);

                    System.out.println("Pedido " + request.getShipperOrderId() + " -> ACCEPTED y publicado en " + ordersNewTopic);
                }
            }

            // Da tiempo a que lleguen las confirmaciones de publicación antes de cerrar.
            Thread.sleep(500);
            session.closeSession();
        }
    }
}
