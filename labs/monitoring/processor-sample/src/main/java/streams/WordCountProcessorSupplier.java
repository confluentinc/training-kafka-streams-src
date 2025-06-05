package streams;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.util.Locale;

public class WordCountProcessorSupplier implements ProcessorSupplier<String, String> {
    public class WordCountProcessor implements Processor<String, String> {
        private ProcessorContext context;
        private KeyValueStore<String, Integer> kvStore;

        @Override
        @SuppressWarnings("unchecked")
        public void init(final ProcessorContext context) {
            // TODO
            this.context = context;
            this.context.schedule(Duration.ofMillis(1000), PunctuationType.STREAM_TIME, new Punctuator() {
                @Override
                public void punctuate(long timestamp) {
                    try (KeyValueIterator<String, Integer> iter = kvStore.all()) {
                        System.out.println("----------- " + timestamp + " ----------- ");
                        while (iter.hasNext()) {
                            KeyValue<String, Integer> entry = iter.next();
                            System.out.println("[" + entry.key + ", " + entry.value + "]");
                            context.forward(entry.key, entry.value.toString());
                        }
                    }
                }
            });
            this.kvStore = (KeyValueStore<String, Integer>) context.getStateStore("Counts");
        }

        @Override
        public void process(String recordKey, String recordValue) {
            // TODO
            String[] words = recordValue.toLowerCase(Locale.getDefault()).split(" ");
            for (String word : words) {
                Integer oldValue = this.kvStore.get(word);
                if (oldValue == null) {
                    this.kvStore.put(word, 1);
                } else {
                    this.kvStore.put(word, oldValue + 1);
                }
            }
            context.commit();
        }
    
        @Override
        public void close() {}
    }

    @Override
    public Processor<String, String> get() {
        return new WordCountProcessor();
    }
}
