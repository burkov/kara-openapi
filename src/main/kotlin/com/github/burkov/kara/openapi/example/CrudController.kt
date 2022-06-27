package com.github.burkov.kara.openapi.example

import com.github.burkov.kara.openapi.annotations.OpenApi
import kara.*
import kara.Controller
import java.util.UUID

data class RecordDto(
    val id: Int,
    val email: String,
)

data class CreateRecordDto(
    val email: String
)

@Controller("application/json")
@Location("/api/emails")
object CrudController {
    private var mockSerial = 0
    private val mock = mutableListOf<RecordDto>()

    init {
        repeat(5) {
            mock.add(RecordDto(mockSerial++, "${UUID.randomUUID().toString().substringBefore("-")}@gmail.com"))
        }
    }

    @OpenApi
    @Post("/", "*")
    fun create(@RequestBodyParameter request: CreateRecordDto): RecordDto {
        return RecordDto(
            id = mockSerial++,
            email = request.email
        ).also { mock.add(it) }
    }

    @Get("/")
    fun list(): List<RecordDto> {
        return mock
    }

    @Put("/:id", "*")
    fun update(@RequestBodyParameter request: RecordDto): RecordDto {
        return mock.first()
    }

    @Delete("/:id", "*")
    fun delete(id: Int) {
        mock.removeIf { it.id == id }
    }
}