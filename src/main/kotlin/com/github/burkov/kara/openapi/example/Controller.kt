package com.github.burkov.kara.openapi.example

import kara.*
import kara.Controller
import java.util.UUID

data class Record(
    val id: Int,
    val email: String,
)

data class CreateRecordRequest(
    val email: String
)

@Controller("application/json")
@Location("/api")
object Controller {
    private var mockSerial = 0
    private val mock = mutableListOf<Record>()

    init {
        repeat(5) {
            mock.add(Record(mockSerial++, "${UUID.randomUUID().toString().substringBefore("-")}@gmail.com"))
        }
    }

    @Get("/")
    fun list(): List<Record> {
        return mock
    }

    @Post("/", "*")
    fun create(@RequestBodyParameter request: CreateRecordRequest): Record {
        return Record(
            id = mockSerial++,
            email = request.email
        ).also { mock.add(it) }
    }

    @Delete("/:id", "*")
    fun delete(id: Int) {
        mock.removeIf { it.id == id }
    }
}