package com.moviereview;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.tomakehurst.wiremock.WireMockServer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	WireMockServer tmdbServer() {
		return new WireMockServer(options().dynamicPort());
	}

	@Bean
	DynamicPropertyRegistrar tmdbProperties(WireMockServer tmdbServer) {
		return registry -> registry.add("app.tmdb.base-url", tmdbServer::baseUrl);
	}

}
