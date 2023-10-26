package net.insidr.internal.status

enum InternalStatusCheckItem {
    DATABASE_CONNECTION("database_connectivity", "Database connectivity"),
    RABBITMQ_CONNECTION("rabbitmq_service", "RabbitMQ connectivity"),
    REDIS_CONNECTION("redis_service", "Redis connectivity"),
    ROUTING_UNITS_OF_WORK("routing_units_of_work", "Routing units of work"),

    final String name
    final String legacyName

    InternalStatusCheckItem (name, legacyName) {
        this.name = name
        this.legacyName = legacyName
    }

    String value() {
        name
    }

    String toString() {
        name
    }

}
