package kinako.api.v1;

import kinako.domain.model.tuner.TunerStatus;
import kinako.domain.operator.ITunerOperator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@AllArgsConstructor
@RequestMapping("api/v1/streams")
@RestController
public class TunerRestController {

    private final ITunerOperator tunerOperator;

    private ResponseEntity<StreamingResponseBody> getStream(
            final int channelRemoteControl, final long duration, final TunerStatus tunerStatus
    ) {
        log.info("INET Socket mode");
        return tunerOperator.open(channelRemoteControl, duration, tunerStatus);
    }

    @GetMapping("/{channelRemoteControl}")
    public ResponseEntity<StreamingResponseBody> start(@PathVariable final int channelRemoteControl) {
        return getStream(channelRemoteControl, -1, TunerStatus.LIVE);
    }

    @DeleteMapping("/{channelRemoteControl}/{tunerStatus}")
    public ResponseEntity<Object> stop(@PathVariable final int channelRemoteControl, @PathVariable final TunerStatus tunerStatus) {
        return tunerOperator.close(channelRemoteControl, tunerStatus);
    }

    @DeleteMapping("/{channelRemoteControl}")
    public ResponseEntity<Object> stop(@PathVariable final int channelRemoteControl) {
        return tunerOperator.close(channelRemoteControl, TunerStatus.LIVE);
    }

}
