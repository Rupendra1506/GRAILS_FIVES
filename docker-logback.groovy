import java.nio.charset.Charset

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName("UTF-8")
        pattern = "%date [%thread] %level %logger{0} - %msg%n"
        outputPatternAsHeader = true
    }
}

root(${LOG_LEVEL}, [ "STDOUT" ])
