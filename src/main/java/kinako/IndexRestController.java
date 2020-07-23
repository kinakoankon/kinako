package kinako;

import kinako.domain.model.Hello;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
public class IndexRestController {
    @GetMapping("")
    public Hello hello() {
        final long epoch = Instant.now().toEpochMilli();
        final Hello hello = new Hello();
        hello.setEpoch(epoch);
        return hello;
    }
}
