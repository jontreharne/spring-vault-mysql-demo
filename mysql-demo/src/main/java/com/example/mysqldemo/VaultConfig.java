package com.example.mysqldemo;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.domain.RequestedSecret;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.core.lease.event.SecretLeaseExpiredEvent;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@ConditionalOnProperty(value="spring.cloud.vault.enabled", havingValue = "true")
public class VaultConfig {

    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @Autowired
    private HikariDataSource hikariDataSource;
    @Autowired
    private SecretLeaseContainer leaseContainer;

    @PostConstruct
    private void postConstruct(){
        log.info("Adding Lease Listener");
        leaseContainer.addLeaseListener(event -> {
            String vaultCredsPath = "database/creds/my-role";

            if (event.getSource().getPath().equals(vaultCredsPath)){
                if(event instanceof SecretLeaseExpiredEvent && event.getSource().getMode() == RequestedSecret.Mode.RENEW){
                    log.info ( "RENEW Event received. Requesting Rotating Secret" );
                    leaseContainer.requestRotatingSecret(vaultCredsPath);
                } else if (event instanceof SecretLeaseCreatedEvent && event.getSource().getMode() == RequestedSecret.Mode.ROTATE){
                    log.info ( "Secret Rotated. Updating Database credentials." );
                    Credentials credentials = getCredentials((SecretLeaseCreatedEvent) event);
                    refreshDatabaseConnection(credentials);
                }
            }
        });
    }

    private void refreshDatabaseConnection(Credentials credentials) {
        System.setProperty("spring.datasource.username", credentials.username);
        System.setProperty("spring.datasource.password", credentials.password);
        hikariDataSource.getHikariConfigMXBean().setUsername(credentials.username);
        hikariDataSource.getHikariConfigMXBean().setPassword(credentials.password);
        hikariDataSource.getHikariPoolMXBean().softEvictConnections();
    }

    private Credentials getCredentials(SecretLeaseCreatedEvent event){
        Credentials credentials = new Credentials();
        credentials.username = event.getSecrets().get("username").toString();
        credentials.password = event.getSecrets().get("password").toString();
        return credentials;
    }

    @Data
    private class Credentials{
        String username;
        String password;
    }
}
