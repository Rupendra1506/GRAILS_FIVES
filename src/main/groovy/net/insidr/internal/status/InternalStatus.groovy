package net.insidr.internal.status

class InternalStatus {

    def status = InternalStatusCode.OK
    def message = InternalStatusCode.OK.defaultMessage
    def entries = [:]

    def add(entry) {
        entries[entry.name] = entry
    }

    def updateGeneralStatus() {
        status = entries.max { it.value.status }?.value?.status ?: InternalStatusCode.OK
        message = status.defaultMessage
    }

}
