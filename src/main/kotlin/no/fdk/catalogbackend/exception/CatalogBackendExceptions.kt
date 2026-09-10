package no.fdk.catalogbackend.exception

class NotFoundException(message: String) : RuntimeException(message)

class BadRequestException(message: String?) : RuntimeException(message)

class InternalServerErrorException(message: String?) : RuntimeException(message)
