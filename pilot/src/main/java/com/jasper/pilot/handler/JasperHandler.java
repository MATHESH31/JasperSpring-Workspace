package com.jasper.pilot.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.jasper.pilot.projection.UserData;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class JasperHandler {

	@Autowired
	ReactiveMongoTemplate reactiveMongoTemplate;

	final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();

	public Mono<ServerResponse> data(ServerRequest request) {
		Flux<UserData> user = reactiveMongoTemplate.findAll(UserData.class);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(user, UserData.class);
	}

	public Mono<ServerResponse> generateReport(ServerRequest request) {
		Flux<UserData> users = reactiveMongoTemplate.findAll(UserData.class);

		int fromYear = Integer.parseInt(request.queryParam("fromYear").orElse(""));
		int toYear = Integer.parseInt(request.queryParam("toYear").orElse(""));

		try {
			InputStream reportInputStream = getClass().getResourceAsStream("/jrxmlTemplates/Sample.jrxml");
			JasperReport jasperReport = JasperCompileManager.compileReport(reportInputStream);

			return users.collectList().flatMap(user -> {
				JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(user);

				Map<String, Object> parameters = new HashMap<>();
				parameters.put("fromYear", fromYear);
				parameters.put("toYear", toYear);

				JasperPrint jasperPrint = null;
				try {
					jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
				} catch (JRException e) {
					e.printStackTrace();
				}

				ByteArrayOutputStream pdfByteArrayOutputStream = new ByteArrayOutputStream();
				try {
					JasperExportManager.exportReportToPdfStream(jasperPrint, pdfByteArrayOutputStream);
				} catch (JRException e) {
					e.printStackTrace();
				}
				Flux<DataBuffer> dataBufferFlux = Flux.just(pdfByteArrayOutputStream.toByteArray())
						.map(bufferFactory::wrap);

				try {
					pdfByteArrayOutputStream.close();
					reportInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				return ServerResponse.ok().contentType(MediaType.APPLICATION_PDF)
						.body(BodyInserters.fromDataBuffers(dataBufferFlux));

			});
		} catch (Exception e) {
			e.printStackTrace();
			return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
