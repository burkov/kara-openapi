package com.github.burkov.kara.openapi.example

import com.github.burkov.kara.openapi.annotations.OpenApi
import kara.*
import kara.Controller
import java.util.UUID

data class UserDto(
    val id: Int,
    val name: String,
)

data class CreateUserDto(
    val name: String
)

data class UpdateUserDto(
    val name: String?
)

@OpenApi
@Controller("application/json")
@Location("/api/users")
object UsersController {
    private val users = mutableListOf<UserDto>()

    init {
        repeat(5) { i ->
            users.add(UserDto(i, UUID.randomUUID().toString().substringBefore("-")))
        }
    }

    @Post("/", "*")
    fun createUser(@RequestBodyParameter request: CreateUserDto): UserDto {
        return UserDto(
            id = users.maxByOrNull { it.id }?.id ?: 0,
            name = request.name
        ).also { users.add(it) }
    }

    @Get("/")
    fun listUsers(): List<UserDto> {
        return users
    }

//    @Put("/:id", "*")
//    fun updateUser(@RequestBodyParameter request: UpdateUserDto): UserDto {
//        return users.first()
//    }

//    @Delete("/:id", "*")
//    fun deleteUser(id: Int) {
//        users.removeIf { it.id == id }
//    }
}