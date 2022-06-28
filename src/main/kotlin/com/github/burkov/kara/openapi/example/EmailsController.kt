package com.github.burkov.kara.openapi.example

import com.github.burkov.kara.openapi.annotations.OpenApi
import kara.*
import kara.Controller
import java.util.UUID

data class EmailDto(
    val id: Int,
    val email: String,
)

data class CreateEmailDto(
    val email: String
)

data class UpdateEmailDto(
    val email: String?
)

@OpenApi
@Controller("application/json")
@Location("/api/emails")
object EmailsController {
    private val emails = mutableListOf<EmailDto>()

    init {
        repeat(5) { i ->
            emails.add(EmailDto(i, "${UUID.randomUUID().toString().substringBefore("-")}@gmail.com"))
        }
    }

    @Post("/", "*")
    fun createEmail(@RequestBodyParameter request: CreateEmailDto): EmailDto {
        return EmailDto(
            id = emails.maxByOrNull { it.id }?.id ?: 0,
            email = request.email
        ).also { emails.add(it) }
    }

    @Get("/")
    fun listEmails(): List<EmailDto> {
        return emails
    }

    @Put("/:id", "*")
    @OpenApiSomething(400, Error::class, "")

    fun updateEmail(@RequestBodyParameter request: UpdateEmailDto): EmailDto {
        return emails.first()
    }

    @Delete("/:id", "*")
    fun deleteEmail(id: Int) {
        emails.removeIf { it.id == id }
    }

    data class Error(val id: Int)
}