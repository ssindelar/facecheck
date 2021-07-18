package de.facecheck.ui;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Base64;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;

import de.facecheck.ui.model.FaceCheckRequest;
import de.facecheck.ui.model.FaceCheckResponse;

@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImageTestPresenter implements Receiver {

	private static final long serialVersionUID = -4656162032689158055L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageTestPresenter.class);

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private ImageTestView view;
	private ByteArrayOutputStream uploadStream;


	public void setView(ImageTestView view) {
		this.view = view;
		
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.setSerializationInclusion(Include.NON_NULL);

		Button analyseUrlButton = view.getAnalyseUrlButton();
		analyseUrlButton.setEnabled(false);
		TextField urlField = view.getUrlField();
		urlField.setValueChangeMode(ValueChangeMode.LAZY);
		urlField.addValueChangeListener(event -> analyseUrlButton.setEnabled(!event.getValue().isEmpty()));
		analyseUrlButton.addClickListener(event -> analyseButtonClicked());
		view.getImageUpload().addSucceededListener(event -> uploadSuccessfull());
	}

	private void uploadSuccessfull() {
		LOGGER.info("Upload fertig");
		byte[] imageBytes = uploadStream.toByteArray();
		FaceCheckRequest request = FaceCheckRequest.builder()
				.image(Base64.getEncoder().encodeToString(imageBytes))
				.profileId(view.getProfile().getValue())
				.build();
		request(request);
	}

	private void analyseButtonClicked() {
		LOGGER.info("Button geklickt");
		TextField field = view.getUrlField();
		FaceCheckRequest request = FaceCheckRequest.builder()
				.link(field.getValue())
				.profileId(view.getProfile().getValue())
				.build();
		request(request);
		field.clear();
	}

	private void request(FaceCheckRequest request) {
		try {
			String json = objectMapper.writeValueAsString(request);
			view.setRequest(json);
		} catch (JsonProcessingException e) {
			LOGGER.error("Fehler beim Request JSON erzeugen.");
		}

		StopWatch stopWatch = StopWatch.createStarted();
		ResponseEntity<FaceCheckResponse> responseEntity = restTemplate.postForEntity("https://api.wupu.de/facecheck",
				request, FaceCheckResponse.class);
		FaceCheckResponse response = responseEntity.getBody();
		view.setTime(stopWatch.getTime());

		try {
			String json = objectMapper.writeValueAsString(response);
			view.setResponse(json);
		} catch (JsonProcessingException e) {
			LOGGER.error("Fehler beim Response JSON erzeugen.");
		}
		LOGGER.info("Analyse fertig");
	}


	@Override
	public OutputStream receiveUpload(String fileName, String mimeType) {
		uploadStream = new ByteArrayOutputStream();
		return uploadStream;
	}

}
