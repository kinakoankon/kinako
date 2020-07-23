package kinako.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kinako.domain.config.TunerConfig;
import kinako.domain.model.channel.ChannelConfiguration;
import kinako.domain.model.channel.ChannelConfigurationWrapper;
import kinako.domain.model.tuner.Tuner;
import kinako.domain.model.tuner.TunerStatus;
import kinako.domain.model.tuner.TunerType;
import kinako.domain.model.tuner.TunerWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class TunerService implements ITunerService {

    private final String fileSeparator = File.separator;

    private List<Tuner> tunerList = new ArrayList<>();

    @Value("${tuner.json}")
    private String tunerJson;

    private final TunerConfig tunerConfig;

    public TunerService(TunerConfig tunerConfig) {
        this.tunerConfig = requireNonNull(tunerConfig);
    }

    private List<Tuner> getTunerListFromTunerJson() {

        final String path = ITunerService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        final String[] pathArray = path.split(fileSeparator);
        final StringBuilder currentPath = new StringBuilder();
        for (int i = 1; i < pathArray.length - 3; i++) {
            currentPath.append(fileSeparator).append(pathArray[i]);
        }

        try {
            Resource resource = new FileSystemResource(currentPath + fileSeparator + tunerJson);
            if (!resource.exists()) {
                resource = new ClassPathResource(tunerJson);
            }
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(resource.getInputStream(), TunerWrapper.class).getTunerList();
        } catch (IOException ex) {
            throw new IllegalStateException("Invalid tuner.json");
        }
    }

    @PostConstruct
    void init() {
        tunerList = getTunerListFromTunerJson();
        log.info(tunerList.toString());
    }

    private Optional<Tuner> getSuitableTuner(final TunerType tunerType, final TunerStatus tunerStatus) {
        final Predicate<Tuner> predicate;
        switch (tunerStatus) {
            case LIVE:
                predicate = tuner -> tuner.getStatus() == TunerStatus.AVAILABLE
                        || tuner.getStatus() == TunerStatus.EPG;
                break;
            case RECORDING:
                predicate = tuner -> tuner.getStatus() == TunerStatus.AVAILABLE
                        || tuner.getStatus() == TunerStatus.EPG
                        || tuner.getStatus() == TunerStatus.LIVE;
                break;
            case EPG:
            default:
                predicate = tuner -> tuner.getStatus() == TunerStatus.AVAILABLE;
        }
        return tunerList.stream()
                .filter(tuner -> tuner.getType() == tunerType)
                .filter(predicate)
                .min(Comparator.comparingInt(Tuner::getIndex));
    }

    @Override
    public Tuner getSuitableOne(final TunerType tunerType, final TunerStatus tunerStatus) {
        final String message = "No tuner is available for " + tunerStatus.name().toUpperCase() + ".";
        return getSuitableTuner(tunerType, tunerStatus).orElseThrow(() -> new NoSuchElementException(message));
    }

    @Override
    public Tuner getClosableOne(final int channelRemoteControl, final TunerStatus tunerStatus) {
        for (Tuner tuner : tunerList) {
            for (ChannelConfiguration channelConfiguration : getChannelConfigurationList()) {
                if (channelRemoteControl == channelConfiguration.getChannelRemoteControl()
                        && tuner.getChannelRecording() == channelConfiguration.getChannelRecording()
                ) {
                    if (tuner.getStatus() == tunerStatus) {
                        return tuner;
                    }
                }
            }
        }
        final String message = "No tuner is available for " + channelRemoteControl + "(" + tunerStatus + ").";
        throw new NoSuchElementException(message);
    }

    @Override
    public void lock(final String deviceName, final long pid, final int channelRecording, final TunerStatus tunerStatus, final Process process) {
        for (Tuner tuner : tunerList) {
            if (tuner.getDeviceName().equals(deviceName)) {
                tuner.setStatus(tunerStatus);
                tuner.setPid(pid);
                tuner.setChannelRecording(channelRecording);
                tuner.setProcess(process);
                return;
            }
        }
        throw new IllegalStateException("Failed to lock tuner: " + deviceName);
    }

    private void reset(final Tuner tuner) {
        if (tuner.getProcess() != null) {
            tuner.getProcess().destroy();
        }
        tuner.setProcess(null);
        tuner.setChannelRecording(0);
        tuner.setPid(0);
        tuner.setStatus(TunerStatus.AVAILABLE);
    }

    @Override
    public void release(final String deviceName) {
        for (Tuner tuner : tunerList) {
            if (tuner.getDeviceName().equals(deviceName)) {
                reset(tuner);
                return;
            }
        }
        throw new IllegalStateException("Failed to release tuner: " + deviceName);
    }

    @Override
    public void releaseAll() {
        for (Tuner tuner : tunerList) {
            reset(tuner);
        }
    }

    @Override
    public boolean isEpgAcquisitionRunning() {
        return tunerList.stream().anyMatch(tuner -> tuner.getStatus() == TunerStatus.EPG);
    }

    @Override
    public List<ChannelConfiguration> getChannelConfigurationList() {
        try {
            final Resource resource = new ClassPathResource(tunerConfig.getChannelConfiguration());
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(resource.getInputStream(), ChannelConfigurationWrapper.class)
                    .getChannelConfigurationList();
        } catch (IOException ex) {
            throw new IllegalStateException("invalid channel-configuration.json");
        }
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(final int channelRemoteControl) {
        final List<ChannelConfiguration> channelConfigurationList = getChannelConfigurationList();
        for (ChannelConfiguration channelConfiguration : channelConfigurationList) {
            if (channelConfiguration.getChannelRemoteControl() == channelRemoteControl) {
                return channelConfiguration;
            }
        }
        throw new IllegalStateException("Invalid channelRemoteControl:" + channelRemoteControl);
    }
}
