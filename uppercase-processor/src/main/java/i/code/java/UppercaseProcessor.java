package i.code.java;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.messaging.handler.annotation.SendTo;

@EnableBinding(Processor.class)
public class UppercaseProcessor {

    private static final Logger log = LoggerFactory.getLogger(UppercaseProcessor.class);

    @StreamListener(Processor.INPUT)
    @SendTo(Processor.OUTPUT)
    public String uppercase(String input) {
        String upperCasedInput = input.toUpperCase();

        log.debug("[{}] upper cased to: [{}]", input, upperCasedInput);
        return upperCasedInput;
    }
}
