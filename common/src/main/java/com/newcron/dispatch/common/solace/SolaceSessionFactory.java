package com.newcron.dispatch.common.solace;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;

/** Crea y conecta una sesión JCSMP a partir de las credenciales del broker Solace. */
public final class SolaceSessionFactory {

    private SolaceSessionFactory() {
    }

    public static JCSMPSession createSession(String host, String vpn, String username, String password) throws Exception {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        properties.setProperty(JCSMPProperties.PASSWORD, password);

        JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        return session;
    }
}
