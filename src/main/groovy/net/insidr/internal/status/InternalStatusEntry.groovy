package net.insidr.internal.status

class InternalStatusEntry {

    InternalStatusCheckItem name
    InternalStatusCode status
    String message
    Map metrics

    InternalStatusEntry(name, status, message = null, metrics = null) {
        this.name = name
        this.status = status
        this.message = message ?: status.defaultMessage
        this.metrics = metrics ?: [:]
    }

    String toString() {
        return "${name}: { status: ${status}, message: ${message}, metrics: ${metrics} }"
    }

}
