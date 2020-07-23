package kinako.domain.model.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kinako.domain.model.tuner.TunerType;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ChannelConfiguration {
    private TunerType type;
    private int channelRemoteControl;
    private int channelRecording;
    private int serviceId;
}
