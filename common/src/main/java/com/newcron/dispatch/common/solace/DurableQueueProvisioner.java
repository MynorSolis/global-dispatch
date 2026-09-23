package com.newcron.dispatch.common.solace;

import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;

/**
 * Crea (si no existe) una cola durable y la suscribe a un tópico, siguiendo
 * el patrón de "Topic to Queue Mapping" de Solace: todo lo publicado a ese
 * tópico queda también copiado en la cola.
 */
public final class DurableQueueProvisioner {

    private DurableQueueProvisioner() {
    }

    public static Queue provisionQueueForTopic(JCSMPSession session, String queueName, String topicName) throws Exception {
        Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);

        EndpointProperties endpointProps = new EndpointProperties();
        endpointProps.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
        endpointProps.setPermission(EndpointProperties.PERMISSION_CONSUME);

        // FLAG_IGNORE_ALREADY_EXISTS hace esta operación segura de repetir en cada arranque.
        session.provision(queue, endpointProps, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);

        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
        session.addSubscription(queue, topic, JCSMPSession.WAIT_FOR_CONFIRM);

        return queue;
    }
}
