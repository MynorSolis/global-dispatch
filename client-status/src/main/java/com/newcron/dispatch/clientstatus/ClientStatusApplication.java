package com.newcron.dispatch.clientstatus;

import com.newcron.dispatch.common.model.OrderResult;
import com.newcron.dispatch.common.solace.DurableQueueProvisioner;
import com.newcron.dispatch.common.solace.JsonMessageCodec;
import com.newcron.dispatch.common.solace.SolaceSessionFactory;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

/**
 * Client Status: apartado donde el cliente que envió la solicitud de carga
 * ve en tiempo real si fue "Accepted" o "Cancelled" (y el motivo).
 */
@SpringBootApplication
public class ClientStatusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientStatusApplication.class, args);
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
        @Value("${solace.topic.orders-result}")
        private String ordersResultTopic;
        @Value("${solace.queue.orders-results}")
        private String ordersResultsQueue;

        @Override
        public void run(String... args) throws Exception {
            JCSMPSession session = SolaceSessionFactory.createSession(host, vpn, username, password);
            Queue queue = DurableQueueProvisioner.provisionQueueForTopic(session, ordersResultsQueue, ordersResultTopic);

            ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
            flowProps.setEndpoint(queue);
            flowProps.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

            EndpointProperties endpointProps = new EndpointProperties();
            endpointProps.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);

            FlowReceiver flow = session.createFlow(new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage msg) {
                    try {
                        if (msg instanceof TextMessage textMessage) {
                            OrderResult result = JsonMessageCodec.fromJson(textMessage.getText(), OrderResult.class);
                            String tag = "Accepted".equals(result.getStatus()) ? "[ACCEPTED]" : "[CANCELLED]";
                            System.out.println(tag + " Pedido #" + result.getShipperOrderId()
                                    + " -> " + result.getNotes());
                        }
                    } catch (Exception e) {
                        System.err.println("Error procesando mensaje: " + e.getMessage());
                    } finally {
                        msg.ackMessage();
                    }
                }

                @Override
                public void onException(JCSMPException e) {
                    System.err.println("Error en el flow de consumo: " + e.getMessage());
                }
            }, flowProps, endpointProps);

            flow.start();

            System.out.println("=== Estado de Solicitudes del Cliente ===");
            System.out.println("Escuchando cola: " + ordersResultsQueue + " (suscrita a " + ordersResultTopic + ")");
            System.out.println("Esperando actualizaciones... (Ctrl+C para salir)\n");

            Thread.currentThread().join();
        }
    }
}
