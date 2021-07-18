package de.facecheck.ui;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.appreciated.prism.element.Language;
import com.github.appreciated.prism.element.PrismHighlighter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import lombok.Getter;

@Route(value = "imagetest")
@PageTitle("Bildanalyse")
@Getter
public class ImageTestView extends VerticalLayout {

	private static final long serialVersionUID = 6306472220487349319L;

	private final ImageTestPresenter presenter;

	private final H1 title = new H1("Bildanalyse");

	private final TextField urlField = new TextField("Bildaddresse");
	private final Button analyseUrlButton = new Button("Analysieren");
	private final HorizontalLayout urlLayout = new HorizontalLayout(urlField, analyseUrlButton);

	private final Upload imageUpload = new Upload();

	private final HorizontalLayout inputLayout = new HorizontalLayout(urlLayout, imageUpload);

	private final TextField profile = new TextField("Profil");

	private final Label requestLabel = new Label("Anfrage:");
	private PrismHighlighter request = new PrismHighlighter("{}", Language.json);
	private final VerticalLayout requestLayout = new VerticalLayout(requestLabel, request);

	private final Label time = new Label("Dauer:");

	private final Label responseLabel = new Label("Antwort:");
	private PrismHighlighter response = new PrismHighlighter("{}", Language.json);
	private final VerticalLayout responseLayout = new VerticalLayout(responseLabel, response);

	@Autowired
	public ImageTestView(ImageTestPresenter presenter) {
		this.presenter = presenter;

		urlField.setWidth("400px");
		urlLayout.setAlignItems(Alignment.END);

		imageUpload.setReceiver(presenter);
		imageUpload.setAcceptedFileTypes("image/png", "image/jpeg");
		imageUpload.setMaxFileSize(5 * 1024 * 1024);
		imageUpload.setUploadButton(new Button("Bild auswählen"));
		imageUpload.setDropLabel(new Label("oder hierher ziehen"));

		requestLayout.setPadding(false);
		responseLayout.setPadding(false);

		add(title, inputLayout, profile, time, requestLayout, responseLayout);

		presenter.setView(this);
	}

	public void setRequest(String json) {
		PrismHighlighter newRequest = new PrismHighlighter(json, Language.json);
		requestLayout.replace(request, newRequest);
		request = newRequest;
	}

	public void setTime(long time) {
		String text = String.format("Dauer: %dms", time);
		this.time.setText(text);
	}

	public void setResponse(String json) {
		PrismHighlighter newResponse = new PrismHighlighter(json, Language.json);
		responseLayout.replace(response, newResponse);
		response = newResponse;
	}

}
