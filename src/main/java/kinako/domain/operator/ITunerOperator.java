package kinako.domain.operator;

import kinako.domain.model.tuner.TunerStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface ITunerOperator {

    ResponseEntity<StreamingResponseBody> open(final int channelRemoteControl, final long duration, final TunerStatus tunerStatus);

    ResponseEntity<Object> close(final int channelRemoteControl, final TunerStatus tunerStatus);

}
