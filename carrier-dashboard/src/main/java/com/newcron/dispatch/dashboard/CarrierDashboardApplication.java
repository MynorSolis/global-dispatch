package com.newcron.dispatch.dashboard;

import com.newcron.dispatch.common.model.ShipperOrderRequest;
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

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carrier Dashboard: simula el panel donde los transportistas ven, en tiempo
 * real, las cargas disponibles publicadas por Order Intake, y pueden "tomar"
 * una carga desde la consola.
 */
@SpringBootApplication
public class CarrierDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarrierDashboardApplication.class, args);
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
        @Value("${solace.queue.orders-pending}")
        private String ordersPendingQueue;

        private final Map<String, ShipperOrderRequest> availableLoads = new ConcurrentHashMap<>();

        @Override
        public void run(String... args) throws Exception {
            JCSMPSession session = SolaceSessionFactory.createSession(host, vpn, username, password);
            Queue queue = DurableQueueProvisioner.provisionQueueForTopic(session, ordersPendingQueue, ordersNewTopic);

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
                            ShipperOrderRequest order = JsonMessageCodec.fromJson(textMessage.getText(), ShipperOrderRequest.class);
                            availableLoads.put(order.getShipperOrderId(), order);
                            printLoad(order);
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

            System.out.println("=== Dashboard de Transportistas ===");
            System.out.println("Escuchando cola: " + ordersPendingQueue + " (suscrita a " + ordersNewTopic + ")");
            System.out.println("Esperando cargas disponibles...\n");

            Scanner scanner = new Scanner(System.in);
            String input;
            System.out.print("Escribe el ID de una carga para tomarla, o 'salir' para terminar: ");
            while (!(input = scanner.nextLine()).trim().equalsIgnoreCase("salir")) {
                ShipperOrderRequest taken = availableLoads.remove(input.trim());
                if (taken != null) {
                    System.out.println("Carga #" + taken.getShipperOrderId() + " tomada. ¡Buen viaje!");
                } else {
                    System.out.println("No existe esa carga o ya fue tomada.");
                }
                System.out.print("Escribe el ID de una carga para tomarla, o 'salir' para terminar: ");
            }

            flow.close();
            session.closeSession();
        }

        private void printLoad(ShipperOrderRequest order) {
            String origin = order.getStops().get(0).getCity() + ", " + order.getStops().get(0).getState();
            String destination = order.getStops().get(order.getStops().size() - 1).getCity()
                    + ", " + order.getStops().get(order.getStops().size() - 1).getState();

            System.out.println("[NUEVA CARGA DISPONIBLE] #" + order.getShipperOrderId()
                    + " | " + origin + " -> " + destination
                    + " | Pickup: " + order.getPickupDate()
                    + " | Delivery: " + order.getDeliveryDate()
                    + " | $" + order.getPrice());
        }
    }
}
