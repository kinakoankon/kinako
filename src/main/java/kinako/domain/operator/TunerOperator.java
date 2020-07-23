package kinako.domain.operator;

import kinako.domain.model.channel.ChannelConfiguration;
import kinako.domain.model.tuner.Tuner;
import kinako.domain.model.tuner.TunerStatus;
import kinako.domain.model.tuner.TunerType;
import kinako.domain.service.ITunerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static kinako.domain.constants.KinakoConstants.MPEG2_TS_PACKET_LENGTH;

@Slf4j
@AllArgsConstructor
@Service
public class TunerOperator implements ITunerOperator {

    private final ITunerService tunerService;

    @Override
    public ResponseEntity<StreamingResponseBody> open(final int channelRemoteControl, final long duration, final TunerStatus tunerStatus) {

        final ChannelConfiguration channelConfiguration = tunerService.getChannelConfiguration(channelRemoteControl);
        final TunerType tunerType = channelConfiguration.getType();
        final int channelRecording = channelConfiguration.getChannelRecording();

        final Tuner tuner = tunerService.getSuitableOne(tunerType, tunerStatus);

        final String channelString = Integer.toString(channelRecording);
        final String durationString = duration < 0 ? "-" : Long.toString(duration);

        final String command = tuner.getCommand()
                .replace("<channel>", channelString)
                .replace("<duration>", durationString)
                .replace("<destination>", "-");
        final String[] commandArray = command.split(" ");

        return new ResponseEntity<>(outputStream -> {
            final ProcessBuilder processBuilder = new ProcessBuilder(commandArray);
            final Process process;
            try {
                process = processBuilder.start();
                final long pid = process.pid();
                tunerService.lock(tuner.getDeviceName(), pid, channelRecording, tunerStatus, process);
                final byte[] buf = new byte[MPEG2_TS_PACKET_LENGTH];
                int len;
                while ((len = process.getInputStream().read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                process.destroy();
            } catch (Exception ex) {
                log.warn("{}", ex.getMessage());
            } finally {
                tunerService.release(tuner.getDeviceName());
            }
        }, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> close(int channelRemoteControl, final TunerStatus tunerStatus) {
        final Tuner tuner = tunerService.getClosableOne(channelRemoteControl, tunerStatus);
        tunerService.release(tuner.getDeviceName());
        return ResponseEntity.noContent().build();
    }
}
