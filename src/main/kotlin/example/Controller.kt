package example

object Controller {
    private var mockSerial = 0
    private val mock = mutableListOf<AbuserEmail>()

    @Get("/")
    fun list(): List<AbuserEmail> {
        return mock
    }

    @Post("/", "*") // FIXME
    fun create(@RequestBodyParameter request: CreateAbuserEmailRequest): AbuserEmail {
        val validation = EmailValidationOps.validateEmail(request.email)
        return when {
            !validation.isValid -> throw ResultWithCodeException(SC_BAD_REQUEST, "invalid email")
            validation.isDisposable -> throw ResultWithCodeException(SC_BAD_REQUEST, "disposable email")
            AbuserEmailOps.lookup(request.email) != null -> throw ResultWithCodeException(SC_CONFLICT, "already exists")
            else -> {
                AbuserEmail(
                    id = mockSerial++,
                    email = Contact.normalizeEmail(request.email),
                    product = request.product,
                    addedAt = trDateTimeNow
                ).also { mock.add(it) }
            }
        }
    }

    @Delete("/:id", "*") // FIXME
    fun delete(id: Int) {
        mock.removeIf { it.id == id }
    }
}