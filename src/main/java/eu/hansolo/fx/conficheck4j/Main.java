package eu.hansolo.fx.conficheck4j;

import eu.hansolo.fx.conficheck4j.data.ConferenceItem;
import eu.hansolo.fx.conficheck4j.data.ConfiModel;
import eu.hansolo.fx.conficheck4j.data.SpeakerItem;
import eu.hansolo.fx.conficheck4j.fonts.Fonts;
import eu.hansolo.fx.conficheck4j.tools.Constants;
import eu.hansolo.fx.conficheck4j.tools.Constants.AttendingStatus;
import eu.hansolo.fx.conficheck4j.tools.Constants.Continent;
import eu.hansolo.fx.conficheck4j.tools.Constants.Filter;
import eu.hansolo.fx.conficheck4j.tools.Factory;
import eu.hansolo.fx.conficheck4j.tools.Helper;
import eu.hansolo.fx.conficheck4j.tools.IsoCountries;
import eu.hansolo.fx.conficheck4j.tools.PersistentToggleGroup;
import eu.hansolo.fx.conficheck4j.tools.PropertyManager;
import eu.hansolo.fx.conficheck4j.views.CalendarView;
import eu.hansolo.fx.conficheck4j.views.ConferenceView;
import eu.hansolo.jdktools.versioning.VersionNumber;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static eu.hansolo.toolbox.Constants.NEW_LINE;


public class Main extends Application {
    public static final VersionNumber             VERSION               = PropertyManager.INSTANCE.getVersionNumber();
    private             ConfiModel                model;
    private             Popup                     searchResultPopup;
    private             ComboBox<String>          continentsComboBox;
    private             PersistentToggleGroup     filterToggleGroup;
    private             ToggleButton              allToggleButton       = Factory.createToggleButton(Filter.ALL.getName(), Constants.STD_FONT_SIZE);
    private             ToggleButton              speakingToggleButton  = Factory.createToggleButton(Filter.SPEAKING.getName(), Constants.STD_FONT_SIZE);
    private             ToggleButton              attendingToggleButton = Factory.createToggleButton(Filter.ATTENDING.getName(), Constants.STD_FONT_SIZE);
    private             ToggleButton              cfpOpenToggleButton   = Factory.createToggleButton(Filter.CFP_OPEN.getName(), Constants.STD_FONT_SIZE);
    private             VBox                      conferencesVBox;
    private             CalendarView              calendarView;
    private             VBox                      vBox;
    private             StackPane                 pane;
    private             Stage                     stage;
    private             ObjectProperty<Continent> selectedContinent;
    private             ObjectProperty<Filter>    selectedFilter;
    private             BooleanProperty           speakerInfoVisible;
    private             Clipboard                 clipboard;
    private             ClipboardContent          clipboardContent;


    @Override public void init() {
        this.model = new ConfiModel();

        // Continents
        Text continentText = Factory.createText("Continent", Color.BLACK, Fonts.avenirNextLtProRegular(Constants.STD_FONT_SIZE));

        continentsComboBox = new ComboBox<>();
        for(Continent continent : Continent.values()) {
            continentsComboBox.getItems().add(continent.name);
        }
        continentsComboBox.getSelectionModel().select(0);
        continentsComboBox.getItems().forEach(continent -> continentText.setFont(Fonts.avenirNextLtProRegular(Constants.STD_FONT_SIZE)));
        HBox continentBox = new HBox(5, continentText, continentsComboBox);
        continentBox.setAlignment(Pos.BASELINE_LEFT);

        // Filters
        allToggleButton.getStyleClass().add("left-pill");
        speakingToggleButton.getStyleClass().add("center-pill");
        attendingToggleButton.getStyleClass().add("center-pill");
        cfpOpenToggleButton.getStyleClass().add("right-pill");

        filterToggleGroup = new PersistentToggleGroup();
        filterToggleGroup.getToggles().add(allToggleButton);
        filterToggleGroup.getToggles().add(speakingToggleButton);
        filterToggleGroup.getToggles().add(attendingToggleButton);
        filterToggleGroup.getToggles().add(cfpOpenToggleButton);

        allToggleButton.setSelected(true);
        HBox filterButtons = new HBox(0, Factory.createSpacer(Orientation.HORIZONTAL), allToggleButton, speakingToggleButton, attendingToggleButton, cfpOpenToggleButton, Factory.createSpacer(Orientation.HORIZONTAL));

        // Conferences
        conferencesVBox = new VBox(0);
        conferencesVBox.setAlignment(Pos.CENTER);
        conferencesVBox.setFillWidth(true);
        ScrollPane scrollPane = new ScrollPane(conferencesVBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setMinWidth(ConferenceView.MINIMUM_WIDTH + 40);
        scrollPane.setMinHeight(325);
        scrollPane.setFitToWidth(true);

        calendarView = new CalendarView(Main.this, this.model);
        calendarView.setMinWidth(ConferenceView.MINIMUM_WIDTH + 40);
        calendarView.setMinHeight(120);

        Button speakerInfoButton = Factory.createButton("Speaker Info", "Show speaker info", Constants.STD_FONT_SIZE);
        Button exportButton      = Factory.createButton("Export", "Export conferences visited", Constants.STD_FONT_SIZE);
        Button proposalsButton   = Factory.createButton("Proposals", "Show proposals", Constants.STD_FONT_SIZE);

        speakerInfoButton.setOnAction(e -> this.speakerInfoVisible.set(true));

        HBox buttonBox = new HBox(5, speakerInfoButton, Factory.createSpacer(Orientation.HORIZONTAL), exportButton, Factory.createSpacer(Orientation.HORIZONTAL), proposalsButton);

        vBox = new VBox(10, continentBox, filterButtons, scrollPane, calendarView, buttonBox);
        vBox.setMinWidth(ConferenceView.MINIMUM_WIDTH + 40);

        this.pane = new StackPane(vBox);
        pane.getStyleClass().add("confi-check");
        //pane.setStyle("-fx-base: " + (eu.hansolo.fx.conficheck4j.tools.Constants.IS_DARK_MODE ? "#202020" : "#ececec"));
        pane.setPadding(new Insets(10, 10, 10, 10));
        pane.setMinSize(600, 720);

        this.selectedContinent  = new ObjectPropertyBase<>(Continent.ALL) {
            @Override protected void invalidated() { updateView(); }
            @Override public Object getBean() { return Main.this; }
            @Override public String getName() { return "selectedContinent"; }
        };
        this.selectedFilter     = new ObjectPropertyBase<>(Filter.ALL) {
            @Override protected void invalidated() { updateView(); }
            @Override public Object getBean() { return Main.this; }
            @Override public String getName() { return "selectedFilter"; }
        };
        this.speakerInfoVisible = new BooleanPropertyBase(Boolean.FALSE) {
            @Override protected void invalidated() { if (get()) { openSpeakerInfo(); } }
            @Override public Object getBean() { return Main.this; }
            @Override public String getName() { return "speakerInfoVisible"; }
        };

        registerListeners();
    }

    private void registerListeners() {
        this.model.update.addListener(o -> updateView());
        this.continentsComboBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> this.selectedContinent.set(Constants.Continent.fromText(nv)));
        this.allToggleButton.selectedProperty().addListener((o, ov, nv) -> this.selectedFilter.set(Filter.ALL));
        this.speakingToggleButton.selectedProperty().addListener((o, ov, nv) -> this.selectedFilter.set(Filter.SPEAKING));
        this.attendingToggleButton.selectedProperty().addListener((o, ov, nv) -> this.selectedFilter.set(Filter.ATTENDING));
        this.cfpOpenToggleButton.selectedProperty().addListener((o, ov, nv) -> this.selectedFilter.set(Filter.CFP_OPEN));

    }

    private void initOnFXApplicationThread() {
        searchResultPopup   = new Popup();
        searchResultPopup.getScene().getStylesheets().add(Main.class.getResource("conficheck4j.css").toExternalForm());

        this.clipboard        = Clipboard.getSystemClipboard();
        this.clipboardContent = new ClipboardContent();
    }

    @Override public void start(final Stage stage) {
        this.stage = stage;
        initOnFXApplicationThread();

        Scene scene = new Scene(pane, 500, 570);
        scene.getStylesheets().add(Main.class.getResource("conficheck4j.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("ConfiCheck " + Main.VERSION);
        stage.setMinWidth(ConferenceView.MINIMUM_WIDTH + 60);
        stage.setMinHeight(600);
        stage.show();
        stage.centerOnScreen();

        fetchConferences();
        calendarView.setToInitialPosition();
        updateView();
    }

    @Override public void stop() {
        Platform.exit();
        System.exit(0);
    }

    private void updateView() {
        Platform.runLater(() -> {
            List<String>         countriesInContinent   = Continent.ALL == this.selectedContinent.get() ? IsoCountries.ALL_COUNTRIES.stream().map(isoCountryInfo -> isoCountryInfo.name()).toList() : IsoCountries.ALL_COUNTRIES.stream().filter(country -> country.continent().equals(selectedContinent.get().code)).map(isoCountryInfo -> isoCountryInfo.name()).toList();
            List<ConferenceItem> conferencesInContinent = this.model.conferences.stream().filter(conference -> countriesInContinent.contains(conference.getCountry())).toList();
            this.model.conferencesPerMonth.clear();
            this.model.conferencesPerContinent.clear();
            conferencesInContinent.forEach(conference -> {
                final ZonedDateTime date  = ZonedDateTime.ofInstant(conference.getDate(), ZoneId.systemDefault());
                final Integer       month = date.get(ChronoField.MONTH_OF_YEAR);
                if (!this.model.conferencesPerMonth.containsKey(month)) {
                    this.model.conferencesPerMonth.put(month, new TreeSet<>());
                    this.model.conferencesPerContinent.put(month, new TreeSet<>());
                }
                this.model.conferencesPerMonth.get(month).add(conference);
                this.model.conferencesPerContinent.get(month).add(conference);
            });
            switch (this.selectedFilter.get()) {
                case ALL       -> {
                    this.model.filteredConferences.clear();
                    this.model.filteredConferences.putAll(this.model.conferencesPerContinent);
                }
                case SPEAKING  -> {
                    this.model.filteredConferences.clear();
                    conferencesInContinent.forEach(conference -> {
                        final ZonedDateTime date  = ZonedDateTime.ofInstant(conference.getDate(), ZoneId.systemDefault());
                        final Integer       month = date.get(ChronoField.MONTH_OF_YEAR);
                        if (this.model.attendence.containsKey(conference.getId())) {
                            if (this.model.attendence.get(conference.getId()) != AttendingStatus.SPEAKING.id) { return; }
                            if (!this.model.filteredConferences.containsKey(month)) { this.model.filteredConferences.put(month, new TreeSet<>()); }
                            this.model.filteredConferences.get(month).add(conference);
                        }
                    });
                }
                case ATTENDING -> {
                    this.model.filteredConferences.clear();
                    conferencesInContinent.forEach(conference -> {
                        final ZonedDateTime date  = ZonedDateTime.ofInstant(conference.getDate(), ZoneId.systemDefault());
                        final Integer       month = date.get(ChronoField.MONTH_OF_YEAR);
                        if (this.model.attendence.containsKey(conference.getId())) {
                            if (this.model.attendence.get(conference.getId()) != AttendingStatus.ATTENDING.id) { return; }
                            if (!this.model.filteredConferences.containsKey(month)) { this.model.filteredConferences.put(month, new TreeSet<>()); }
                            this.model.filteredConferences.get(month).add(conference);
                        }
                    });
                }
                case CFP_OPEN  -> {
                    for (Integer month : this.model.conferencesPerContinent.keySet()) {
                        if (this.model.conferencesPerContinent.get(month).isEmpty()) { continue; }
                        this.model.filteredConferences.put(month, new TreeSet<>(this.model.conferencesPerContinent.get(month)
                                                                                                                  .stream()
                                                                                                                  .filter(conference -> conference.getCfpDate().isPresent())
                                                                                                                  .filter(conference -> Helper.getDatesFromJavaConferenceDate(conference.getCfpDate().get()).length > 0 && Helper.getDatesFromJavaConferenceDate(conference.getCfpDate().get())[0].isPresent())
                                                                                                                  .filter(conference -> Helper.isCfpOpen(ZonedDateTime.ofInstant(Helper.getDatesFromJavaConferenceDate(conference.getCfpDate().get())[0].get(), ZoneId.systemDefault()).toLocalDate()))
                                                                                                                  .collect(Collectors.toSet())));
                    }
                }
            }

            final int currentMonth = LocalDate.now().getMonthValue();
            List<TitledPane> filtered  = new ArrayList<>();
            this.model.filteredConferences.entrySet().forEach(entry -> {
                final Integer                 month              = entry.getKey();
                final TreeSet<ConferenceItem> conferencesInMonth = entry.getValue();
                VBox monthBox = new VBox();
                conferencesInMonth.forEach(conference -> monthBox.getChildren().add(new ConferenceView(Main.this, this.model, conference)));
                TitledPane monthPane = new TitledPane(Constants.MONTHS[month - 1], monthBox);
                monthPane.setCollapsible(true);
                switch (this.selectedFilter.get()) {
                    case ALL                            -> monthPane.setExpanded(month == currentMonth);
                    case SPEAKING, ATTENDING, CFP_OPEN  -> monthPane.setExpanded(!this.model.filteredConferences.get(month).isEmpty());
                }
                filtered.add(monthPane);
            });
            conferencesVBox.getChildren().setAll(filtered);
        });
    }

    private void fetchConferences() {
        try {
            //String               jsonText        = Helper.readTextFile("/Users/hansolo/Desktop/javaconferences.json", Charset.defaultCharset());
            //String               jsonText        = Helper.getTextFromUrl(Constants.JAVA_CONFERENCES_JSON_URL);
            //List<JavaConference> conferences     = Helper.parseJavaConferencesJson(jsonText);
            //this.model.update(conferences);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSpeakerInfo() {
        SpeakerItem speakerItem      = Helper.loadSpeakerItem();
        Stage       speakerInfoStage = new Stage();
        speakerInfoStage.setTitle("JVM Inventory Report Viewer");

        // Copied Feedback pane
        Region checkmarkIcon = new Region();
        checkmarkIcon.getStyleClass().add("checkmark-icon");
        checkmarkIcon.setFocusTraversable(false);
        checkmarkIcon.setPrefSize(64, 64);
        checkmarkIcon.setMinSize(64, 64);
        checkmarkIcon.setMaxSize(64, 64);

        Rectangle checkmarkIconRect = new Rectangle(128, 128);
        checkmarkIconRect.setArcWidth(30);
        checkmarkIconRect.setArcHeight(30);
        checkmarkIconRect.setFill(Color.color(0.0, 0.0, 0.0, 0.5));

        Text copiedText = new Text("Copied");
        copiedText.setFont(Fonts.avenirNextLtProRegular(Constants.STD_FONT_SIZE));
        copiedText.setFill(Color.WHITE);
        copiedText.setTranslateY(50);

        StackPane copiedFeedbackPane = new StackPane(checkmarkIconRect, checkmarkIcon, copiedText);
        copiedFeedbackPane.setMouseTransparent(true);
        copiedFeedbackPane.setOpacity(0.0);

        // Speaker Image
        Optional<Image> speakerImage = Helper.loadSpeakerImage();

        ImageView speakerImageView = speakerImage.isPresent() ? new ImageView(speakerImage.get()) : new ImageView();
        speakerImageView.setFitHeight(100);
        speakerImageView.setFitWidth(100);
        speakerImageView.setClip(new Circle(50, 50, 50));

        Circle backgroundCircle = new Circle(50, 50, 50);
        backgroundCircle.setFill(Color.TRANSPARENT);
        backgroundCircle.setStroke(Color.BLACK);
        backgroundCircle.setStrokeWidth(5);
        backgroundCircle.setStrokeType(StrokeType.OUTSIDE);

        StackPane speakerImagePane = new StackPane(backgroundCircle);
        speakerImagePane.getChildren().add(speakerImageView);
        if (speakerImage.isEmpty()) {
            speakerImagePane.setBackground(new Background(new BackgroundFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.GRAY), new Stop(1, Color.DARKGRAY)), new CornerRadii(48), Insets.EMPTY)));
        }

        Circle circle = new Circle(12, 12, 12);
        circle.setFill(Color.web("#3478f6"));
        Region photoIcon = new Region();
        photoIcon.getStyleClass().add("photo-icon");
        photoIcon.setFocusTraversable(false);
        photoIcon.setPrefSize(16, 16);
        photoIcon.setMinSize(16, 16);
        photoIcon.setMaxSize(16, 16);
        Tooltip.install(photoIcon, new Tooltip("Select a speaker photo"));
        StackPane photoIconPane = new StackPane(circle, photoIcon);
        photoIconPane.setTranslateX(35.75);
        photoIconPane.setTranslateY(35.75);

        photoIconPane.setOnMousePressed(e -> {
            Optional<Image> newSpeakerImage = Helper.selectImage(this.stage);
            if (newSpeakerImage.isPresent()) {
                speakerImagePane.setBackground(Background.EMPTY);
                speakerImageView.setImage(newSpeakerImage.get());
            }
        });

        speakerImagePane.getChildren().add(photoIconPane);
        speakerImagePane.setPrefSize(100, 100);

        Region copyImageIcon = new Region();
        copyImageIcon.getStyleClass().add("copy-icon");
        copyImageIcon.setFocusTraversable(false);
        copyImageIcon.setPrefSize(16, 16);
        copyImageIcon.setMinSize(16, 16);
        copyImageIcon.setMaxSize(16, 16);

        Button copySpeakerImageButton = Factory.createButton("Copy image", "Copy speaker image to clipboard", Constants.STD_FONT_SIZE);
        copySpeakerImageButton.setGraphic(copyImageIcon);
        copySpeakerImageButton.setOnAction(e -> {
            clipboard.clear();
            clipboardContent.clear();
            clipboardContent.putImage(speakerImageView.getImage());
            clipboard.setContent(clipboardContent);
            copiedFeedbackPane.setOpacity(1.0);
            fadeOutPane(copiedFeedbackPane);
        });

        // Name
        Region copyNameIcon = new Region();
        copyNameIcon.getStyleClass().add("copy-icon");
        copyNameIcon.setFocusTraversable(false);
        copyNameIcon.setPrefSize(16, 16);
        copyNameIcon.setMinSize(16, 16);
        copyNameIcon.setMaxSize(16, 16);
        Tooltip.install(copyNameIcon, new Tooltip("Copy speaker name to clipboard"));

        Label     speakerNameLabel     = Factory.createLabel("Name", Constants.GRAY, Fonts.avenirNextLtProDemi(Constants.STD_FONT_SIZE), Pos.CENTER_RIGHT);
        TextField speakerNameTextField = Factory.createTextField("Your name", "Speaker name", Constants.STD_FONT_SIZE);
        HBox      speakerNameHBox      = new HBox(speakerNameLabel, Factory.createSpacer(Orientation.HORIZONTAL), copyNameIcon);
        VBox      speakerNameBox       = new VBox(speakerNameHBox, speakerNameTextField);
        speakerNameTextField.setText(speakerItem.getName());
        copyNameIcon.setOnMousePressed(e -> {
            clipboard.clear();
            clipboardContent.clear();
            clipboardContent.putString(speakerNameTextField.getText());
            clipboard.setContent(clipboardContent);
            copiedFeedbackPane.setOpacity(1.0);
            fadeOutPane(copiedFeedbackPane);
        });

        // BlueSky
        Region copyBlueSkyIcon = new Region();
        copyBlueSkyIcon.getStyleClass().add("copy-icon");
        copyBlueSkyIcon.setFocusTraversable(false);
        copyBlueSkyIcon.setPrefSize(16, 16);
        copyBlueSkyIcon.setMinSize(16, 16);
        copyBlueSkyIcon.setMaxSize(16, 16);
        Tooltip.install(copyBlueSkyIcon, new Tooltip("Copy speaker bluesky link to clipboard"));

        Label     blueSkyLabel     = Factory.createLabel("BlueSky", Constants.GRAY, Fonts.avenirNextLtProDemi(Constants.STD_FONT_SIZE), Pos.CENTER_RIGHT);
        TextField blueSkyTextField = Factory.createTextField("Your BlueSky account", "Speaker bluesky name", Constants.STD_FONT_SIZE);
        HBox      blueSkyHBox      = new HBox(blueSkyLabel, Factory.createSpacer(Orientation.HORIZONTAL), copyBlueSkyIcon);
        VBox      blueSkyBox       = new VBox(blueSkyHBox, blueSkyTextField);
        blueSkyTextField.setText(speakerItem.getBluesky());
        copyBlueSkyIcon.setOnMousePressed(e -> {
            clipboard.clear();
            clipboardContent.clear();
            clipboardContent.putString("https://bsky.app/profile/" + blueSkyTextField.getText());
            clipboard.setContent(clipboardContent);
            copiedFeedbackPane.setOpacity(1.0);
            fadeOutPane(copiedFeedbackPane);
        });

        // Bio
        Region copyBioIcon = new Region();
        copyBioIcon.getStyleClass().add("copy-icon");
        copyBioIcon.setFocusTraversable(false);
        copyBioIcon.setPrefSize(16, 16);
        copyBioIcon.setMinSize(16, 16);
        copyBioIcon.setMaxSize(16, 16);
        Tooltip.install(copyBioIcon, new Tooltip("Copy speaker bio to clipboard"));

        Label    bioLabel     = Factory.createLabel("Bio", Constants.GRAY, Fonts.avenirNextLtProDemi(Constants.STD_FONT_SIZE), Pos.CENTER_RIGHT);
        TextArea bioTextArea = Factory.createRegularTextArea("Your bio", Constants.BLACK, Constants.STD_FONT_SIZE);
        HBox     bioHBox      = new HBox(bioLabel, Factory.createSpacer(Orientation.HORIZONTAL), copyBioIcon);
        VBox     bioBox       = new VBox(bioHBox, bioTextArea);
        bioTextArea.setText(speakerItem.getBio());
        copyBioIcon.setOnMousePressed(e -> {
            clipboard.clear();
            clipboardContent.clear();
            clipboardContent.putString(bioTextArea.getText());
            clipboard.setContent(clipboardContent);
            copiedFeedbackPane.setOpacity(1.0);
            fadeOutPane(copiedFeedbackPane);
        });

        // Experience
        Region copyExperienceIcon = new Region();
        copyExperienceIcon.getStyleClass().add("copy-icon");
        copyExperienceIcon.setFocusTraversable(false);
        copyExperienceIcon.setPrefSize(16, 16);
        copyExperienceIcon.setMinSize(16, 16);
        copyExperienceIcon.setMaxSize(16, 16);
        Tooltip.install(copyExperienceIcon, new Tooltip("Copy speaker experience to clipboard"));

        Label    experienceLabel    = Factory.createLabel("Experience", Constants.GRAY, Fonts.avenirNextLtProDemi(Constants.STD_FONT_SIZE), Pos.CENTER_RIGHT);
        TextArea experienceTextArea = Factory.createRegularTextArea("Your experience as a speaker", Constants.BLACK, Constants.STD_FONT_SIZE);
        HBox     experienceHBox     = new HBox(experienceLabel, Factory.createSpacer(Orientation.HORIZONTAL), copyExperienceIcon);
        VBox     experienceBox      = new VBox(experienceHBox, experienceTextArea);
        experienceTextArea.setText(speakerItem.getExperience());
        copyExperienceIcon.setOnMousePressed(e -> {
            clipboard.clear();
            clipboardContent.clear();
            clipboardContent.putString(experienceTextArea.getText());
            clipboard.setContent(clipboardContent);
            copiedFeedbackPane.setOpacity(1.0);
            fadeOutPane(copiedFeedbackPane);
        });

        Button copySpeakerInfoButton = Factory.createButton("Copy Speaker Info", "Copy complete speaker info", Constants.STD_FONT_SIZE);
        copySpeakerInfoButton.setOnAction(e -> {
           final StringBuilder speakerInfoBuilder = new StringBuilder();
           speakerInfoBuilder.append("Name").append(NEW_LINE)
                             .append(speakerNameTextField.getText()).append(NEW_LINE)
                             .append(NEW_LINE)
                             .append("BlueSky").append(NEW_LINE)
                             .append("https://bsky.app/profile/").append(blueSkyTextField.getText()).append(NEW_LINE)
                             .append(NEW_LINE)
                             .append("Bio").append(NEW_LINE)
                             .append(bioTextArea.getText()).append(NEW_LINE)
                             .append(NEW_LINE)
                             .append("Experience").append(NEW_LINE)
                             .append(experienceTextArea.getText());

            clipboard.clear();
            clipboardContent.clear();
            clipboardContent.putString(speakerInfoBuilder.toString());
            clipboard.setContent(clipboardContent);
            copiedFeedbackPane.setOpacity(1.0);
            fadeOutPane(copiedFeedbackPane);
        });

        Button closeButton = Factory.createButton("Close", "Close speaker info dialog", Constants.STD_FONT_SIZE);
        closeButton.setOnAction(e -> {
            speakerItem.setName(speakerNameTextField.getText());
            speakerItem.setBluesky(blueSkyTextField.getText());
            speakerItem.setBio(bioTextArea.getText());
            speakerItem.setExperience(experienceTextArea.getText());
            Helper.saveSpeakerItem(speakerItem);
            speakerInfoStage.close();
            speakerInfoVisible.set(false);
        });
        HBox   buttonBox = new HBox(copySpeakerInfoButton, Factory.createSpacer(Orientation.HORIZONTAL), closeButton);
        buttonBox.setPadding(new Insets(10));

        VBox  speakerInfoVBox  = new VBox(15, speakerImagePane, copySpeakerImageButton, speakerNameBox, blueSkyBox, bioBox, experienceBox, buttonBox);
        speakerInfoVBox.setPadding(new Insets(10));
        speakerInfoVBox.setAlignment(Pos.CENTER);
        speakerInfoVBox.setPrefWidth(360);


        Scene scene = new Scene(new StackPane(speakerInfoVBox, copiedFeedbackPane));
        scene.getStylesheets().add(Main.class.getResource("conficheck4j.css").toExternalForm());

        speakerInfoStage.setOnCloseRequest(e -> speakerInfoVisible.set(false));
        speakerInfoStage.setScene(scene);
        speakerInfoStage.show();
        speakerInfoStage.setAlwaysOnTop(true);
        speakerInfoStage.toFront();
    }

    private void fadeOutPane(final Pane pane) {
        final PauseTransition pauseTransition = new PauseTransition(Duration.seconds(0.3));
        final FadeTransition  fadeTransition  = new FadeTransition(Duration.millis(1000));
        fadeTransition.setInterpolator(Interpolator.EASE_OUT);
        fadeTransition.setNode(pane);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        fadeTransition.setAutoReverse(false);
        SequentialTransition sequentialTransition = new SequentialTransition(pauseTransition, fadeTransition);
        sequentialTransition.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}