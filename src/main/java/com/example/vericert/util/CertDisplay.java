package com.example.vericert.util;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.X509Certificate;

public class CertDisplay {

    public static String commonName(X509Certificate cert) {
        try {
            LdapName ln = new LdapName(cert.getSubjectX500Principal().getName());
            for (Rdn rdn : ln.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) return String.valueOf(rdn.getValue());
            }
            return cert.getSubjectX500Principal().getName();
        } catch (Exception e) {
            return cert.getSubjectX500Principal().getName();
        }
    }
}
