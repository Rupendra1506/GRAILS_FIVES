package net.insidr.internal.status

enum InternalStatusCode {
    OK("ok", "Everything is 200 ok."),
    UNKNOWN("unknown", "No info for this."),
    FAILING("failing", "Failing service."),
    CRITICAL("critical", "Critical status.")

    def name
    def defaultMessage

    InternalStatusCode(name, defaultMessage) {
        this.name = name
        this.defaultMessage = defaultMessage
    }

}
