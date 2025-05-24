package media_player;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/**
 *
 * @author rifial
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private StackPane centerContentStackPane;
    @FXML
    private MediaView mediaView;
    @FXML
    private HBox musicInfoPane;
    @FXML
    private Label musicTitleLabel;
    @FXML
    private Label musicArtistLabel;

    @FXML
    private ToggleButton musicToggleButton;
    @FXML
    private ToggleButton videoToggleButton;
    @FXML
    private ListView<MediaItem> playlistView;

    @FXML
    private Slider progressSlider;
    @FXML
    private Label currentTimeLabel;
    @FXML
    private Label totalDurationLabel;

    @FXML
    private Button prevButton;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button rewindButton;
    @FXML
    private Button fastForwardButton;
    @FXML
    private Slider volumeSlider;

    @FXML
    private BorderPane rootInnerPane;
    @FXML
    private VBox controlsVBox;
    @FXML
    private VBox sidebarVBox;
    @FXML
    private HBox buttonContainerHBox;
    @FXML
    private HBox mediaControlsHBox;
    @FXML
    private MenuButton openFileMenuButton;

    private MediaPlayer mediaPlayer;
    private final ObservableList<MediaItem> allPlaylistItems = FXCollections.observableArrayList();
    private FilteredList<MediaItem> filteredPlaylistItems;
    private ToggleGroup mediaTypeToggleGroup;
    private boolean isVideoMode = true;
    private boolean atEndOfMedia = false;
    private boolean isPlaying = false;
    private boolean isSliderBeingDragged = false;

    private static class MediaItem {

        String filePath;
        String title;
        String artist;
        boolean isVideo;

        public MediaItem(String filePath, String title, boolean isVideo, String artist, String albumArtPath) {
            this.filePath = filePath;
            this.title = title;
            this.isVideo = isVideo;
            this.artist = artist;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filteredPlaylistItems = new FilteredList<>(allPlaylistItems, p -> true);
        playlistView.setItems(filteredPlaylistItems);

        mediaTypeToggleGroup = new ToggleGroup();
        musicToggleButton.setToggleGroup(mediaTypeToggleGroup);
        videoToggleButton.setToggleGroup(mediaTypeToggleGroup);

        videoToggleButton.setSelected(true);

        mediaTypeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                if (oldValue != null) {
                    oldValue.setSelected(true);
                }
                return;
            }
            boolean newModeIsVideo = (newValue == videoToggleButton);
            if (isVideoMode != newModeIsVideo) {
                isVideoMode = newModeIsVideo;
                updateUIMode();
                updatePlaylistFilter();
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
                resetControls();
                mediaView.setMediaPlayer(null);
            }
        });

        playlistView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                playMedia(newSelection);
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
                resetControls();
                mediaView.setMediaPlayer(null);
                updateUIMode();
            }
        });

        volumeSlider.setValue(50);
        volumeSlider.valueProperty().addListener((observable) -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
                mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);
            }
        });

        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            isSliderBeingDragged = isChanging;
            if (!isChanging && mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED
                    && (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY)) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        progressSlider.setOnMouseReleased(event -> {
            if (mediaPlayer != null && !isSliderBeingDragged && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED
                    && (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY)) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });
        progressSlider.setOnMouseClicked(event -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED
                    && (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY)) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        playPauseButton.setText("Play");
        updateControlsAvailability(false);
        resetTimeLabels();
    }

    private void updateUIMode() {
        if (isVideoMode) {
            musicInfoPane.setVisible(false);
            musicInfoPane.setManaged(false);
            mediaView.setVisible(true);
            mediaView.setManaged(true);
        } else {
            mediaView.setVisible(false);
            mediaView.setManaged(false);
            musicInfoPane.setVisible(true);
            musicInfoPane.setManaged(true);
            musicTitleLabel.setText("Judul Musik");
            musicArtistLabel.setText("Nama Artis");
        }
    }

    private void updatePlaylistFilter() {
        if (filteredPlaylistItems != null) {
            filteredPlaylistItems.setPredicate(item -> item.isVideo == isVideoMode);
        }
    }

    @FXML
    private void handleRewind(ActionEvent event) {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED
                && (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED)) {
            Duration currentTime = mediaPlayer.getCurrentTime();
            Duration newTime = currentTime.subtract(Duration.seconds(10));
            if (newTime.lessThan(Duration.ZERO)) {
                newTime = Duration.ZERO;
            }
            mediaPlayer.seek(newTime);
        }
    }

    @FXML
    private void handleFastForward(ActionEvent event) {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED
                && (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED)) {

            Media currentMedia = mediaPlayer.getMedia();
            if (currentMedia == null) {
                return;
            }
            Duration totalTime = currentMedia.getDuration();

            if (totalTime.isUnknown() || totalTime.lessThanOrEqualTo(Duration.ZERO)) {
                return;
            }

            Duration currentTime = mediaPlayer.getCurrentTime();

            if (currentTime.greaterThanOrEqualTo(totalTime)) {
                return;
            }

            Duration newTime = currentTime.add(Duration.seconds(10));

            if (newTime.greaterThan(totalTime)) {
                newTime = totalTime;
            }

            if (newTime.equals(currentTime)) {
                return;
            }

            mediaPlayer.seek(newTime);
        }
    }

    @FXML
    private void handlePlayPause(ActionEvent event) {
        if (mediaPlayer == null || mediaPlayer.getStatus() == MediaPlayer.Status.DISPOSED) {
            MediaItem selectedItem = playlistView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                playMedia(selectedItem);
            } else if (!filteredPlaylistItems.isEmpty()) {
                playlistView.getSelectionModel().selectFirst();
            }
            return;
        }

        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.UNKNOWN || status == MediaPlayer.Status.HALTED) {
            return;
        }

        if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.READY || status == MediaPlayer.Status.STOPPED) {
            if (atEndOfMedia) {
                mediaPlayer.seek(mediaPlayer.getStartTime());
                atEndOfMedia = false;
            }
            mediaPlayer.play();
            isPlaying = true;
            playPauseButton.setText("Pause");
        } else if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setText("Play");
        }
    }

    @FXML
    private void handleStop(ActionEvent event) {
        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            mediaPlayer.stop();
            isPlaying = false;
            atEndOfMedia = false;
            playPauseButton.setText("Play");
            if (progressSlider != null) {
                progressSlider.setValue(0);
            }
            if (currentTimeLabel != null) {
                currentTimeLabel.setText(formatDuration(Duration.ZERO));
            }
        }
    }

    @FXML
    private void handleNext(ActionEvent event) {
        if (!filteredPlaylistItems.isEmpty()) {
            int currentIndexInFiltered = playlistView.getSelectionModel().getSelectedIndex();
            if (currentIndexInFiltered == -1 && !filteredPlaylistItems.isEmpty()) {
                playlistView.getSelectionModel().selectFirst();
                return;
            }
            int nextIndexInFiltered = (currentIndexInFiltered + 1) % filteredPlaylistItems.size();
            playlistView.getSelectionModel().select(nextIndexInFiltered);
        }
    }

    @FXML
    private void handlePrevious(ActionEvent event) {
        if (!filteredPlaylistItems.isEmpty()) {
            int currentIndexInFiltered = playlistView.getSelectionModel().getSelectedIndex();
            if (currentIndexInFiltered == -1 && !filteredPlaylistItems.isEmpty()) {
                playlistView.getSelectionModel().selectLast();
                return;
            }
            int prevIndexInFiltered = (currentIndexInFiltered - 1 + filteredPlaylistItems.size()) % filteredPlaylistItems.size();
            playlistView.getSelectionModel().select(prevIndexInFiltered);
        }
    }

    @FXML
    private void handleAddFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video Files (*.mp4, *.mkv, ...)", "*.mp4", "*.flv", "*.mkv", "*.avi", "*.mov", "*.wmv");
        FileChooser.ExtensionFilter audioFilter = new FileChooser.ExtensionFilter("Audio Files (*.mp3, *.wav, ...)", "*.mp3", "*.wav", "*.aac", "*.ogg", "*.flac");
        FileChooser.ExtensionFilter allMediaFilter = new FileChooser.ExtensionFilter("All Media Files",
                "*.mp4", "*.flv", "*.mkv", "*.avi", "*.mov", "*.wmv",
                "*.mp3", "*.wav", "*.aac", "*.ogg", "*.flac");

        fileChooser.getExtensionFilters().addAll(allMediaFilter, videoFilter, audioFilter);
        fileChooser.setSelectedExtensionFilter(allMediaFilter);

        List<File> files = fileChooser.showOpenMultipleDialog(playlistView.getScene().getWindow());

        if (files != null) {
            for (File file : files) {
                String fileNameLower = file.getName().toLowerCase();
                boolean fileIsVideo = videoFilter.getExtensions().stream().anyMatch(ext -> fileNameLower.endsWith(ext.substring(1)));
                allPlaylistItems.add(new MediaItem(file.getAbsolutePath(), file.getName(), fileIsVideo, "Unknown Artist", null));
            }
        }
    }

    @FXML
    private void handleAddPlaylistOrFolder(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(playlistView.getScene().getWindow());

        if (selectedDirectory != null) {
            FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.flv", "*.avi", "*.mov", "*.wmv");
            FileChooser.ExtensionFilter audioFilter = new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac", "*.ogg", "*.flac");

            File[] filesInDir = selectedDirectory.listFiles((dir, name) -> {
                String nameLower = name.toLowerCase();
                return videoFilter.getExtensions().stream().anyMatch(ext -> nameLower.endsWith(ext.substring(1)))
                        || audioFilter.getExtensions().stream().anyMatch(ext -> nameLower.endsWith(ext.substring(1)));
            });

            if (filesInDir != null) {
                for (File file : filesInDir) {
                    String fileNameLower = file.getName().toLowerCase();
                    boolean fileIsVideo = videoFilter.getExtensions().stream().anyMatch(ext -> fileNameLower.endsWith(ext.substring(1)));
                    allPlaylistItems.add(new MediaItem(file.getAbsolutePath(), file.getName(), fileIsVideo, "Unknown Artist", null));
                }
            }
        }
    }

    private void playMedia(MediaItem item) {
        if (item == null) {
            return;
        }

        if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.DISPOSED) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        mediaPlayer = null;
        mediaView.setMediaPlayer(null);

        updateUIMode();

        try {
            Media media = new Media(new File(item.filePath).toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            if (mediaPlayer == null) {
                updateControlsAvailability(false);
                return;
            }

            mediaPlayer.setVolume(volumeSlider.getValue() / 100.0);

            mediaPlayer.setOnReady(() -> {
                if (mediaPlayer == null || mediaPlayer.getStatus() == MediaPlayer.Status.DISPOSED) {
                    return;
                }

                if (item.isVideo) {
                    mediaView.setMediaPlayer(mediaPlayer);
                } else {
                    musicTitleLabel.setText(item.title);
                    musicArtistLabel.setText(item.artist != null ? item.artist : "Unknown Artist");
                }

                Duration d = media.getDuration();
                if (d.isUnknown() || d.lessThanOrEqualTo(Duration.ZERO)) {
                    progressSlider.setMax(0);
                    totalDurationLabel.setText("--:--");
                } else {
                    progressSlider.setMax(d.toSeconds());
                    totalDurationLabel.setText(formatDuration(d));
                }
                updateControlsAvailability(true);
                atEndOfMedia = false;

                mediaPlayer.play();
                isPlaying = true;
                playPauseButton.setText("Pause");
            });

            mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                if (mediaPlayer == null || mediaPlayer.getStatus() == MediaPlayer.Status.DISPOSED) {
                    return;
                }
                if (!isSliderBeingDragged && !newValue.isUnknown() && newValue.greaterThanOrEqualTo(Duration.ZERO)) {
                    progressSlider.setValue(newValue.toSeconds());
                }
                if (!newValue.isUnknown() && newValue.greaterThanOrEqualTo(Duration.ZERO)) {
                    currentTimeLabel.setText(formatDuration(newValue));
                } else {
                    currentTimeLabel.setText("--:--");
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                if (mediaPlayer == null || mediaPlayer.getStatus() == MediaPlayer.Status.DISPOSED) {
                    return;
                }
                atEndOfMedia = true;
                playPauseButton.setText("Play");
                isPlaying = false;
                int currentFilteredIndex = playlistView.getSelectionModel().getSelectedIndex();
                if (currentFilteredIndex < filteredPlaylistItems.size() - 1) {
                    handleNext(null);
                } else {
                    if (mediaPlayer != null && media.getDuration() != null && !media.getDuration().isUnknown()) {
                        mediaPlayer.seek(Duration.ZERO);
                        progressSlider.setValue(0);
                        currentTimeLabel.setText(formatDuration(Duration.ZERO));
                    } else {
                        progressSlider.setValue(0);
                        currentTimeLabel.setText(formatDuration(Duration.ZERO));
                    }
                }
            });

            mediaPlayer.setOnError(() -> {
                if (mediaPlayer == null || mediaPlayer.getStatus() == MediaPlayer.Status.DISPOSED) {
                    return;
                }
                String errorMessage = "Unknown media player error";
                Throwable error = mediaPlayer.getError();
                if (error != null) {
                    errorMessage = error.getMessage() != null ? error.getMessage() : "Error object (" + error.getClass().getSimpleName() + ") exists but message is null";
                }
                System.err.println("Media Player Error (setOnError) for " + (item != null ? item.title : "unknown item") + ": " + errorMessage);
                updateControlsAvailability(false);
            });

        } catch (Exception e) {
            System.err.println("EXCEPTION BESAR di playMedia untuk " + (item != null ? item.filePath : "unknown item") + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            updateControlsAvailability(false);
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.lessThan(Duration.ZERO)) {
            return "00:00";
        }
        long totalSeconds = (long) Math.floor(duration.toSeconds());
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private void updateControlsAvailability(boolean available) {
        playPauseButton.setDisable(!available);
        stopButton.setDisable(!available);
        boolean playlistHasItems = !filteredPlaylistItems.isEmpty();
        prevButton.setDisable(!available || !playlistHasItems);
        nextButton.setDisable(!available || !playlistHasItems);

        boolean canSeek = false;
        if (mediaPlayer != null && mediaPlayer.getMedia() != null) {
            Duration d = mediaPlayer.getMedia().getDuration();
            if (d != null && !d.isUnknown() && d.greaterThan(Duration.ZERO)) {
                canSeek = true;
            }
        }

        rewindButton.setDisable(!available || !canSeek);
        fastForwardButton.setDisable(!available || !canSeek);

        progressSlider.setDisable(!available || !canSeek);
    }

    private void resetControls() {
        playPauseButton.setText("Play");
        isPlaying = false;
        atEndOfMedia = false;
        if (progressSlider != null) {
            progressSlider.setValue(0);
        }
        resetTimeLabels();
        updateControlsAvailability(false);
    }

    private void resetTimeLabels() {
        if (currentTimeLabel != null) {
            currentTimeLabel.setText("00:00");
        }
        if (totalDurationLabel != null) {
            totalDurationLabel.setText("00:00");
        }
    }
}
