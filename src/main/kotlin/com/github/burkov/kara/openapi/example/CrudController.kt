package com.github.burkov.kara.openapi.example

import com.github.burkov.kara.openapi.OpenApi
import kara.*
import kara.Controller
import java.util.UUID

data class Record(
    val id: Int,
    val email: String,
) {
    companion object {
        data class Record(
            val email: String
        )
    }
}

@Controller("application/json")
@Location("/api")
object CrudController {
    private var mockSerial = 0
    private val mock = mutableListOf<Record>()

    init {
        repeat(5) {
            mock.add(Record(mockSerial++, "${UUID.randomUUID().toString().substringBefore("-")}@gmail.com"))
        }
    }

    @OpenApi
    @Post("/", "*")
    fun create(@RequestBodyParameter request: Record.Companion.Record): Record {
        return Record(
            id = mockSerial++,
            email = request.email
        ).also { mock.add(it) }
    }

    @Get("/")
    fun list(): List<Record> {
        return mock
    }

    @Put("/:id", "*")
    fun update(@RequestBodyParameter request: Record): Record {
        return mock.first()
    }

    @Delete("/:id", "*")
    fun delete(id: Int) {
        mock.removeIf { it.id == id }
    }
}