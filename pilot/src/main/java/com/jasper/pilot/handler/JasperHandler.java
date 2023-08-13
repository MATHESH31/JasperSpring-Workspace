package com.jasper.pilot.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.jasper.pilot.projection.UserData;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class JasperHandler {

	@Autowired
	ReactiveMongoTemplate reactiveMongoTemplate;

	public Mono<ServerResponse> data(ServerRequest request) {
		Flux<UserData> user = reactiveMongoTemplate.findAll(UserData.class);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(user, UserData.class);
	}

	public Mono<ServerResponse> generateReport(ServerRequest request) {
		Flux<UserData> users = reactiveMongoTemplate.findAll(UserData.class);

		int fromYear = Integer.parseInt(request.queryParam("fromYear").orElse(""));
		int toYear = Integer.parseInt(request.queryParam("toYear").orElse(""));

		return users.collectList().flatMap(user -> {
			try {
				byte[] pdfBytes = JasperReportPdfGenerator.generatePdfFromTemplate(user, fromYear, toYear);

				Path pdfPath = Paths.get("/generatedReports/UserDetails.pdf");
				Files.write(pdfPath, pdfBytes);

				return ServerResponse.ok().contentType(MediaType.APPLICATION_PDF)
						.body(BodyInserters.fromValue(pdfBytes));
			} catch (JRException jre) {
				jre.printStackTrace();
				return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			} catch (IOException e) {
				return ServerResponse.status(HttpStatus.CONFLICT).build();
			}
		});
	}

	public class JasperReportPdfGenerator {
		public static byte[] generatePdfFromTemplate(List<UserData> userDetails, int fromYear, int toYear)
				throws JRException {
			
			String jrxmlFile = "pilot/src/main/resources/jrxmlTemplates/Sample.jrxml";
			String jasperFile = "pilot/src/main/resources/jasperTemplate/Sample.jasper";
			
			try {
				compileAndSave(jrxmlFile, jasperFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			InputStream templateStream = JasperReportPdfGenerator.class.getResourceAsStream("/jasperTemplate/Sample.jasper");

			Map<String, Object> parameters = new HashMap<>();
			parameters.put("userDetails", userDetails);
			parameters.put("fromYear", fromYear);
			parameters.put("toYear", toYear);

			JasperPrint jasperPrint = JasperFillManager.fillReport(templateStream, parameters, new JREmptyDataSource());

			return JasperExportManager.exportReportToPdf(jasperPrint);
		}
	}

	public static void compileAndSave(String jrxmlFilePath, String jasperOutputPath) throws Exception {
		JasperCompileManager.compileReportToFile(jrxmlFilePath, jasperOutputPath);
		
	}
}