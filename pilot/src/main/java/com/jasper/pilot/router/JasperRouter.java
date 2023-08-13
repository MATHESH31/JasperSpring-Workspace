package com.jasper.pilot.router;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.jasper.pilot.handler.JasperHandler;

@Configuration
public class JasperRouter {

	@Autowired
	JasperHandler jasperHandler;

	@Bean
	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route().GET("/data", jasperHandler::data)
				.GET("reportGenerator", jasperHandler::generateReport).build();
	}
}
