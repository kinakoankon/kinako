package kinako.domain.service;

import kinako.domain.model.channel.ChannelConfiguration;
import kinako.domain.model.tuner.Tuner;
import kinako.domain.model.tuner.TunerStatus;
import kinako.domain.model.tuner.TunerType;

import java.util.List;

public interface ITunerService {

    Tuner getSuitableOne(final TunerType tunerType, final TunerStatus tunerStatus);

    Tuner getClosableOne(final int channelRemoteControl, final TunerStatus tunerStatus);

    void lock(final String deviceName, final long pid, final int channelRecording, final TunerStatus tunerStatus, final Process process);

    void release(final String deviceName);

    void releaseAll();

    boolean isEpgAcquisitionRunning();

    List<ChannelConfiguration> getChannelConfigurationList();

    ChannelConfiguration getChannelConfiguration(final int channelRemoteControl);

}
