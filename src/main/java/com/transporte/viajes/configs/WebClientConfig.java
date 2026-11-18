package com.transporte.viajes.configs;

import com.transporte.viajes.clients.*;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() { return WebClient.builder(); }

    @Bean
    public ParaderosClient existsParaderos(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl("http://PARADEROS").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient(ParaderosClient.class);
    }

    @Bean
    public GetParaderoClient getParadero(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl("http://PARADEROS").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient(GetParaderoClient.class);
    }

    @Bean
    public UsuarioClient getUser(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl("http://USUARIOS").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient(UsuarioClient.class);
    }

    @Bean
    public ClienteClient getClient(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl("http://CLIENTES").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient(ClienteClient.class);
    }

    @Bean
    public ConductorClient getConductor(WebClient.Builder builder) {
        WebClient webClient = builder
                .baseUrl("http://CONDUCTORES").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
        return factory.createClient(ConductorClient.class);
    }
}
